package de.caritas.cob.messageservice.api.service;

import static com.github.jknack.handlebars.internal.lang3.StringUtils.EMPTY;
import static com.github.jknack.handlebars.internal.lang3.StringUtils.isNotBlank;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.NoMasterKeyException;
import de.caritas.cob.messageservice.api.exception.RocketChatBadRequestException;
import de.caritas.cob.messageservice.api.exception.RocketChatUserNotInitializedException;
import de.caritas.cob.messageservice.api.helper.JSONHelper;
import de.caritas.cob.messageservice.api.helper.XssProtection;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ChatMessage;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.StandardResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.PostGroupAsReadDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageWrapper;
import de.caritas.cob.messageservice.api.service.dto.Message;
import de.caritas.cob.messageservice.api.service.dto.MessageResponse;
import de.caritas.cob.messageservice.api.service.helper.RocketChatCredentialsHelper;
import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class RocketChatService {

  public static final String E2E_ENCRYPTION_TYPE = "e2e";

  private static final String ENDPOINT_MESSAGE_GET = "/chat.getMessage?msgId=";

  @Value("${rocket.chat.api.url}")
  private String baseUrl;

  @Value("${rocket.chat.api.get.group.message.url}")
  private String rcGetGroupMessageUrl;

  @Value("${rocket.chat.api.send.message.url}")
  private String rcSendMessageUrl;

  @Value("${rocket.chat.api.post.group.messages.read.url}")
  private String rcPostGroupMessagesRead;

  @Value("${rocket.chat.api.get.group.info}")
  private String rcGetGroupInfoUrl;

  @Value("${rocket.technical.username}")
  private String rcTechnicalUser;

  @Value("${rocket.chat.header.auth.token}")
  private String rcHeaderAuthToken;

  @Value("${rocket.chat.header.user.id}")
  private String rcHeaderUserId;

  @Value("${rocket.chat.query.param.room.id}")
  private String rcQueryParamRoomId;

  @Value("${rocket.chat.query.param.offset}")
  private String rcQueryParamOffset;

  @Value("${rocket.chat.query.param.count}")
  private String rcQueryParamCount;

  @Value("${rocket.chat.query.param.sort}")
  private String rcQueryParamSort;

  @Value("${rocket.chat.query.param.sort.value}")
  private String rcQueryParamSortValue;

  private final @NonNull RestTemplate restTemplate;
  private final @NonNull EncryptionService encryptionService;
  private final @NonNull RocketChatCredentialsHelper rcCredentialHelper;
  private final MessageMapper mapper;

  // MVP: count and offset are always 0 to get all messages
  private static final int DEFAULT_COUNT = 0;
  private static final int DEFAULT_OFFSET = 0;

  /**
   * Gets the list of messages via Rocket.Chat API for the provided Rocket.Chat group. Filters out
   * technical user messages, decrypts the messages and sets the {@link MessageType}.
   *
   * @param rcToken   Rocket.Chat authentication token
   * @param rcUserId  Rocket.Chat user ID
   * @param rcGroupId Rocket.Chat group ID
   * @return MessageStreamDTO {@link MessageStreamDTO}
   */
  public MessageStreamDTO getGroupMessages(String rcToken, String rcUserId, String rcGroupId) {
    MessageStreamDTO messageStream = obtainMessageStream(rcToken, rcUserId, rcGroupId);
    messageStream.setMessages(Optional.ofNullable(messageStream.getMessages())
        .orElseGet(Collections::emptyList)
        .stream()
        .filter(userDTO -> !userDTO.getU().getUsername().equals(rcTechnicalUser))
        .map(msg -> decryptMessageAndSetMessageType(msg, rcGroupId))
        .map(mapper::typedMessageOf)
        .collect(Collectors.toList()));

    return messageStream;
  }

  private MessageStreamDTO obtainMessageStream(String rcToken, String rcUserId, String rcGroupId) {
    URI uri = buildMessageStreamUri(rcGroupId);
    HttpEntity<?> entity = new HttpEntity<>(getRocketChatHeader(rcToken, rcUserId));

    try {
      return restTemplate.exchange(uri, HttpMethod.GET, entity, MessageStreamDTO.class).getBody();

    } catch (RestClientException exception) {
      LogService.logRocketChatServiceError(exception);
      throw new InternalServerErrorException(String.format(
          "Could not read message stream from Rocket.Chat API (rcUserId: %s, rcGroupId: %s)",
          rcUserId, rcGroupId), LogService::logRocketChatServiceError);
    }
  }

  private URI buildMessageStreamUri(String rcGroupId) {
    try {
      return UriComponentsBuilder.fromUriString(rcGetGroupMessageUrl)
          .queryParam(rcQueryParamRoomId, rcGroupId)
          .queryParam(rcQueryParamOffset, DEFAULT_OFFSET)
          .queryParam(rcQueryParamCount, DEFAULT_COUNT)
          .queryParam(rcQueryParamSort, rcQueryParamSortValue)
          .build()
          .encode()
          .toUri();

    } catch (IllegalArgumentException exception) {
      throw new InternalServerErrorException(
          String.format("Could not build message stream URI for rcGroupId %s", rcGroupId),
          LogService::logRocketChatServiceError);
    }
  }

  private MessagesDTO decryptMessageAndSetMessageType(MessagesDTO msg, String rcGroupId) {
    decryptMessage(msg, rcGroupId);
    setMessageType(msg);

    return msg;
  }

  private void decryptMessage(MessagesDTO msg, String rcGroupId) {
    try {
      msg.setMsg(encryptionService.decrypt(msg.getMsg(), rcGroupId));
      msg.setOrg(encryptionService.decrypt(msg.getOrg(), rcGroupId));
    } catch (CustomCryptoException | NoMasterKeyException ex) {
      throw new InternalServerErrorException(ex, LogService::logEncryptionServiceError);
    }
  }

  private void setMessageType(MessagesDTO msg) {
    if (isNull(msg.getAlias()) || nonNull(msg.getAlias().getMessageType())) {
      return;
    }

    if (nonNull(msg.getAlias().getForwardMessageDTO())) {
      msg.getAlias().setMessageType(MessageType.FORWARD);
    } else {
      msg.getAlias().setMessageType(MessageType.VIDEOCALL);
    }
  }

  /**
   * Posts a message via Rocket.Chat API for the provided Rocket.Chat user in the provided group.
   *
   * @param chatMessage the message
   * @return PostMessageResponseDTO
   * @throws CustomCryptoException if text encryption failed
   */
  public SendMessageResponseDTO postGroupMessage(ChatMessage chatMessage)
      throws CustomCryptoException {

    var headers = getRocketChatHeader(chatMessage.getRcToken(), chatMessage.getRcUserId());

    var sendMessage = new SendMessageDTO(chatMessage.getRcGroupId(),
        extractMessageText(chatMessage), extractOrgMessageText(chatMessage), chatMessage.getAlias(),
        chatMessage.getType());
    var payload = new SendMessageWrapper(sendMessage);
    var request = new HttpEntity<>(payload, headers);

    try {
      return restTemplate.postForObject(rcSendMessageUrl, request, SendMessageResponseDTO.class);
    } catch (Exception ex) {
      LogService.logRocketChatServiceError(
          "Request body which caused the error was " + request.getBody());
      throw new InternalServerErrorException(ex, LogService::logRocketChatServiceError);
    }
  }

  private String extractMessageText(ChatMessage chatMessage) throws CustomCryptoException {
    if (isMessageE2eEncrypted(chatMessage)) {
      return chatMessage.getText();
    }
    return encryptText(chatMessage.getText(), chatMessage.getRcGroupId());
  }

  private String extractOrgMessageText(ChatMessage chatMessage) throws CustomCryptoException {
    if (isNotBlank(chatMessage.getOrgText())) {
      return encryptText(chatMessage.getOrgText(), chatMessage.getRcGroupId());
    }
    return chatMessage.getOrgText();
  }

  private boolean isMessageE2eEncrypted(ChatMessage chatMessage) {
    return E2E_ENCRYPTION_TYPE.equals(chatMessage.getType());
  }

  private String encryptText(String text, String rcGroupId) throws CustomCryptoException {
    var escaped = XssProtection.escapeHtml(text);
    return encryptionService.encrypt(escaped, rcGroupId);
  }

  public SendMessageResponseDTO postAliasOnlyMessageAsSystemUser(String rcGroupId,
      AliasMessageDTO aliasMessageDTO) {
    return postAliasOnlyMessageAsSystemUser(rcGroupId, aliasMessageDTO, null);
  }

  /**
   * Posts metadata contained in an {@link AliasMessageDTO} in the given Rocket.Chat group with an
   * empty message.
   *
   * @param rcGroupId       the Rocket.Chat group id
   * @param aliasMessageDTO {@link AliasMessageDTO}
   * @return {@link SendMessageResponseDTO}
   */
  public SendMessageResponseDTO postAliasOnlyMessageAsSystemUser(String rcGroupId,
      AliasMessageDTO aliasMessageDTO, String messageString) {
    var systemUser = retrieveSystemUser();
    var alias = JSONHelper.convertAliasMessageDTOToString(aliasMessageDTO).orElse(null);
    var aliasMessage = createAliasMessage(rcGroupId, systemUser, alias, messageString);

    try {
      return this.postGroupMessage(aliasMessage);
    } catch (CustomCryptoException e) {
      throw new InternalServerErrorException(e, LogService::logInternalServerError);
    }
  }

  private ChatMessage createAliasMessage(String rcGroupId, RocketChatCredentials systemUser,
      String alias, String message) {
    var text = isNull(message) ? EMPTY : message;

    return ChatMessage.builder()
        .rcToken(systemUser.getRocketChatToken())
        .rcUserId(systemUser.getRocketChatUserId())
        .rcGroupId(rcGroupId)
        .text(text)
        .alias(alias).build();
  }

  private RocketChatCredentials retrieveSystemUser() {
    try {
      return rcCredentialHelper.getSystemUser();
    } catch (RocketChatUserNotInitializedException e) {
      throw new InternalServerErrorException(e, LogService::logInternalServerError);
    }
  }

  private HttpHeaders getRocketChatHeader(String rcToken, String rcUserId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(rcHeaderAuthToken, rcToken);
    headers.add(rcHeaderUserId, rcUserId);

    return headers;
  }

  /**
   * Marks the specified Rocket.Chat group as read for the system (message) user.
   *
   * @param rcGroupId Rocket.Chat group ID
   */
  public void markGroupAsReadForSystemUser(String rcGroupId) {

    RocketChatCredentials rocketChatCredentials = retrieveSystemUser();

    if (areRequiredRocketChatParamsNotNull(rocketChatCredentials)) {
      this.markGroupAsRead(
          rocketChatCredentials.getRocketChatToken(),
          rocketChatCredentials.getRocketChatUserId(),
          rcGroupId);

    } else {
      LogService.logRocketChatServiceError(
          String.format("Could not set messages as read for system user in group %s", rcGroupId));
    }
  }

  private boolean areRequiredRocketChatParamsNotNull(RocketChatCredentials rocketChatCredentials) {
    return nonNull(rocketChatCredentials.getRocketChatToken())
        && nonNull(rocketChatCredentials.getRocketChatUserId());
  }

  private void markGroupAsRead(String rcToken, String rcUserId, String rcGroupId) {

    try {
      HttpHeaders headers = getRocketChatHeader(rcToken, rcUserId);
      PostGroupAsReadDTO postGroupAsReadDTO = new PostGroupAsReadDTO(rcGroupId);
      HttpEntity<PostGroupAsReadDTO> request = new HttpEntity<>(postGroupAsReadDTO, headers);

      restTemplate.postForObject(rcPostGroupMessagesRead, request, StandardResponseDTO.class);

    } catch (Exception ex) {
      throw new InternalServerErrorException(ex, LogService::logRocketChatServiceError);
    }
  }

  /**
   * Returns detailed group information for the given Rocket.Chat group ID.
   *
   * @param rcToken   Rocket.Chat token
   * @param rcUserId  Rocket.Chat user ID
   * @param rcGroupId Rocket.Chatgroup ID
   * @return {@link GetGroupInfoDto}
   */
  public GetGroupInfoDto getGroupInfo(String rcToken, String rcUserId, String rcGroupId) {

    try {
      URI uri = UriComponentsBuilder.fromUriString(rcGetGroupInfoUrl)
          .queryParam(rcQueryParamRoomId, rcGroupId)
          .build()
          .encode()
          .toUri();
      HttpEntity<?> entity = new HttpEntity<>(getRocketChatHeader(rcToken, rcUserId));

      return restTemplate.exchange(uri, HttpMethod.GET, entity, GetGroupInfoDto.class).getBody();

    } catch (HttpClientErrorException clientErrorEx) {
      throw new RocketChatBadRequestException(
          String.format(
              "Rocket.Chat API call failed with status %s for parameters rcUserId: %s, rcGroupId: %s)",
              clientErrorEx.getStatusCode(), rcUserId, rcGroupId),
          LogService::logRocketChatBadRequestError);
    }
  }

  public Message findMessage(String rcToken, String rcUserId, String messageId) {
    var url = baseUrl + ENDPOINT_MESSAGE_GET + messageId;
    var entity = new HttpEntity<>(getRocketChatHeader(rcToken, rcUserId));

    try {
      var response = restTemplate.exchange(url, HttpMethod.GET, entity, MessageResponse.class);
      if (nonNull(response.getBody())) {
        return response.getBody().getMessage();
      }
    } catch (HttpClientErrorException exception) {
      if (!isRcNotFoundResponse(exception)) {
        log.error("Chat Get-Message failed.", exception);
        var errorFormat = "Could not read message (%s) from Rocket.Chat API";
        var errorMessage = String.format(errorFormat, messageId);
        throw new InternalServerErrorException(errorMessage, LogService::logRocketChatServiceError);
      }
    }

    return null;
  }

  private boolean isRcNotFoundResponse(HttpClientErrorException exception) {
    return HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())
        && exception.getResponseBodyAsString().matches(".*\"success\"\\s*:\\s*false.*");
  }
}

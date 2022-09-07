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
import de.caritas.cob.messageservice.api.service.dto.UpdateMessage;
import de.caritas.cob.messageservice.api.service.helper.RocketChatCredentialsHelper;
import java.net.URI;
import java.time.Instant;
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
  private static final String ENDPOINT_MESSAGE_UPDATE = "/chat.update";

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

  /**
   * Gets the list of messages via Rocket.Chat API for the provided Rocket.Chat group. Filters out
   * technical user messages, decrypts the messages and sets the {@link MessageType}.
   *
   * @param rcToken   Rocket.Chat authentication token
   * @param rcUserId  Rocket.Chat user ID
   * @param rcGroupId Rocket.Chat group ID
   * @return MessageStreamDTO {@link MessageStreamDTO}
   */
  public MessageStreamDTO getGroupMessages(String rcToken, String rcUserId, String rcGroupId,
      int offset, int count, Instant since) {
    var uri = buildMessageStreamUri(rcGroupId, offset, count, since);
    var messageStream = obtainMessageStream(rcToken, rcUserId, uri);

    messageStream.setMessages(Optional.ofNullable(messageStream.getMessages())
        .orElseGet(Collections::emptyList)
        .stream()
        .map(msg -> decryptMessageAndSetMessageType(msg, rcGroupId))
        .map(mapper::typedMessageOf)
        .collect(Collectors.toList()));

    return messageStream;
  }

  private MessageStreamDTO obtainMessageStream(String rcToken, String rcUserId, URI uri) {
    HttpEntity<?> entity = new HttpEntity<>(getRocketChatHeader(rcToken, rcUserId));

    try {
      return restTemplate.exchange(uri, HttpMethod.GET, entity, MessageStreamDTO.class).getBody();

    } catch (RestClientException exception) {
      LogService.logRocketChatServiceError(exception);
      var msg = String.format("Could not read message stream from Rocket.Chat API (uri: %s)", uri);
      throw new InternalServerErrorException(msg, LogService::logRocketChatServiceError);
    }
  }

  private URI buildMessageStreamUri(String rcGroupId, int offset, int count, Instant instant) {
    try {
      return UriComponentsBuilder.fromUriString(rcGetGroupMessageUrl)
          .queryParam(rcQueryParamRoomId, rcGroupId)
          .queryParam(rcQueryParamOffset, offset)
          .queryParam(rcQueryParamCount, count)
          .queryParam(rcQueryParamSort, rcQueryParamSortValue)
          .queryParam("query", mapper.queryOperatorSinceAndNot(instant, rcTechnicalUser))
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
    var alias = msg.getAlias();
    if (nonNull(alias)) {
      alias.setMessageType(mapper.messageTypeOf(alias));
    }

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

  public SendMessageResponseDTO postGroupMessage(ChatMessage chatMessage)
      throws CustomCryptoException {
    return postGroupMessage(chatMessage, true);
  }

  /**
   * Posts a message via Rocket.Chat API for the provided Rocket.Chat user in the provided group.
   *
   * @param chatMessage the message
   * @return PostMessageResponseDTO
   * @throws CustomCryptoException if text encryption failed
   */
  public SendMessageResponseDTO postGroupMessage(ChatMessage chatMessage, boolean escapeMsg)
      throws CustomCryptoException {
    var headers = getRocketChatHeader(chatMessage.getRcToken(), chatMessage.getRcUserId());

    var msg = extractMessageText(chatMessage, escapeMsg);
    var sendMessage = new SendMessageDTO(chatMessage.getRcGroupId(), msg,
        extractOrgMessageText(chatMessage), chatMessage.getAlias(), chatMessage.getType());
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

  private String extractMessageText(ChatMessage chatMessage, boolean escapeMsg)
      throws CustomCryptoException {
    if (isMessageE2eEncrypted(chatMessage)) {
      return chatMessage.getText();
    }
    return encryptText(chatMessage.getText(), chatMessage.getRcGroupId(), escapeMsg);
  }

  private String extractOrgMessageText(ChatMessage chatMessage) throws CustomCryptoException {
    if (isNotBlank(chatMessage.getOrgText())) {
      return encryptText(chatMessage.getOrgText(), chatMessage.getRcGroupId(), true);
    }
    return chatMessage.getOrgText();
  }

  private boolean isMessageE2eEncrypted(ChatMessage chatMessage) {
    return E2E_ENCRYPTION_TYPE.equals(chatMessage.getType());
  }

  private String encryptText(String text, String rcGroupId, boolean escapeText)
      throws CustomCryptoException {
    if (escapeText) {
      text = XssProtection.escapeHtml(text);
    }

    return encryptionService.encrypt(text, rcGroupId);
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
      return postGroupMessage(aliasMessage, false);
    } catch (CustomCryptoException e) {
      throw new InternalServerErrorException(e, LogService::logInternalServerError);
    }
  }

  public boolean updateMessage(UpdateMessage message) {
    var systemUser = retrieveSystemUser();
    var headers = getRocketChatHeader(systemUser.getRocketChatToken(),
        systemUser.getRocketChatUserId());
    var request = new HttpEntity<>(message, headers);
    var url = baseUrl + ENDPOINT_MESSAGE_UPDATE;

    try {
      var response = restTemplate.postForObject(url, request, MessageResponse.class);
      return nonNull(response) && response.getSuccess();
    } catch (HttpClientErrorException exception) {
      log.error("Chat Update-Message failed.", exception);
      return false;
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
        var errorFormat = "Could not read message (%s) from Rocket.Chat API";
        var errorMessage = String.format(errorFormat, messageId);
        throw new InternalServerErrorException(errorMessage, LogService::logRocketChatServiceError);
      }
    }

    return null;
  }

  @SuppressWarnings("java:S5852") // Using slow regular expressions is security-sensitive
  private boolean isRcNotFoundResponse(HttpClientErrorException exception) {
    return HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())
        && exception.getResponseBodyAsString().matches("\\{.*\"success\"\\s*:\\s*false.*}");
  }
}

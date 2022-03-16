package de.caritas.cob.messageservice.api.service;

import static com.github.jknack.handlebars.internal.lang3.StringUtils.EMPTY;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.NoMasterKeyException;
import de.caritas.cob.messageservice.api.exception.RocketChatBadRequestException;
import de.caritas.cob.messageservice.api.exception.RocketChatUserNotInitializedException;
import de.caritas.cob.messageservice.api.helper.JSONHelper;
import de.caritas.cob.messageservice.api.helper.XssProtection;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.StandardResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.PostGroupAsReadDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageResponseDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class RocketChatService {

  @Value("${rocket.chat.api.get.group.message.url}")
  private String rcGetGroupMessageUrl;

  @Value("${rocket.chat.api.post.message.url}")
  private String rcPostMessageUrl;

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
        .collect(Collectors.toList()));

    return messageStream;
  }

  private MessageStreamDTO obtainMessageStream(String rcToken, String rcUserId, String rcGroupId) {
    URI uri = buildMessageStreamUri(rcGroupId);
    HttpEntity<?> entity = new HttpEntity<>(getRocketChatHeader(rcToken, rcUserId));

    try {
      log.info("Request groups.message: {}", uri);
      var body = restTemplate.exchange(uri, HttpMethod.GET, entity, MessageStreamDTO.class)
          .getBody();
      var bodyString = new ObjectMapper().writeValueAsString(body);
      log.info("Response groups.message: {}", bodyString);

      return body;

    } catch (RestClientException | JsonProcessingException exception) {
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
   * @param rcToken   Rocket.Chat authentication token
   * @param rcUserId  Rocket.Chat user Id
   * @param rcGroupId Rocket.Chat group Id
   * @param message   Rocket.Chat message
   * @return PostMessageResponseDTO
   */
  public PostMessageResponseDTO postGroupMessage(
      String rcToken, String rcUserId, String rcGroupId, String message, String alias)
      throws CustomCryptoException {

    message = XssProtection.escapeHtml(message);
    message = encryptionService.encrypt(message, rcGroupId);
    HttpHeaders headers = getRocketChatHeader(rcToken, rcUserId);
    PostMessageDTO postMessageDTO = new PostMessageDTO(rcGroupId, message, alias);
    HttpEntity<PostMessageDTO> request = new HttpEntity<>(postMessageDTO, headers);

    try {
      return restTemplate.postForObject(rcPostMessageUrl, request, PostMessageResponseDTO.class);
    } catch (Exception ex) {
      LogService.logRocketChatServiceError(
          "Request body which caused the error was " + request.getBody());
      throw new InternalServerErrorException(ex, LogService::logRocketChatServiceError);
    }
  }

  /**
   * Posts metadata contained in an {@link AliasMessageDTO} in the given Rocket.Chat group with an
   * empty message.
   *
   * @param rcGroupId       the Rocket.Chat group id
   * @param aliasMessageDTO {@link AliasMessageDTO}
   */
  public void postAliasOnlyMessageAsSystemUser(String rcGroupId, AliasMessageDTO aliasMessageDTO) {
    RocketChatCredentials systemUser = retrieveSystemUser();
    String alias = JSONHelper.convertAliasMessageDTOToString(aliasMessageDTO).orElse(null);

    try {
      this.postGroupMessage(systemUser.getRocketChatToken(), systemUser.getRocketChatUserId(),
          rcGroupId, EMPTY, alias);
    } catch (CustomCryptoException e) {
      throw new InternalServerErrorException(e, LogService::logInternalServerError);
    }
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
}

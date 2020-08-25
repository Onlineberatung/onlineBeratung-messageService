package de.caritas.cob.messageservice.api.service;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.NoMasterKeyException;
import de.caritas.cob.messageservice.api.exception.RocketChatBadRequestException;
import de.caritas.cob.messageservice.api.exception.RocketChatUserNotInitializedException;
import de.caritas.cob.messageservice.api.helper.Helper;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.StandardResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.PostGroupAsReadDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.helper.RocketChatCredentialsHelper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
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

  private @NonNull RestTemplate restTemplate;
  private @NonNull EncryptionService encryptionService;
  private @NonNull RocketChatCredentialsHelper rcCredentialHelper;

  /**
   * Gets the list of messages via Rocket.Chat API for the provided Rocket.Chat user and group
   *
   * @param rcToken Rocket.Chat authentication token
   * @param rcUserId Rocket.Chat user Id
   * @param rcGroupId Rocket.Chat group Id
   * @param rcOffset Number of items where to start in the query (0 = first item)
   * @param rcCount In MVP only 0 (all) or 1(one entry) are allowed - Number of item which are being
   *     returned (0 = all)
   * @return MessageStreamDTO
   */
  public MessageStreamDTO getGroupMessages(
      String rcToken, String rcUserId, String rcGroupId, int rcOffset, int rcCount) {

    try {
      URI uri =
          UriComponentsBuilder.fromUriString(rcGetGroupMessageUrl)
              .queryParam(rcQueryParamRoomId, rcGroupId)
              .queryParam(rcQueryParamOffset, rcOffset)
              .queryParam(rcQueryParamCount, 0) // Im MVP immer alles
              .queryParam(rcQueryParamSort, rcQueryParamSortValue)
              .build()
              .encode()
              .toUri();
      HttpEntity<?> entity = new HttpEntity<>(getRocketChatHeader(rcToken, rcUserId));

      HttpEntity<MessageStreamDTO> response =
          restTemplate.exchange(uri, HttpMethod.GET, entity, MessageStreamDTO.class);

      MessageStreamDTO messageStream =
          decryptMessages(requireNonNull(response.getBody()), rcGroupId);

      if (rcCount == 1) {
        updateToFirstMessage(messageStream);
      }
      return messageStream;
    } catch (HttpClientErrorException clientErrorEx) {
      throw new RocketChatBadRequestException(
          String.format(
              "Rocket.Chat API fails due to a bad request (rcUserId: %s, rcGroupId: %s, rcOffset: %s, rcCount: %s)",
              rcUserId, rcGroupId, rcOffset, rcCount),
          LogService::logRocketChatBadRequestError);
    } catch (CustomCryptoException | NoMasterKeyException ex) {
      throw new InternalServerErrorException(ex, LogService::logEncryptionServiceError);
    } catch (Exception ex) {
      throw new InternalServerErrorException(
          String.format(
              "Could not read message stream from Rocket.Chat API (rcUserId: %s, rcGroupId: %s, rcOffset: %s, rcCount: %s)",
              rcUserId, rcGroupId, rcOffset, rcCount),
          LogService::logRocketChatServiceError);
    }
  }

  /**
   * Decrypts all messages, deletes all messages from the technical User and updates the offset
   * accordingly
   *
   * @param dto The MessageStream, which needs to be updated
   * @return The updated MessageStreamDTO
   */
  private MessageStreamDTO decryptMessages(MessageStreamDTO dto, String rcGroupId)
      throws CustomCryptoException {
    if (CollectionUtils.isEmpty(dto.getMessages())) {
      return dto;
    }

    List<MessagesDTO> messages = dto.getMessages();
    List<MessagesDTO> decryptedMessages = new ArrayList<>();

    for (MessagesDTO message : messages) {
      if (!message.getU().getUsername().equals(rcTechnicalUser)) {
        message.setMsg(encryptionService.decrypt(message.getMsg(), rcGroupId));
        decryptedMessages.add(message);
      }
    }

    dto.setMessages(decryptedMessages);

    return dto;
  }

  private void updateToFirstMessage(MessageStreamDTO dto) {
    List<MessagesDTO> messages = dto.getMessages();
    if (CollectionUtils.isEmpty(messages)) {
      dto.setMessages(new ArrayList<>());
      dto.setCount("0");
    } else {
      MessagesDTO messagesDTO = messages.get(messages.size() - 1);
      messages.clear();
      messages.add(messagesDTO);
      dto.setMessages(messages);
      dto.setCount("1");
    }
  }

  /**
   * Posts a message via Rocket.Chat API for the provided Rocket.Chat user in the provided group
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

    // XSS-Protection
    message = Helper.removeHTMLFromText(message);

    message = encryptionService.encrypt(message, rcGroupId);

    try {
      HttpHeaders headers = getRocketChatHeader(rcToken, rcUserId);
      PostMessageDTO postMessageDTO = new PostMessageDTO(rcGroupId, message, alias);
      HttpEntity<PostMessageDTO> request = new HttpEntity<>(postMessageDTO, headers);

      return restTemplate.postForObject(rcPostMessageUrl, request, PostMessageResponseDTO.class);

    } catch (Exception ex) {
      throw new InternalServerErrorException(ex, LogService::logRocketChatServiceError);
    }
  }

  /**
   * Creates and returns the {@link HttpHeaders} with Rocket.Chat Authentication Token and User Id
   */
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

    RocketChatCredentials rocketChatCredentials;
    try {
      rocketChatCredentials = rcCredentialHelper.getSystemUser();
    } catch (RocketChatUserNotInitializedException e) {
      throw new InternalServerErrorException(e, LogService::logInternalServerError);
    }

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

  /**
   * Marks the specified Rocket.Chat group as read for the given user credentials.
   *
   * @param rcToken the rocket chat token
   * @param rcUserId the rocket chat user id
   * @param rcGroupId the rocket chat group id
   */
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

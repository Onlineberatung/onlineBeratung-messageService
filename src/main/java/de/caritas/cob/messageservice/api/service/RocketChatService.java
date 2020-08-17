package de.caritas.cob.messageservice.api.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.NoMasterKeyException;
import de.caritas.cob.messageservice.api.exception.RocketChatBadRequestException;
import de.caritas.cob.messageservice.api.exception.RocketChatGetGroupMessagesException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMarkGroupAsReadException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMessageException;
import de.caritas.cob.messageservice.api.helper.Helper;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.StandardResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.PostGroupAsReadDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.helper.RocketChatCredentialsHelper;
import lombok.extern.slf4j.Slf4j;

@Service
public class RocketChatService {

  @Value("${rocket.chat.api.get.group.message.url}")
  private String RC_GET_GROUP_MESSAGE_URL;

  @Value("${rocket.chat.api.post.message.url}")
  private String RC_POST_MESSAGE_URL;

  @Value("${rocket.chat.api.post.group.messages.read.url}")
  private String RC_POST_GROUP_MESSAGES_READ;

  @Value("${rocket.chat.api.user.login}")
  private String RC_POST_USER_LOGIN_URL;

  @Value("${rocket.technical.username}")
  private String RC_TECHNICAL_USER;

  @Value("${rocket.chat.header.auth.token}")
  private String RC_HEADER_AUTH_TOKEN;

  @Value("${rocket.chat.header.user.id}")
  private String RC_HEADER_USER_ID;

  @Value("${rocket.chat.query.param.room.id}")
  private String RC_QUERY_PARAM_ROOM_ID;

  @Value("${rocket.chat.query.param.offset}")
  private String RC_QUERY_PARAM_OFFSET;

  @Value("${rocket.chat.query.param.count}")
  private String RC_QUERY_PARAM_COUNT;

  @Value("${rocket.chat.query.param.sort}")
  private String RC_QUERY_PARAM_SORT;

  @Value("${rocket.chat.query.param.sort.value}")
  private String RC_QUERY_PARAM_SORT_VALUE;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private EncryptionService encryptionService;

  @Autowired
  private RocketChatCredentialsHelper rcCredentialHelper;

  /**
   * Gets the list of messages via Rocket.Chat API for the provided Rocket.Chat user and group
   * 
   * @param rcToken Rocket.Chat authentication token
   * @param rcUserId Rocket.Chat user Id
   * @param rcGroupId Rocket.Chat group Id
   * @param rcOffset Number of items where to start in the query (0 = first item)
   * @param rcCount In MVP only 0 (all) or 1(one entry) are allowed - Number of item which are being
   *        returned (0 = all)
   * @return MessageStreamDTO
   */
  public MessageStreamDTO getGroupMessages(String rcToken, String rcUserId, String rcGroupId,
      int rcOffset, int rcCount) {

    HttpEntity<MessageStreamDTO> response;
    MessageStreamDTO messageStream = null;

    try {
      URI uri = UriComponentsBuilder.fromUriString(RC_GET_GROUP_MESSAGE_URL)
          .queryParam(RC_QUERY_PARAM_ROOM_ID, rcGroupId).queryParam(RC_QUERY_PARAM_OFFSET, rcOffset)
          .queryParam(RC_QUERY_PARAM_COUNT, 0) // Im MVP immer alles
          .queryParam(RC_QUERY_PARAM_SORT, RC_QUERY_PARAM_SORT_VALUE).build().encode().toUri();
      HttpEntity<?> entity = new HttpEntity<>(getRocketChatHeader(rcToken, rcUserId));

      response = restTemplate.exchange(uri, HttpMethod.GET, entity, MessageStreamDTO.class);

      if (response != null) {
        messageStream = decryptMessagesAndremoveTechnicalMessages(response.getBody(), rcGroupId);

        if (rcCount == 1) {
          messageStream = getFirstMessage(messageStream);
        }
      }

    } catch (HttpClientErrorException clientErrorEx) {
      LogService.logRocketChatBadRequestError(clientErrorEx);
      throw new RocketChatBadRequestException(String.format(
          "Rocket.Chat API fails due to a bad request (rcUserId: %s, rcGroupId: %s, rcOffset: %s, rcCount: %s)",
          rcUserId, rcGroupId, rcOffset, rcCount));
    } catch (CustomCryptoException | NoMasterKeyException ex) {

      LogService.logEncryptionServiceError(ex);
      throw ex;

    } catch (Exception ex) {

      LogService.logRocketChatServiceError(ex);
      throw new RocketChatGetGroupMessagesException(String.format(
          "Could not read message stream from Rocket.Chat API (rcUserId: %s, rcGroupId: %s, rcOffset: %s, rcCount: %s)",
          rcUserId, rcGroupId, rcOffset, rcCount));
    }

    return messageStream;
  }

  /**
   * Decrypts all messages, deletes all messages from the technical User and updates the offset
   * accordingly
   * 
   * 
   * @param dto The MessageStream, which needs to be updated
   * @param rcGroupId
   * @return The updated MessageStreamDTO
   */
  private MessageStreamDTO decryptMessagesAndremoveTechnicalMessages(MessageStreamDTO dto,
      String rcGroupId) {
    int deleteCounter = 0;
    List<MessagesDTO> messages = dto.getMessages();

    List<MessagesDTO> decryptedMessages = new ArrayList<>();

    for (MessagesDTO message : messages) {
      if (!message.getU().getUsername().equals(RC_TECHNICAL_USER)) {
        message.setMsg(encryptionService.decrypt(message.getMsg(), rcGroupId));
        decryptedMessages.add(message);
      }
    }

    dto.setMessages(decryptedMessages);

    if (deleteCounter > 0) {
      dto.setCleaned(Integer.toString(deleteCounter));

      int messageCount = Integer.parseInt(dto.getCount());
      dto.setCount(Integer.toString(messageCount - deleteCounter));

      int messageTotal = Integer.parseInt(dto.getTotal());
      dto.setTotal(Integer.toString(messageTotal - deleteCounter));

      dto.setMessages(messages);
    }

    return dto;
  }

  private MessageStreamDTO getFirstMessage(MessageStreamDTO dto) {
    List<MessagesDTO> messages = dto.getMessages();
    if (messages == null || messages.size() == 0) {
      dto.setMessages(new ArrayList<MessagesDTO>());
      dto.setCount("0");
      return dto;
    }
    MessagesDTO messagesDTO = messages.get(messages.size() - 1);
    messages.clear();
    messages.add(messagesDTO);
    dto.setMessages(messages);
    dto.setCount("1");
    return dto;
  }

  /**
   * Posts a message via Rocket.Chat API for the provided Rocket.Chat user in the provided group
   * 
   * @param rcToken Rocket.Chat authentication token
   * @param rcUserId Rocket.Chat user Id
   * @param rcGroupId Rocket.Chat group Id
   * @param message Rocket.Chat message
   * @return PostMessageResponseDTO
   */
  public PostMessageResponseDTO postGroupMessage(String rcToken, String rcUserId, String rcGroupId,
      String message, String alias) {

    PostMessageResponseDTO response = null;
    // XSS-Protection
    message = Helper.removeHTMLFromText(message);

    message = encryptionService.encrypt(message, rcGroupId);

    try {
      HttpHeaders headers = getRocketChatHeader(rcToken, rcUserId);
      PostMessageDTO postMessageDTO = new PostMessageDTO(rcGroupId, message, alias);
      HttpEntity<PostMessageDTO> request = new HttpEntity<PostMessageDTO>(postMessageDTO, headers);

      response =
          restTemplate.postForObject(RC_POST_MESSAGE_URL, request, PostMessageResponseDTO.class);

    } catch (Exception ex) {
      LogService.logRocketChatServiceError(ex);
      throw new RocketChatPostMessageException(ex);
    }

    return response;
  }

  /**
   * Creates and returns the {@link HttpHeaders} with Rocket.Chat Authentication Token and User Id
   * 
   * @param rcToken
   * @param rcUserId
   * @return
   */
  private HttpHeaders getRocketChatHeader(String rcToken, String rcUserId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(RC_HEADER_AUTH_TOKEN, rcToken);
    headers.add(RC_HEADER_USER_ID, rcUserId);

    return headers;
  }

  /**
   * Marks the specified Rocket.Chat group as read for the system (message) user
   * 
   * @param rcGroupId
   * @return
   */
  public boolean markGroupAsReadForSystemUser(String rcGroupId) {

    RocketChatCredentials rocketChatCredentials = rcCredentialHelper.getSystemUser();

    if (rocketChatCredentials.getRocketChatToken() != null
        && rocketChatCredentials.getRocketChatUserId() != null) {
      return this.markGroupAsRead(rocketChatCredentials.getRocketChatToken(),
          rocketChatCredentials.getRocketChatUserId(), rcGroupId);

    } else {
      LogService.logRocketChatServiceError(
          String.format("Could not set messages as read for system user in group %s", rcGroupId));
      return false;
    }
  }

  /**
   * Marks the specified Rocket.Chat group as read for the given user credentials
   * 
   * @param rcToken
   * @param rcUserId
   * @param rcGroupId
   * @return
   */
  private boolean markGroupAsRead(String rcToken, String rcUserId, String rcGroupId) {

    StandardResponseDTO response;

    try {
      HttpHeaders headers = getRocketChatHeader(rcToken, rcUserId);
      PostGroupAsReadDTO postGroupAsReadDTO = new PostGroupAsReadDTO(rcGroupId);
      HttpEntity<PostGroupAsReadDTO> request =
          new HttpEntity<PostGroupAsReadDTO>(postGroupAsReadDTO, headers);

      response = restTemplate.postForObject(RC_POST_GROUP_MESSAGES_READ, request,
          StandardResponseDTO.class);

    } catch (Exception ex) {
      LogService.logRocketChatServiceError(ex);
      throw new RocketChatPostMarkGroupAsReadException(ex);
    }

    return response != null && response.isSuccess();
  }

}

package de.caritas.cob.messageservice.api.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.api.model.MessageResponseDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.dto.Message;
import de.caritas.cob.messageservice.api.service.dto.UpdateMessage;
import java.util.Date;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageMapper {

  private final ObjectMapper objectMapper;
  private final EncryptionService encryptionService;

  public MessagesDTO typedMessageOf(MessagesDTO messagesDTO) {
    var messageType = messagesDTO.getT();

    if (nonNull(messageType)) {
      if (messageType.equalsIgnoreCase("user-muted")) {
        var alias = aliasMessageDtoOf(MessageType.USER_MUTED);
        messagesDTO.setAlias(alias);
      } else if (messageType.equalsIgnoreCase("user-unmuted")) {
        var alias = aliasMessageDtoOf(MessageType.USER_UNMUTED);
        messagesDTO.setAlias(alias);
      }
    }

    return messagesDTO;
  }

  public AliasMessageDTO aliasMessageDtoOf(MessageType messageType) {
    var alias = new AliasMessageDTO();
    alias.setMessageType(messageType);

    return alias;
  }

  public MessageResponseDTO messageResponseOf(SendMessageResponseDTO sendMessageResponse) {
    var message = sendMessageResponse.getMessage();
    return new MessageResponseDTO().msg(message.getMsg())
        .id(message.getId())
        .rid(message.getRid())
        .ts(message.getTimestamp() != null ? message.getTimestamp().toString()
            : new Date().toString())
        .updatedAt(message.getUpdatedAt() != null ? message.getUpdatedAt().toString()
            : new Date().toString())
        .e2e(message.getE2e())
        .t(message.getT())
        .org(message.getOrg());
  }

  public String messageStringOf(AliasArgs aliasArgs) {
    if (isNull(aliasArgs)) {
      return null;
    }

    try {
      return objectMapper.writeValueAsString(aliasArgs);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public ConsultantReassignment consultantReassignmentOf(Message message) {
    try {
      var foundMsgString = encryptionService.decrypt(message.getMsg(), message.getRid());
      foundMsgString = foundMsgString.replace("&quot;", "\"");
      return objectMapper.readValue(foundMsgString, ConsultantReassignment.class);
    } catch (JsonProcessingException | CustomCryptoException e) {
      throw new RuntimeException(e);
    }
  }

  public UpdateMessage updateMessageOf(Message message, ConsultantReassignment reassignment) {
    try {
      var text = objectMapper.writeValueAsString(reassignment);
      var encryptedText = encryptionService.encrypt(text, message.getRid());

      var updatedMessage = new UpdateMessage();
      updatedMessage.setRoomId(message.getRid());
      updatedMessage.setMsgId(message.getId());
      updatedMessage.setText(encryptedText);

      return updatedMessage;
    } catch (JsonProcessingException | CustomCryptoException e) {
      throw new RuntimeException(e);
    }
  }

  public String queryOperatorNot(String username) {
    var queryMap = Map.of("u.username", Map.of("$ne", username));
    try {
      return objectMapper.writeValueAsString(queryMap);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}

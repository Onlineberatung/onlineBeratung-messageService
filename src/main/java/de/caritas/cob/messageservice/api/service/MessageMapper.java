package de.caritas.cob.messageservice.api.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.MessageResponseDTO;
import de.caritas.cob.messageservice.api.model.MessageText;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageMapper {

  private final ObjectMapper objectMapper;

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

  public String messageStringOf(MessageText message) {
    if (isNull(message)) {
      return null;
    }

    try {
      return objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}

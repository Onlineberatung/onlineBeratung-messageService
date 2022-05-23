package de.caritas.cob.messageservice.api.service;

import static java.util.Objects.nonNull;

import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.MessageResponseDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class MessageMapper {

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

  private AliasMessageDTO aliasMessageDtoOf(MessageType messageType) {
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
        .t(message.getT());
  }
}

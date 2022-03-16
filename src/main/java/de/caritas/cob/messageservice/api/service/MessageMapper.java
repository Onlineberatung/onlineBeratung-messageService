package de.caritas.cob.messageservice.api.service;

import static java.util.Objects.nonNull;

import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
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
}

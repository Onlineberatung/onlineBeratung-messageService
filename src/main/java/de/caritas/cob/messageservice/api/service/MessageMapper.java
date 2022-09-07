package de.caritas.cob.messageservice.api.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.NoMasterKeyException;
import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.api.model.MessageResponseDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.dto.Message;
import de.caritas.cob.messageservice.api.service.dto.UpdateMessage;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
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

  public Message typedMessageOf(Message message) {
    var messageType = (String) message.getOtherProperties().get("t");

    if (nonNull(messageType)) {
      AliasMessageDTO alias = null;
      if (messageType.equalsIgnoreCase("user-muted")) {
        alias = aliasMessageDtoOf(MessageType.USER_MUTED);
      } else if (messageType.equalsIgnoreCase("user-unmuted")) {
        alias = aliasMessageDtoOf(MessageType.USER_UNMUTED);
      }
      if (nonNull(alias)) {
        try {
          message.setAlias(objectMapper.writeValueAsString(alias));
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return message;
  }

  public Message decryptedMessageOf(Message message) {
    try {
      message.setMsg(encryptionService.decrypt(message.getMsg(), message.getRid()));
      var others = message.getOtherProperties();
      others.put("org", encryptionService.decrypt((String) others.get("org"), message.getRid()));
    } catch (CustomCryptoException | NoMasterKeyException ex) {
      throw new InternalServerErrorException(ex, LogService::logEncryptionServiceError);
    }

    return message;
  }

  public AliasMessageDTO aliasMessageDtoOf(MessageType messageType) {
    var alias = new AliasMessageDTO();
    alias.setMessageType(messageType);

    return alias;
  }

  public MessagesDTO messageDtoOf(Message message) {
    var messageDto = new MessagesDTO();
    messageDto.set_id(message.getId());
    messageDto.setMsg(message.getMsg());
    messageDto.setRid(message.getRid());

    var decodedAlias = URLDecoder.decode(message.getAlias(), StandardCharsets.UTF_8);
    try {
      var alias = objectMapper.readValue(decodedAlias, AliasMessageDTO.class);
      if (nonNull(alias)) {
        alias.setMessageType(messageTypeOf(alias));
      }
      messageDto.setAlias(alias);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    var obsoleteMsg = objectMapper.convertValue(message.getOtherProperties(), MessagesDTO.class);
    messageDto.setTs(obsoleteMsg.getTs());
    messageDto.setU(obsoleteMsg.getU());
    messageDto.setUnread(obsoleteMsg.isUnread());
    messageDto.set_updatedAt(obsoleteMsg.get_updatedAt());
    messageDto.setAttachments(obsoleteMsg.getAttachments());
    messageDto.setFile(obsoleteMsg.getFile());
    messageDto.setT(obsoleteMsg.getT());
    messageDto.setOrg(obsoleteMsg.getOrg());

    return messageDto;
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

  public String queryOperatorSinceAndNot(Instant since, String username) {
    var olderThan = Map.of("ts", Map.of("$gt", Map.of("$date", since.toString())));
    var notUser = Map.of("u.username", Map.of("$ne", username));
    var op = Map.of("$and", List.of(olderThan, notUser));
    try {
      return objectMapper.writeValueAsString(op);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public MessageType messageTypeOf(AliasMessageDTO alias) {
    if (nonNull(alias)) {
      if (nonNull(alias.getMessageType())) {
        return alias.getMessageType();
      }

      if (nonNull(alias.getForwardMessageDTO())) {
        return MessageType.FORWARD;
      }

      if (nonNull(alias.getVideoCallMessageDTO())) {
        return MessageType.VIDEOCALL;
      }
    }

    return null;
  }
}

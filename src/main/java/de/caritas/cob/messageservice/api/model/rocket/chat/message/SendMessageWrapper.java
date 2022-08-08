package de.caritas.cob.messageservice.api.model.rocket.chat.message;

import lombok.Data;

@Data
public class SendMessageWrapper {

  private final SendMessageDTO message;
}

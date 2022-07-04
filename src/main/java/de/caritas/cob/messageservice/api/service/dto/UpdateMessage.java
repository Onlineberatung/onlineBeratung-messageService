package de.caritas.cob.messageservice.api.service.dto;

import lombok.Data;

@Data
public class UpdateMessage {

  private String roomId;

  private String msgId;

  private String text;
}

package de.caritas.cob.messageservice.api.service.dto;

import lombok.Data;

@Data
public class MessageResponse {

  private Message message;

  private Boolean success;
}

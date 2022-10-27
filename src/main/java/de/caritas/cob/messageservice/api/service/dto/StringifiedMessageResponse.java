package de.caritas.cob.messageservice.api.service.dto;

import lombok.Data;

@Data
public class StringifiedMessageResponse {

  private String message;

  private Boolean success;
}

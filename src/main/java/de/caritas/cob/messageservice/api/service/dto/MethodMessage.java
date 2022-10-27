package de.caritas.cob.messageservice.api.service.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MethodMessage {

  private String msg;

  private int id;

  private String method;

  private List<Map<String, String>> params;
}

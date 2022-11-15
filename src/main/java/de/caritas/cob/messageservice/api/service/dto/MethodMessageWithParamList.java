package de.caritas.cob.messageservice.api.service.dto;

import java.util.List;
import lombok.Data;

@Data
public class MethodMessageWithParamList {

  private String msg = "method";

  private int id;

  private String method;

  private List<String> params;
}

package de.caritas.cob.messageservice.api.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessage {

  private String rcToken;
  private String rcUserId;
  private String rcGroupId;
  private String text;
  private String alias;
  private String type;
  private boolean sendNotification;

}

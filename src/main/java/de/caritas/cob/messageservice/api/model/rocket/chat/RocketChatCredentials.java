package de.caritas.cob.messageservice.api.model.rocket.chat;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representation of Rocket.Chat credentials for the technical user.
 *
 */

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RocketChatCredentials {

  private String RocketChatToken;
  private String RocketChatUserId;
  private String RocketChatUsername;
  private LocalDateTime TimeStampCreated;
}

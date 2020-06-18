package de.caritas.cob.MessageService.api.model.rocket.chat.login;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * EmailsDTO for LoginResponseDTO
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class EmailsDTO {
  private String address;
  private boolean verified;
}

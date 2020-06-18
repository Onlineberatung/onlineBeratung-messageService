package de.caritas.cob.MessageService.api.model.rocket.chat.login;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DataDTO for LoginResponseDTO
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DataDTO {
  private String userId;
  private String authToken;
  private MeDTO me;
}

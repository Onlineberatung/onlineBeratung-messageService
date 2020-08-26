package de.caritas.cob.messageservice.api.model.rocket.chat.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GetGroupInfoResponseDto: Rocket.Chat API response object for group infos.
 *
 * <p>https://docs.rocket.chat/api/rest-api/methods/groups/info
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetGroupInfoDto {

  private GroupDto group;
  private boolean success;
}

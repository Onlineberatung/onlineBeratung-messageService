package de.caritas.cob.messageservice.api.model.rocket.chat.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GroupDto for {@link GetGroupInfoDto}
 *
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupDto {

  @JsonProperty("_id")
  private String roomId;
  private String name;
}

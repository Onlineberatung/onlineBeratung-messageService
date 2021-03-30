package de.caritas.cob.messageservice.api.model.rocket.chat.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.caritas.cob.messageservice.api.model.jsondeserializer.DecodeUsernameJsonDeserializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rocket.Chat user model (sub of MessagesDTO)
 * 
 * https://rocket.chat/docs/developer-guides/rest-api/groups/messages/
 *
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "U")
public class UserDTO {

  @ApiModelProperty(required = true, example = "vppRFqjrzTsTZ6iEn", position = 0)
  private String _id;

  @ApiModelProperty(required = true, example = "test", position = 1)
  @JsonDeserialize(using = DecodeUsernameJsonDeserializer.class)
  private String username;

  @ApiModelProperty(required = true, example = "Mustermax", position = 3)
  private String name;
}

package de.caritas.cob.MessageService.api.model.rocket.chat.message.attachment;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rocket.Chat file model (sub of MessagesDTO)
 * 
 * https://rocket.chat/docs/developer-guides/rest-api/groups/messages/
 *
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileDTO {

  @ApiModelProperty(required = true, example = "M73fE4WhYF4peYB3s", position = 0)
  private String _id;

  @ApiModelProperty(required = true, example = "filename.jpg", position = 1)
  private String name;

  @ApiModelProperty(required = true, example = "image/jepg", position = 1)
  private String type;

}

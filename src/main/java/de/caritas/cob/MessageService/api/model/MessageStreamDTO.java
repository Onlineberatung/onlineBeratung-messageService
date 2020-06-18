package de.caritas.cob.MessageService.api.model;

import java.util.List;
import de.caritas.cob.MessageService.api.model.rocket.chat.message.MessagesDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MessageStreamDTO: Rocket.Chat API response object for (group) messages
 * 
 * https://rocket.chat/docs/developer-guides/rest-api/groups/messages/
 *
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "MessageStream")
public class MessageStreamDTO {

  @ApiModelProperty(required = true, position = 0)
  private List<MessagesDTO> messages;

  @ApiModelProperty(required = true, example = "2", position = 1)
  private String count;

  @ApiModelProperty(required = true, example = "0", position = 2)
  private String offset;

  @ApiModelProperty(required = true, example = "2", position = 3)
  private String total;

  @ApiModelProperty(required = true, example = "true", position = 4)
  private String success;

  @ApiModelProperty(required = true, example = "true", position = 5)
  private String cleaned;
}

package de.caritas.cob.messageservice.api.model.rocket.chat.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.AttachmentDTO;
import de.caritas.cob.messageservice.api.model.FileDTO;
import de.caritas.cob.messageservice.api.model.jsondeserializer.AliasJsonDeserializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rocket.Chat message model (sub of MessageStreamDTO)
 * 
 * https://rocket.chat/docs/developer-guides/rest-api/groups/messages/
 *
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "Messages")
public class MessagesDTO {

  @ApiModelProperty(required = true, example = "M73fE4WhYF4peYB3s", position = 0)
  private String _id;

  @ApiModelProperty(required = false, example = "%7B%0A%20%20%22message%22%3A%20", position = 1)
  @JsonDeserialize(using = AliasJsonDeserializer.class)
  private AliasMessageDTO alias;

  @ApiModelProperty(required = true, example = "fR2Rz7dmWmHdXE8uz", position = 2)
  private String rid;

  @ApiModelProperty(required = true, example = "Lorem ipsum dolor sit amet, consetetur...",
      position = 3)
  private String msg;

  @ApiModelProperty(required = true, example = "2018-11-15T09:33:00.057Z", position = 4)
  private String ts;

  @ApiModelProperty(required = true, position = 5)
  private UserDTO u;

  @ApiModelProperty(required = true, position = 6)
  private boolean unread;

  @JsonIgnore
  @ApiModelProperty(required = true, position = 7)
  private String[] mentions;

  @JsonIgnore
  @ApiModelProperty(required = true, position = 8)
  private String[] channels;

  @ApiModelProperty(required = true, example = "2018-11-15T09:33:00.067Z", position = 9)
  private String _updatedAt;

  @ApiModelProperty(required = false, position = 10)
  private AttachmentDTO[] attachments;

  @ApiModelProperty(required = false, position = 11)
  private FileDTO file;


}

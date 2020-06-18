package de.caritas.cob.MessageService.api.model;

import javax.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POSTMessage model
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ApiModel(value = "PostMessage")
public class MessageDTO {

  @NotBlank(message = "{rocket.chat.message.notBlank}")
  @ApiModelProperty(required = true, example = "Lorem ipsum dolor sit amet, consetetur...",
      position = 0)
  @JsonProperty("message")
  private String message;

  @ApiModelProperty(required = false, example = "true", position = 1)
  private boolean sendNotification;
}

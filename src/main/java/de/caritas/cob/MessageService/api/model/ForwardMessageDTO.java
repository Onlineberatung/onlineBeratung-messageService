package de.caritas.cob.MessageService.api.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ForwardMessageDTO model
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ApiModel(value = "ForwardMessage")
public class ForwardMessageDTO {

  @NotBlank(message = "{rocket.chat.forward.message.notBlank}")
  @ApiModelProperty(required = true, example = "Lorem ipsum dolor sit amet, consetetur...",
      position = 0)
  private String message;

  @NotNull(message = "{rocket.chat.forward.timestamp.notBlank}")
  @ApiModelProperty(required = true, example = "2018-11-15T09:33:00.057Z", position = 1)
  private String timestamp;

  @NotBlank(message = "{rocket.chat.forward.username.notBlank}")
  @ApiModelProperty(required = true, example = "asker23", position = 2)
  private String username;

  @NotBlank(message = "{rocket.chat.forward.rcUserId.notBlank}")
  @ApiModelProperty(required = true, example = "ag89h3tjkerg94t", position = 3)
  private String rcUserId;

  // Ignore message for JSON deserialization of the alias object
  @JsonIgnore
  public String getMessage() {
    return message;
  }

  @JsonProperty
  public void setMessage(String message) {
    this.message = message;
  }
}

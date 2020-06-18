package de.caritas.cob.MessageService.api.model.rocket.chat.message.attachment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rocket.Chat attachment model (sub of MessagesDTO)
 * 
 * https://rocket.chat/docs/developer-guides/rest-api/groups/messages/
 *
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentDTO {

  @ApiModelProperty(required = true, example = "filename.png", position = 0)
  private String title;

  @ApiModelProperty(required = true, example = "file", position = 1)
  private String type;

  @ApiModelProperty(required = true, example = "Description", position = 2)
  private String description;

  @ApiModelProperty(required = true, example = "/file-upload/ijxact7nd5SMpSwiS/file.png",
      position = 3)
  @JsonProperty("title_link")
  private String titleLink;

  @ApiModelProperty(required = true, example = "true", position = 4)
  @JsonProperty("title_link_download")
  private boolean titleLinkDownload;

  @ApiModelProperty(required = true, example = "/file-upload/ijxact7nd5SMpSwiS/file.png",
      position = 5)
  @JsonProperty("image_url")
  private String imageUrl;

  @ApiModelProperty(required = true, example = "image/png", position = 6)
  @JsonProperty("image_type")
  private String imageType;

  @ApiModelProperty(required = true, example = "36461", position = 7)
  @JsonProperty("image_size")
  private int imageSize;

  @ApiModelProperty(required = true, example = "/9j/2wBDAAYEBQYFBAYGBQY", position = 8)
  @JsonProperty("image_preview")
  private String imagePreview;

}

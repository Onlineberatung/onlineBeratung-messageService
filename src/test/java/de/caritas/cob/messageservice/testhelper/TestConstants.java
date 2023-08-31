package de.caritas.cob.messageservice.testhelper;

import de.caritas.cob.messageservice.api.model.ChatMessage;
import de.caritas.cob.messageservice.api.model.MessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GroupDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResultDTO;
import java.util.Date;
import org.apache.commons.lang3.RandomStringUtils;

public class TestConstants {

  /*
   * Common
   */

  public static final String TIMESTAMP = "1568128850636";

  /*
   * Rocket.Chat
   */
  public static String RC_USER_ID = "p5NdZSxc2Kh7GfXdB";
  public static final String RC_TOKEN = "r94qMDk8gtgVNzqCq9zD2hELK-eXGB5VHlUVBgE8a8f";
  public static final String RC_OFFSET = "0";
  public static final String RC_COUNT = "0";
  public static final String RC_GROUP_ID = "fR2Rz7dmWmHdXE8uz";
  public static final String RC_GROUP_NAME = "405_1590499423659";
  public static final String RC_FEEDBACK_GROUP_ID = "Ad3RzsdmWmHdXE8xy";
  public static final String RC_FEEDBACK_GROUP_NAME = "405_feedback_1590499423600";
  public static final String RC_TIMESTAMP = "2018-11-15T09:32:55.045Z";
  public static final String RC_ATTACHMENT_ID = "aR2Rz7dmWmHdXE8u3";
  public static final String RC_ATTACHMENT_TITLE = "filename.jpg";
  public static final String RC_ATTACHMENT_DESCRIPTION = "A picture";
  public static final String RC_ATTACHMENT_FILE_TYPE = "image/jpeg";
  public static final String RC_ATTACHMENT_IMAGE_TYPE = "image/jpeg";
  public static final int RC_ATTACHMENT_IMAGE_SIZE = 737089;
  public static final String RC_ATTACHMENT_TITLE_LINK =
      "/file-upload/QbAtLCmrxJt9GHGeB/filename.jpg";
  public static final boolean RC_ATTACHMENT_TITLE_LINK_DOWNLOAD = true;
  public static final String RC_ATTACHMENT_IMAGE_URL =
      "/file-upload/QbAtLCmrxJt9GHGeB/filename.jpg";
  public static final String RC_ATTACHMENT_IMAGE_PREVIEW =
      "/9j/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAAVACADASIAAhEBAxEB/8QAGQAAAgMBAAAAAAAAAAAAAAAAAAYDBAUH/8QAKBAAAQMDBAAFBQAAAAAAAAAAAQIDBAAFBhESITETIkFRgQcjQmHw/8QAGAEAAwEBAAAAAAAAAAAAAAAAAgMEAQX/xAAdEQADAAICAwAAAAAAAAAAAAAAAQIDEQQSIjEy/9oADAMBAAIRAxEAPwCjapLjU7YhHlA+77I+aY0XNgHxZGo52pI9RSvIx6LerBPbsd0U1JLv56jcNff4rHstquEF+PFuc0uBscacgfNc+uJExpMzrpGxdszGL5avw5BYQ+gHno1LdMzs0yKXX56FSFebeDyKzc6iW+4WZ5TzBelAbG3D6HQ/qlbHcPiSVoTKhLUdnSOdDxyafgfWFsKZO24JFiy8ZZfXFaCndSfL1/a1PZ7ZEl3p1l9lJbSNQANKKKg5NNNJDn7QqJsEef8AUB+1OrWIpOoHe3vqnqxYnGs8mT4D7i1ngKUOhRRV+L4AT8j/2Q==";

  /*
   * User
   */
  public static final String ENCODING_PREFIX = "enc.";
  public static final String USERNAME_ENCODED = ENCODING_PREFIX + "OVZWK4TOMFWWK...";
  public static final String USERNAME_DECODED = "username";

  /* Consultant */
  public static final String CONSULTANT_ID = "d63f4cc0-215d-40e2-a866-2d3e910f0590";

  public static final String ADVICESEEKER_ID = "c34g4cc0-215d-40e2-a866-2d3e910f0440";

  /*
   * Messages
   */
  public static final String MESSAGE = "Lorem ipsum";
  public static final String MESSAGE_FORWARD_ALIAS_JSON_WITH_ENCODED_USERNAME =
      "{\"alias\":\"%7B%22timestamp%22%3A%221568128850636%22%2C%22username%22%3A%22"
          + USERNAME_ENCODED
          + "%22%2C%22rcUserId%22%3A%22p5NdZSxc2Kh7GfXdB%22%7D\"";
  public static final String MESSAGE_FORWARD_ALIAS_JSON_WITH_DECODED_USERNAME =
      "{\"alias\":\"%7B%22timestamp%22%3A%221568128850636%22%2C%22username%22%3A%22"
          + USERNAME_DECODED
          + "%22%2C%22rcUserId%22%3A%22p5NdZSxc2Kh7GfXdB%22%7D\"";
  public static final String MESSAGE_ALIAS_VIDEOCALL_JSON_WITH_ENCODED_USERNAME =
      "{\"alias\":\"%7B%22timestamp%22%3A%221568128850636%22%2C%22username%22%3A%22"
          + USERNAME_ENCODED
          + "%22%2C%22rcUserId%22%3A%22p5NdZSxc2Kh7GfXdB%22%7D\"";
  public static final String MESSAGE_ALIAS_VIDEOCALL_JSON_WITH_DECODED_USERNAME =
      "{\"alias\":\"%7B%22timestamp%22%3A%221568128850636%22%2C%22username%22%3A%22"
          + USERNAME_DECODED
          + "%22%2C%22rcUserId%22%3A%22p5NdZSxc2Kh7GfXdB%22%7D\"";
  public static final String COMPLETE_MESSAGE_ALIAS_DTO_JSON_WITH_ENCODED_USERNAMES =
      "{\"alias\":\"%7B%22timestamp%22%3A%221568128850636%22%2C%22username%22%3A%22"
          + USERNAME_DECODED
          + "%22%2C%22rcUserId%22%3A%22p5NdZSxc2Kh7GfXdB%22%7D\"";
  public static final String MESSAGE_ALIAS_DTO_EMPTY_ALIAS_JSON = "{\"alias\":\"\"";
  public static final String MESSAGE_ALIAS_DTO_NULL_ALIAS_JSON = "{\"alias\":null";
  public static final boolean SEND_NOTIFICATION = true;
  public static final boolean DONT_SEND_NOTIFICATION = false;
  public static final MessageDTO MESSAGE_DTO_WITH_NOTIFICATION =
      new MessageDTO().message(MESSAGE).sendNotification(SEND_NOTIFICATION);
  public static final MessageDTO MESSAGE_DTO_WITHOUT_NOTIFICATION =
      new MessageDTO().message(MESSAGE).sendNotification(DONT_SEND_NOTIFICATION);

  /*
   * Groups
   */
  public static final GroupDto GROUP_DTO = new GroupDto(RC_GROUP_ID, RC_GROUP_NAME);
  public static final GetGroupInfoDto GET_GROUP_INFO_DTO = new GetGroupInfoDto(GROUP_DTO, true);
  public static final GroupDto GROUP_DTO_FEEDBACK_CHAT =
      new GroupDto(RC_FEEDBACK_GROUP_ID, RC_FEEDBACK_GROUP_NAME);
  public static final GetGroupInfoDto GET_GROUP_INFO_DTO_FEEDBACK_CHAT =
      new GetGroupInfoDto(GROUP_DTO_FEEDBACK_CHAT, true);

  public static ChatMessage createGroupMessage() {
    return ChatMessage.builder().rcToken(RC_TOKEN).rcUserId(RC_USER_ID)
        .rcGroupId(RC_GROUP_ID).text(MESSAGE).build();
  }

  public static ChatMessage createFeedbackGroupMessage() {
    return ChatMessage.builder().rcToken(RC_TOKEN).rcUserId(RC_USER_ID)
        .rcGroupId(RC_FEEDBACK_GROUP_ID).text(MESSAGE).build();
  }

  public static SendMessageResponseDTO createSuccessfulMessageResult(String type, String roomId) {
    var result = new SendMessageResultDTO();
    result.setId(RandomStringUtils.randomAlphabetic(16));
    result.setTimestamp(new Date());
    result.setUpdatedAt(new Date());
    result.setRid(roomId);
    result.setT(type);

    var successfulResponse = new SendMessageResponseDTO();
    successfulResponse.setMessage(result);
    successfulResponse.setSuccess(true);

    return successfulResponse;
  }
}

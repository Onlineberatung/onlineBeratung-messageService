package de.caritas.cob.MessageService.api.controller;

import static de.caritas.cob.MessageService.testHelper.TestConstants.DONT_SEND_NOTIFICATION;
import static de.caritas.cob.MessageService.testHelper.TestConstants.MESSAGE;
import static de.caritas.cob.MessageService.testHelper.TestConstants.MESSAGE_DTO_WITH_NOTIFICATION;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_DESCRIPTION;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_FILE_TYPE;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_ID;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_IMAGE_PREVIEW;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_IMAGE_SIZE;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_IMAGE_TYPE;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_IMAGE_URL;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_TITLE;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_TITLE_LINK;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_ATTACHMENT_TITLE_LINK_DOWNLOAD;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_COUNT;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_FEEDBACK_GROUP_ID;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_OFFSET;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_TIMESTAMP;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_TOKEN;
import static de.caritas.cob.MessageService.testHelper.TestConstants.RC_USER_ID;
import static de.caritas.cob.MessageService.testHelper.TestConstants.SEND_NOTIFICATION;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.MessageService.api.facade.PostGroupMessageFacade;
import de.caritas.cob.MessageService.api.model.MessageStreamDTO;
import de.caritas.cob.MessageService.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.MessageService.api.model.rocket.chat.message.UserDTO;
import de.caritas.cob.MessageService.api.model.rocket.chat.message.attachment.AttachmentDTO;
import de.caritas.cob.MessageService.api.model.rocket.chat.message.attachment.FileDTO;
import de.caritas.cob.MessageService.api.service.EncryptionService;
import de.caritas.cob.MessageService.api.service.LogService;
import de.caritas.cob.MessageService.api.service.RocketChatService;

@RunWith(SpringRunner.class)
@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(secure = false)
public class MessageControllerTestIT {

  private final String VALID_MESSAGE_REQUEST_BODY_WITHOUT_NOTIFICATION =
      "{\"message\": \"Lorem ipsum\", \"sendNotification\": " + DONT_SEND_NOTIFICATION + "}";
  private final String VALID_MESSAGE_REQUEST_BODY_WITH_NOTIFICATION =
      "{\"message\": \"Lorem ipsum\", \"sendNotification\": " + SEND_NOTIFICATION + "}";
  private final String VALID_FORWARD_MESSAGE_REQUEST_BODY = "{\"message\": \"" + MESSAGE + "\","
      + "\"timestamp\": \"2018-11-15T09:33:00.057Z\", \"username\": \"asker23\",\r\n"
      + "\"rcUserId\": \"ag89h3tjkerg94t\"}";
  private final String INVALID_MESSAGE_REQUEST_BODY = "{\"in\": \"valid\"}";
  private final FileDTO FILE_DTO =
      new FileDTO(RC_ATTACHMENT_ID, RC_ATTACHMENT_TITLE, RC_ATTACHMENT_FILE_TYPE);
  private final AttachmentDTO ATTACHMENT_DTO =
      new AttachmentDTO(RC_ATTACHMENT_TITLE, RC_ATTACHMENT_FILE_TYPE, RC_ATTACHMENT_DESCRIPTION,
          RC_ATTACHMENT_TITLE_LINK, RC_ATTACHMENT_TITLE_LINK_DOWNLOAD, RC_ATTACHMENT_IMAGE_URL,
          RC_ATTACHMENT_IMAGE_TYPE, RC_ATTACHMENT_IMAGE_SIZE, RC_ATTACHMENT_IMAGE_PREVIEW);
  private final MessagesDTO MESSAGES_DTO = new MessagesDTO("123", null, RC_GROUP_ID, MESSAGE,
      RC_TIMESTAMP, new UserDTO(RC_USER_ID, "test", "name"), false, new String[0], new String[0],
      RC_TIMESTAMP, Arrays.array(ATTACHMENT_DTO), FILE_DTO);
  private final String PATH_UPDATE_KEY = "/messages/key?key=";
  private final String PATH_CREATE_MESSAGE = "/messages/new";
  private final String PATH_CREATE_FEEDBACK_MESSAGE = "/messages/feedback/new";
  private final String PATH_GET_MESSAGES = "/messages";
  private final String PATH_POST_FORWARD_MESSAGE = "/messages/forward";
  private final String QUERY_PARAM_OFFSET = "offset";
  private final String QUERY_PARAM_COUNT = "count";
  private final String QUERY_PARAM_RC_USER_ID = "rcUserId";
  private final String QUERY_PARAM_RC_GROUP_ID = "rcGroupId";
  private final String QUERY_PARAM_RC_TOKEN = "rcToken";
  private final String QUERY_PARAM_RC_FEEDBACK_GROUP_ID = "rcFeedbackGroupId";
  private final String MASTER_KEY_1 = "key1";
  private final String MASTER_KEY_2 = "key2";
  private final String MASTER_KEY_DTO_KEY_1 = "{\"masterKey\": \"" + MASTER_KEY_1 + "\"}";
  private final String MASTER_KEY_DTO_KEY_2 = "{\"masterKey\": \"" + MASTER_KEY_2 + "\"}";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private RocketChatService rocketChatService;

  @MockBean
  private EncryptionService encryptionService;

  @MockBean
  private LogService logService;

  @MockBean
  private PostGroupMessageFacade postGroupMessageFacade;

  /**
   * 400 - Bad Request tests
   * 
   */

  @Test
  public void createMessage_Should_ReturnBadRequest_WhenProvidedWithInvalidRequestBody()
      throws Exception {

    mvc.perform(post(PATH_CREATE_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .content(INVALID_MESSAGE_REQUEST_BODY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  public void createMessage_Should_ReturnBadRequest_WhenHeaderValuesAreMissing() throws Exception {

    mvc.perform(post(PATH_CREATE_MESSAGE).content(VALID_MESSAGE_REQUEST_BODY_WITHOUT_NOTIFICATION)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void getMessageStream_Should_ReturnBadRequest_WhenHeaderValuesAreMissing()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGES).param(QUERY_PARAM_OFFSET, RC_OFFSET)
        .param(QUERY_PARAM_COUNT, RC_COUNT).param(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  public void getMessageStream_Should_ReturnBadRequest_WhenRequestParamsAreMissing()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGES).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void forwardMessage_Should_ReturnBadRequest_WhenProvidedWithInvalidRequestBody()
      throws Exception {

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .content(INVALID_MESSAGE_REQUEST_BODY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  public void forwardMessage_Should_ReturnBadRequest_WhenHeaderValuesAreMissing() throws Exception {

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).content(VALID_FORWARD_MESSAGE_REQUEST_BODY)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void createFeedbackMessage_Should_ReturnBadRequest_WhenProvidedWithInvalidRequestBody()
      throws Exception {

    mvc.perform(post(PATH_CREATE_FEEDBACK_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID)
        .header(QUERY_PARAM_RC_FEEDBACK_GROUP_ID, RC_FEEDBACK_GROUP_ID)
        .content(INVALID_MESSAGE_REQUEST_BODY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  public void createFeedbackMessage_Should_ReturnBadRequest_WhenHeaderValuesAreMissing()
      throws Exception {

    mvc.perform(
        post(PATH_CREATE_FEEDBACK_MESSAGE).content(VALID_MESSAGE_REQUEST_BODY_WITHOUT_NOTIFICATION)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  /**
   * 200 - OK & 201 CREATED tests
   * 
   */

  @Test
  public void getMessageStream_Should_ReturnOk_WhenProvidedWithValidRequestValues()
      throws Exception {

    List<MessagesDTO> messages = new ArrayList<>();
    messages.add(MESSAGES_DTO);
    MessageStreamDTO stream =
        new MessageStreamDTO(messages, RC_COUNT, RC_OFFSET, RC_COUNT, "true", "0");
    String streamJson = convertObjectToJson(stream);

    when(rocketChatService.getGroupMessages(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(stream);

    mvc.perform(get(PATH_GET_MESSAGES).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).param(QUERY_PARAM_OFFSET, RC_OFFSET)
        .param(QUERY_PARAM_COUNT, RC_COUNT).param(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
        .andExpect(content().json(streamJson));

    verify(rocketChatService, atLeastOnce()).getGroupMessages(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
  }

  @Test
  public void createMessage_Should_ReturnCreated_WhenProvidedWithValidRequestValuesAndSuccessfulPostGroupMessageFacadeCall()
      throws Exception {

    when(postGroupMessageFacade.postGroupMessage(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString(), Mockito.any())).thenReturn(HttpStatus.CREATED);

    mvc.perform(post(PATH_CREATE_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .content(VALID_MESSAGE_REQUEST_BODY_WITH_NOTIFICATION)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    verify(postGroupMessageFacade, atLeastOnce()).postGroupMessage(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString(), Mockito.any());
  }

  @Test
  public void forwardMessage_Should_ReturnCreated_WhenProvidedWithValidRequestValuesAndSuccessfulPostGroupMessageFacadeCall()
      throws Exception {

    when(postGroupMessageFacade.postFeedbackGroupMessage(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(HttpStatus.CREATED);

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .content(VALID_FORWARD_MESSAGE_REQUEST_BODY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
  }

  @Test
  public void createFeedbackMessage_Should_ReturnCreated_WhenProvidedWithValidRequestValuesAndSuccessfulPostGroupMessageFacadeCall()
      throws Exception {

    when(postGroupMessageFacade.postFeedbackGroupMessage(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID,
        MESSAGE, null)).thenReturn(HttpStatus.CREATED);

    mvc.perform(post(PATH_CREATE_FEEDBACK_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID)
        .header(QUERY_PARAM_RC_FEEDBACK_GROUP_ID, RC_FEEDBACK_GROUP_ID)
        .content(VALID_MESSAGE_REQUEST_BODY_WITHOUT_NOTIFICATION)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    verify(postGroupMessageFacade, atLeastOnce()).postFeedbackGroupMessage(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.isNull());
  }

  /**
   * 204 - No Content test
   * 
   */
  @Test
  public void getMessageStream_Should_ReturnNoContent_WhenProvidedWithValidRequestValuesAndMessageStreamIsEmpty()
      throws Exception {

    when(rocketChatService.getGroupMessages(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);

    mvc.perform(get(PATH_GET_MESSAGES).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).param(QUERY_PARAM_OFFSET, RC_OFFSET)
        .param(QUERY_PARAM_COUNT, RC_COUNT).param(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

    verify(rocketChatService, atLeastOnce()).getGroupMessages(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
  }

  /**
   * 500 - Internal Server Error test
   * 
   */
  @Test
  public void createMessage_Should_ReturnInternalServerError_WhenProvidedWithValidRequestValuesAndPostGroupMessageFacadeResponseIsEmpty()
      throws Exception {

    when(postGroupMessageFacade.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        MESSAGE_DTO_WITH_NOTIFICATION)).thenReturn(null);

    mvc.perform(post(PATH_CREATE_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .content(VALID_MESSAGE_REQUEST_BODY_WITHOUT_NOTIFICATION)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());

    verify(postGroupMessageFacade, atLeastOnce()).postGroupMessage(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString(), Mockito.any());
  }

  @Test
  public void forwardMessage_Should_ReturnInternalServerError_WhenProvidedWithValidRequestValuesAndPostGroupMessageFacadeResponseIsEmpty()
      throws Exception {

    when(postGroupMessageFacade.postGroupMessage(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString(), Mockito.any())).thenReturn(null);

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .content(VALID_FORWARD_MESSAGE_REQUEST_BODY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError());

    verify(postGroupMessageFacade, atLeastOnce()).postFeedbackGroupMessage(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
  }

  @Test
  public void createFeedbackMessage_Should_ReturnInternalServerError_WhenProvidedWithValidRequestValuesAndPostGroupMessageFacadeResponseIsEmpty()
      throws Exception {

    when(postGroupMessageFacade.postFeedbackGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE,
        null)).thenReturn(null);

    mvc.perform(post(PATH_CREATE_FEEDBACK_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID)
        .header(QUERY_PARAM_RC_FEEDBACK_GROUP_ID, RC_FEEDBACK_GROUP_ID)
        .content(VALID_MESSAGE_REQUEST_BODY_WITHOUT_NOTIFICATION)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());

    verify(postGroupMessageFacade, atLeastOnce()).postFeedbackGroupMessage(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.isNull());
  }

  /**
   * 202 - Accepted Test
   * 
   * @throws Exception
   */
  @Test
  public void updateKey_Should_ReturnAccepted_WhenProvidedWithNewKey() throws Exception {

    when(encryptionService.getMasterKey()).thenReturn(MASTER_KEY_1);

    mvc.perform(post(PATH_UPDATE_KEY).contentType(MediaType.APPLICATION_JSON)
        .content(MASTER_KEY_DTO_KEY_2).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  /**
   * 409 - Conflict test
   * 
   * @throws Exception
   */
  @Test
  public void updateKey_Should_ReturnConflict_WhenProvidedWithSameKey() throws Exception {

    when(encryptionService.getMasterKey()).thenReturn(MASTER_KEY_1);

    mvc.perform(post(PATH_UPDATE_KEY).contentType(MediaType.APPLICATION_JSON)
        .content(MASTER_KEY_DTO_KEY_1).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict());
  }

  /**
   * Helper methods
   * 
   */
  private String convertObjectToJson(Object object) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(object);
  }
}

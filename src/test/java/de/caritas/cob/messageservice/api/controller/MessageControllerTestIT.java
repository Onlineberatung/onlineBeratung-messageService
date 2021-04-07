package de.caritas.cob.messageservice.api.controller;

import static de.caritas.cob.messageservice.api.controller.MessageControllerAuthorizationTestIT.PATH_GET_MESSAGE_STREAM;
import static de.caritas.cob.messageservice.api.controller.MessageControllerAuthorizationTestIT.PATH_POST_CREATE_ALIAS_ONLY_MESSAGE;
import static de.caritas.cob.messageservice.api.controller.MessageControllerAuthorizationTestIT.PATH_POST_CREATE_FEEDBACK_MESSAGE;
import static de.caritas.cob.messageservice.api.controller.MessageControllerAuthorizationTestIT.PATH_POST_CREATE_MESSAGE;
import static de.caritas.cob.messageservice.api.controller.MessageControllerAuthorizationTestIT.PATH_POST_CREATE_VIDEO_HINT_MESSAGE;
import static de.caritas.cob.messageservice.api.controller.MessageControllerAuthorizationTestIT.PATH_POST_FORWARD_MESSAGE;
import static de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType.NEW_MESSAGE;
import static de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType.OVERWRITTEN_MESSAGE;
import static de.caritas.cob.messageservice.testhelper.TestConstants.DONT_SEND_NOTIFICATION;
import static de.caritas.cob.messageservice.testhelper.TestConstants.MESSAGE;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_DESCRIPTION;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_FILE_TYPE;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_IMAGE_PREVIEW;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_IMAGE_SIZE;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_IMAGE_TYPE;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_IMAGE_URL;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_TITLE;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_TITLE_LINK;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_ATTACHMENT_TITLE_LINK_DOWNLOAD;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_COUNT;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_FEEDBACK_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_OFFSET;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_TIMESTAMP;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_TOKEN;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_USER_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.SEND_NOTIFICATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.authorization.RoleAuthorizationAuthorityMapper;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.facade.PostGroupMessageFacade;
import de.caritas.cob.messageservice.api.model.AliasOnlyMessageDTO;
import de.caritas.cob.messageservice.api.model.AttachmentDTO;
import de.caritas.cob.messageservice.api.model.FileDTO;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.UserDTO;
import de.caritas.cob.messageservice.api.service.DraftMessageService;
import de.caritas.cob.messageservice.api.service.EncryptionService;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.util.Arrays;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
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
      new FileDTO().id(RC_ATTACHMENT_ID).name(RC_ATTACHMENT_TITLE).type(RC_ATTACHMENT_FILE_TYPE);
  private final AttachmentDTO ATTACHMENT_DTO =
      new AttachmentDTO().title(RC_ATTACHMENT_TITLE).type(RC_ATTACHMENT_FILE_TYPE)
          .description(RC_ATTACHMENT_DESCRIPTION).titleLink(RC_ATTACHMENT_TITLE_LINK)
          .titleLinkDownload(RC_ATTACHMENT_TITLE_LINK_DOWNLOAD).imageUrl(RC_ATTACHMENT_IMAGE_URL)
          .imageType(RC_ATTACHMENT_IMAGE_TYPE).imageSize(RC_ATTACHMENT_IMAGE_SIZE)
          .imagePreview(RC_ATTACHMENT_IMAGE_PREVIEW);
  private final MessagesDTO MESSAGES_DTO = new MessagesDTO("123", null, RC_GROUP_ID, MESSAGE,
      RC_TIMESTAMP, new UserDTO(RC_USER_ID, "test", "name"), false, new String[0], new String[0],
      RC_TIMESTAMP, Arrays.array(ATTACHMENT_DTO), FILE_DTO);
  private final String PATH_UPDATE_KEY = "/messages/key?key=";
  private final String PATH_DRAFT_MESSAGE = "/messages/draft";
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
  private PostGroupMessageFacade postGroupMessageFacade;

  @MockBean
  private DraftMessageService draftMessageService;

  @MockBean
  private RoleAuthorizationAuthorityMapper roleAuthorizationAuthorityMapper;

  @Mock
  private Logger logger;

  @Before
  public void setup() {
    setInternalState(LogService.class, "LOGGER", logger);
  }

  /**
   * 400 - Bad Request tests
   */

  @Test
  public void createMessage_Should_ReturnBadRequest_WhenProvidedWithInvalidRequestBody()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .content(INVALID_MESSAGE_REQUEST_BODY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  public void createMessage_Should_ReturnBadRequest_WhenHeaderValuesAreMissing() throws Exception {

    mvc.perform(
        post(PATH_POST_CREATE_MESSAGE).content(VALID_MESSAGE_REQUEST_BODY_WITHOUT_NOTIFICATION)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void getMessageStream_Should_ReturnBadRequest_WhenHeaderValuesAreMissing()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).param(QUERY_PARAM_OFFSET, RC_OFFSET)
        .param(QUERY_PARAM_COUNT, RC_COUNT).param(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  public void getMessageStream_Should_ReturnBadRequest_WhenRequestParamsAreMissing()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
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

    mvc.perform(post(PATH_POST_CREATE_FEEDBACK_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID)
        .header(QUERY_PARAM_RC_FEEDBACK_GROUP_ID, RC_FEEDBACK_GROUP_ID)
        .content(INVALID_MESSAGE_REQUEST_BODY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  public void createFeedbackMessage_Should_ReturnBadRequest_WhenHeaderValuesAreMissing()
      throws Exception {

    mvc.perform(
        post(PATH_POST_CREATE_FEEDBACK_MESSAGE)
            .content(VALID_MESSAGE_REQUEST_BODY_WITHOUT_NOTIFICATION)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  /**
   * 200 - OK & 201 CREATED tests
   */

  @Test
  public void getMessageStream_Should_ReturnOk_WhenProvidedWithValidRequestValues()
      throws Exception {

    List<MessagesDTO> messages = new ArrayList<>();
    messages.add(MESSAGES_DTO);
    MessageStreamDTO stream = new MessageStreamDTO().messages(messages);
    String streamJson = convertObjectToJson(stream);

    when(rocketChatService.getGroupMessages(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString())).thenReturn(stream);

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).param(QUERY_PARAM_OFFSET, RC_OFFSET)
        .param(QUERY_PARAM_COUNT, RC_COUNT).param(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
        .andExpect(content().json(streamJson));

    verify(rocketChatService, atLeastOnce()).getGroupMessages(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString());
  }

  @Test
  public void createMessage_Should_ReturnCreated_WhenProvidedWithValidRequestValuesAndSuccessfulPostGroupMessageFacadeCall()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
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

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .content(VALID_FORWARD_MESSAGE_REQUEST_BODY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
  }

  @Test
  public void createFeedbackMessage_Should_ReturnCreated_WhenProvidedWithValidRequestValuesAndSuccessfulPostGroupMessageFacadeCall()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_FEEDBACK_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
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
   */
  @Test
  public void getMessageStream_Should_ReturnNoContent_WhenProvidedWithValidRequestValuesAndMessageStreamIsEmpty()
      throws Exception {

    when(rocketChatService.getGroupMessages(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString())).thenReturn(null);

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).param(QUERY_PARAM_OFFSET, RC_OFFSET)
        .param(QUERY_PARAM_COUNT, RC_COUNT).param(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

    verify(rocketChatService, atLeastOnce()).getGroupMessages(Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString());
  }

  /**
   * 500 - Internal Server Error test
   */
  @Test
  public void createMessage_Should_ReturnInternalServerError_WhenProvidedWithValidRequestValuesAndPostGroupMessageFacadeResponseIsEmpty()
      throws Exception {

    doThrow(new InternalServerErrorException())
        .when(postGroupMessageFacade).postGroupMessage(eq(RC_TOKEN), eq(RC_USER_ID),
        eq(RC_GROUP_ID), any());

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
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

    doThrow(new InternalServerErrorException())
        .when(postGroupMessageFacade).postFeedbackGroupMessage(any(), any(), any(), any(), any());

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

    doThrow(new InternalServerErrorException()).when(postGroupMessageFacade)
        .postFeedbackGroupMessage(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    mvc.perform(post(PATH_POST_CREATE_FEEDBACK_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
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
   */
  private String convertObjectToJson(Object object) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(object);
  }

  @Test
  public void createMessage_Should_LogInternalServerError_When_InternalServerErrorIsThrown()
      throws Exception {

    doThrow(new InternalServerErrorException())
        .when(postGroupMessageFacade).postGroupMessage(any(), any(), any(), any());

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).header(QUERY_PARAM_RC_TOKEN, RC_TOKEN)
        .header(QUERY_PARAM_RC_USER_ID, RC_USER_ID).header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .content(VALID_MESSAGE_REQUEST_BODY_WITHOUT_NOTIFICATION)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());

    verify(logger, atLeastOnce()).error(eq("{}{}"), eq("Internal Server Error: "), anyString());
  }

  @Test
  public void saveDraftMessage_Should_returnBadRequest_When_rcGroupIdAndMessageIsMissing()
      throws Exception {
    mvc.perform(post(PATH_DRAFT_MESSAGE).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void saveDraftMessage_Should_returnBadRequest_When_rcGroupIdIsMissing() throws Exception {
    mvc.perform(post(PATH_DRAFT_MESSAGE)
        .content("message")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void saveDraftMessage_Should_returnBadRequest_When_messageIsMissing() throws Exception {
    mvc.perform(post(PATH_DRAFT_MESSAGE)
        .header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void saveDraftMessage_Should_returnCreated_When_messageIsNew() throws Exception {
    when(this.draftMessageService.saveDraftMessage(any(), any())).thenReturn(NEW_MESSAGE);

    mvc.perform(post(PATH_DRAFT_MESSAGE)
        .content("message")
        .header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());
  }

  @Test
  public void saveDraftMessage_Should_returnOk_When_messageIsOverwritten() throws Exception {
    when(this.draftMessageService.saveDraftMessage(any(), any())).thenReturn(OVERWRITTEN_MESSAGE);

    mvc.perform(post(PATH_DRAFT_MESSAGE)
        .content("message")
        .header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  public void findDraftMessage_Should_returnBadRequest_When_rcGroupIdIsMissing() throws Exception {
    mvc.perform(get(PATH_DRAFT_MESSAGE).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void findDraftMessage_Should_returnDraftMessage_When_messageExists() throws Exception {
    when(this.draftMessageService.findAndDecryptDraftMessage(any())).thenReturn("message");

    String message = mvc.perform(get(PATH_DRAFT_MESSAGE)
        .header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    assertThat(message, is("message"));
  }

  @Test
  public void findDraftMessage_Should_returnNoContent_When_noDraftMessageExists() throws Exception {
    mvc.perform(get(PATH_DRAFT_MESSAGE)
        .header(QUERY_PARAM_RC_GROUP_ID, RC_GROUP_ID)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  public void createVideoHintMessage_Should_ReturnBadRequest_When_rcGroupIdIsMissing()
      throws Exception {

    VideoCallMessageDTO videoCallMessageDTO =
        new EasyRandom().nextObject(VideoCallMessageDTO.class);

    mvc.perform(
        post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(videoCallMessageDTO))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(this.postGroupMessageFacade);
  }

  @Test
  public void createVideoHintMessage_Should_ReturnBadRequest_When_videoCallMessageDTOIsMissing()
      throws Exception {

    mvc.perform(
        post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
            .header("RCGroupId", RC_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(this.postGroupMessageFacade);
  }

  @Test
  public void createVideoHintMessage_Should_ReturnBadRequest_When_videoCallMessageDTOIsEmpty()
      throws Exception {

    mvc.perform(
        post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
            .header("RCGroupId", RC_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(new VideoCallMessageDTO()))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(this.postGroupMessageFacade);
  }

  @Test
  public void createVideoHintMessage_Should_ReturnCreated_When_paramsAreValid()
      throws Exception {

    VideoCallMessageDTO videoCallMessageDTO =
        new EasyRandom().nextObject(VideoCallMessageDTO.class);

    mvc.perform(
        post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
            .header("RCGroupId", RC_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(videoCallMessageDTO))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    verify(this.postGroupMessageFacade, times(1)).createVideoHintMessage(any(), any());
  }

  @Test
  public void saveAliasOnlyMessage_Should_ReturnBadRequest_When_rcGroupIdIsMissing()
      throws Exception {
    AliasOnlyMessageDTO aliasOnlyMessageDTO =
        new EasyRandom().nextObject(AliasOnlyMessageDTO.class);

    mvc.perform(
        post(PATH_POST_CREATE_ALIAS_ONLY_MESSAGE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(aliasOnlyMessageDTO))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(this.postGroupMessageFacade);
  }

  @Test
  public void saveAliasOnlyMessage_Should_ReturnBadRequest_When_AliasOnlyMessageDtoIsMissing()
      throws Exception {
    mvc.perform(
        post(PATH_POST_CREATE_ALIAS_ONLY_MESSAGE)
            .header("rcGroupId", RC_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(this.postGroupMessageFacade);
  }

  @Test
  public void saveAliasOnlyMessage_Should_ReturnCreated_When_paramsAreValid()
      throws Exception {
    AliasOnlyMessageDTO aliasOnlyMessageDTO =
        new EasyRandom().nextObject(AliasOnlyMessageDTO.class);
    aliasOnlyMessageDTO.setMessageType(MessageType.FORWARD);

    mvc.perform(
        post(PATH_POST_CREATE_ALIAS_ONLY_MESSAGE)
            .header("rcGroupId", RC_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(aliasOnlyMessageDTO))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    verify(this.postGroupMessageFacade, times(1))
        .postAliasOnlyMessage(RC_GROUP_ID, MessageType.FORWARD);
  }
}

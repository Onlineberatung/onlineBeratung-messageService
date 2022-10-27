package de.caritas.cob.messageservice.api.controller;

import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_TOKEN;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.Messenger;
import de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue;
import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.AliasOnlyMessageDTO;
import de.caritas.cob.messageservice.api.model.MessageDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.ReassignStatus;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.service.EncryptionService;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import javax.servlet.http.Cookie;
import org.apache.commons.lang3.RandomStringUtils;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@ActiveProfiles("testing")
@SpringBootTest
@AutoConfigureMockMvc
public class MessageControllerAuthorizationTestIT {

  protected final static String PATH_GET_MESSAGE_STREAM = "/messages";
  protected final static String PATH_POST_CREATE_MESSAGE = "/messages/new";
  protected final static String PATH_POST_CREATE_FEEDBACK_MESSAGE = "/messages/feedback/new";
  protected final static String PATH_POST_CREATE_VIDEO_HINT_MESSAGE = "/messages/videohint/new";
  protected final static String PATH_POST_CREATE_ALIAS_ONLY_MESSAGE = "/messages/aliasonly/new";
  protected final static String PATH_POST_UPDATE_KEY = "/messages/key";
  protected final static String PATH_POST_FORWARD_MESSAGE = "/messages/forward";
  private final static String CSRF_COOKIE = "CSRF-TOKEN";
  private final static String CSRF_HEADER = "X-CSRF-TOKEN";
  private final static String CSRF_VALUE = "test";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final EasyRandom easyRandom = new EasyRandom();

  @Autowired
  private MockMvc mvc;

  @MockBean
  private RocketChatService rocketChatService;

  @MockBean
  private EncryptionService encryptionService;

  @MockBean
  private Messenger messenger;

  private Cookie csrfCookie;
  private String messageId;
  private AliasArgs aliasArgs;

  @Before
  public void setUp() {
    csrfCookie = new Cookie(CSRF_COOKIE, CSRF_VALUE);
    messageId = null;
    aliasArgs = null;
  }

  @Test
  public void findMessages_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(rocketChatService);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.TECHNICAL_DEFAULT})
  public void findMessages_Should_ReturnForbiddenAndCallNoMethods_WhenNoUserOrConsultantDefaultAuthority()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.CONSULTANT_DEFAULT, AuthorityValue.USER_DEFAULT})
  public void findMessages_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
  }

  @Test
  public void createMessage_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser
  public void createMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoUserOrConsultantOrTechnicalDefaultAuthority()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.CONSULTANT_DEFAULT, AuthorityValue.USER_DEFAULT,
      AuthorityValue.TECHNICAL_DEFAULT})
  public void createMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(messenger);
  }

  @Test
  public void updateKey_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(post(PATH_POST_UPDATE_KEY).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(encryptionService);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.CONSULTANT_DEFAULT, AuthorityValue.USER_DEFAULT})
  public void updateKey_Should_ReturnForbiddenAndCallNoMethods_WhenNoTechnicalDefaultAuthority()
      throws Exception {

    mvc.perform(post(PATH_POST_UPDATE_KEY).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(encryptionService);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.TECHNICAL_DEFAULT})
  public void updateKey_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens() throws Exception {

    mvc.perform(post(PATH_POST_UPDATE_KEY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(encryptionService);
  }

  @Test
  public void forwardMessage_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser
  public void forwardMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoUserFeedbackAuthority()
      throws Exception {

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USE_FEEDBACK})
  public void forwardMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens()
      throws Exception {

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(messenger);
  }

  @Test
  public void createFeedbackMessage_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(
            post(PATH_POST_CREATE_FEEDBACK_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser
  public void createFeedbackMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoUserFeedbackAuthority()
      throws Exception {

    mvc.perform(
            post(PATH_POST_CREATE_FEEDBACK_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USE_FEEDBACK})
  public void createFeedbackMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_FEEDBACK_MESSAGE).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(messenger);
  }

  @Test
  public void createVideoHintMessage_Should_ReturnUnauthorizedAndCallNoMethods_When_NoKeycloakAuthorization()
      throws Exception {

    mvc.perform(
            post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("RCGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser
  public void createVideoHintMessage_Should_ReturnForbiddenAndCallNoMethods_When_NoUserOrConsultantAuthority()
      throws Exception {

    mvc.perform(
            post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("RCGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void createVideoHintMessage_Should_ReturnForbiddenAndCallNoMethods_When_NoCsrfTokens()
      throws Exception {

    mvc.perform(
            post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
                .header("RCGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void createVideoHintMessage_Should_ReturnCreatedAndCallPostGroupMessageFacade_When_UserAuthority()
      throws Exception {

    var videoCallMessageDTO = easyRandom.nextObject(VideoCallMessageDTO.class);

    mvc.perform(
            post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("RCGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(videoCallMessageDTO))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    verify(messenger, times(1)).createVideoHintMessage(any(), any());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.CONSULTANT_DEFAULT})
  public void createVideoHintMessage_Should_ReturnCreatedAndCallPostGroupMessageFacade_When_ConsultantAuthority()
      throws Exception {

    var videoCallMessageDTO = easyRandom.nextObject(VideoCallMessageDTO.class);

    mvc.perform(
            post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("RCGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(videoCallMessageDTO))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    verify(messenger, times(1)).createVideoHintMessage(any(), any());
  }

  @Test
  public void saveAliasOnlyMessage_Should_ReturnUnauthorizedAndCallNoMethods_When_NoKeycloakAuthorization()
      throws Exception {
    var aliasOnlyMessageDTO = createAliasOnlyMessageWithoutProtectedType();

    mvc.perform(
            post(PATH_POST_CREATE_ALIAS_ONLY_MESSAGE)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessageDTO))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser
  public void saveAliasOnlyMessage_Should_ReturnForbiddenAndCallNoMethods_When_NoUserDefaultAuthority()
      throws Exception {
    var aliasOnlyMessageDTO = createAliasOnlyMessageWithoutProtectedType();

    mvc.perform(
            post(PATH_POST_CREATE_ALIAS_ONLY_MESSAGE)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessageDTO))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void saveAliasOnlyMessage_Should_ReturnForbiddenAndCallNoMethods_When_NoCsrfTokens()
      throws Exception {
    var aliasOnlyMessageDTO = createAliasOnlyMessageWithoutProtectedType();

    mvc.perform(
            post(PATH_POST_CREATE_ALIAS_ONLY_MESSAGE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessageDTO))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(messenger);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void saveAliasOnlyMessage_Should_ReturnCreatedAndCallPostGroupMessageFacade_When_UserDefaultAuthority()
      throws Exception {
    var aliasOnlyMessageDTO = createAliasOnlyMessageWithoutProtectedType();

    mvc.perform(
            post(PATH_POST_CREATE_ALIAS_ONLY_MESSAGE)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessageDTO))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    verify(messenger).createEvent(any(), any(), any());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.CONSULTANT_DEFAULT)
  public void saveAliasOnlyMessageShouldReturnCreatedWhenConsultantDefaultAuthority()
      throws Exception {
    var aliasOnlyMessageDTO = createAliasOnlyMessageWithoutProtectedType();

    mvc.perform(
            post("/messages/aliasonly/new")
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessageDTO))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.ANONYMOUS_DEFAULT})
  public void createVideoHintMessage_Should_ReturnCreatedAndCallPostGroupMessageFacade_When_AnonyousAuthority()
      throws Exception {

    var videoCallMessageDTO = easyRandom.nextObject(VideoCallMessageDTO.class);

    mvc.perform(
            post(PATH_POST_CREATE_VIDEO_HINT_MESSAGE)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("RCGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(videoCallMessageDTO))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    verify(messenger).createVideoHintMessage(any(), any());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.ANONYMOUS_DEFAULT})
  public void sendNewMessage_Should_ReturnCreated_When_AnonyousAuthority()
      throws Exception {

    var messageDTO = easyRandom.nextObject(MessageDTO.class);

    mvc.perform(post(PATH_POST_CREATE_MESSAGE)
            .cookie(csrfCookie)
            .header(CSRF_HEADER, CSRF_VALUE)
            .header("rcToken", RC_TOKEN)
            .header("rcUserId", RC_USER_ID)
            .header("rcGroupId", RC_GROUP_ID)
            .content(objectMapper.writeValueAsString(messageDTO))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.ANONYMOUS_DEFAULT})
  public void findMessages_Should_ReturnNoContent_When_AnonymousAuthority()
      throws Exception {
    mvc.perform(get(PATH_GET_MESSAGE_STREAM)
            .cookie(csrfCookie)
            .header(CSRF_HEADER, CSRF_VALUE)
            .header("rcToken", RC_TOKEN)
            .header("rcUserId", RC_USER_ID)
            .queryParam("rcGroupId", RC_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.TECHNICAL_DEFAULT})
  public void saveAliasOnlyMessage_Should_ReturnCreatedAndCallPostGroupMessageFacade_When_TechnicalDefaultAuthority()
      throws Exception {
    var aliasOnlyMessageDTO = givenAValidAliasOnlyMessageDTO();

    mvc.perform(
            post(PATH_POST_CREATE_ALIAS_ONLY_MESSAGE)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessageDTO))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    verify(messenger).createEvent(any(), any(), any());
  }

  @Test
  public void patchMessageShouldReturnUnauthorizedWhenNoKeycloakAuthorization() throws Exception {
    givenAPatchSupportedReassignArg();
    givenAValidMessageId();

    mvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(authorities = {
      AuthorityValue.ANONYMOUS_DEFAULT,
      AuthorityValue.CONSULTANT_DEFAULT,
      AuthorityValue.TECHNICAL_DEFAULT,
      AuthorityValue.USE_FEEDBACK
  })
  public void patchMessageShouldReturnForbiddenAndCallNoMethodsWhenNoUserDefaultAuthority()
      throws Exception {
    givenAPatchSupportedReassignArg();
    givenAValidMessageId();

    mvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  public void patchMessageShouldReturnForbiddenAndCallNoMethodsWhenNoCsrfToken() throws Exception {
    givenAPatchSupportedReassignArg();
    givenAValidMessageId();

    mvc.perform(
            patch("/messages/{messageId}", messageId)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs)))
        .andExpect(status().isForbidden());
  }

  @Test
  public void deleteMessageShouldReturnUnauthorizedWhenNoKeycloakAuthorization() throws Exception {
    givenAValidMessageId();

    mvc.perform(
        delete("/messages/{messageId}", messageId)
            .cookie(csrfCookie)
            .header(CSRF_HEADER, CSRF_VALUE)
            .header("rcToken", RandomStringUtils.randomAlphabetic(16))
            .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
    ).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(authorities = {
      AuthorityValue.TECHNICAL_DEFAULT,
      AuthorityValue.USE_FEEDBACK
  })
  public void deleteMessageShouldReturnForbiddenAndCallNoMethodsWhenNoUserDefaultAuthority()
      throws Exception {
    givenAValidMessageId();

    mvc.perform(
        delete("/messages/{messageId}", messageId)
            .cookie(csrfCookie)
            .header(CSRF_HEADER, CSRF_VALUE)
            .header("rcToken", RandomStringUtils.randomAlphabetic(16))
            .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
    ).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  public void deleteMessageShouldReturnForbiddenAndCallNoMethodsWhenNoCsrfToken() throws Exception {
    givenAValidMessageId();

    mvc.perform(
        delete("/messages/{messageId}", messageId)
            .header(CSRF_HEADER, CSRF_VALUE)
            .header("rcToken", RandomStringUtils.randomAlphabetic(16))
            .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
    ).andExpect(status().isForbidden());
  }

  @Test
  public void findMessageShouldReturnUnauthorizedWhenNoKeycloakAuthorization() throws Exception {
    givenAValidMessageId();

    mvc.perform(
            get("/messages/{messageId}", messageId)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(authorities = {
      AuthorityValue.TECHNICAL_DEFAULT,
      AuthorityValue.USE_FEEDBACK
  })
  public void findMessageShouldReturnForbiddenAndCallNoMethodsWhenNoSupportedAuthority()
      throws Exception {
    givenAValidMessageId();

    mvc.perform(
            get("/messages/{messageId}", messageId)
                .cookie(csrfCookie)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  public void findMessageShouldReturnForbiddenAndCallNoMethodsWhenNoCsrfToken() throws Exception {
    givenAValidMessageId();

    mvc.perform(
            get("/messages/{messageId}", messageId)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16)))
        .andExpect(status().isForbidden());
  }

  private AliasOnlyMessageDTO givenAValidAliasOnlyMessageDTO() {
    var alias = easyRandom.nextObject(AliasOnlyMessageDTO.class);
    alias.setArgs(null);
    while (alias.getMessageType().equals(MessageType.USER_MUTED)
        || alias.getMessageType().equals(MessageType.USER_UNMUTED)) {
      alias.setMessageType(easyRandom.nextObject(MessageType.class));
    }

    return alias;
  }

  AliasOnlyMessageDTO createAliasOnlyMessageWithoutProtectedType() {
    AliasOnlyMessageDTO aliasOnlyMessageDTO;
    do {
      aliasOnlyMessageDTO = easyRandom.nextObject(AliasOnlyMessageDTO.class);
    } while (isProtectedMessageType(aliasOnlyMessageDTO.getMessageType()));

    aliasOnlyMessageDTO.setArgs(null);

    return aliasOnlyMessageDTO;
  }

  private boolean isProtectedMessageType(MessageType messageType) {
    return messageType == MessageType.USER_MUTED
        || messageType == MessageType.USER_UNMUTED
        || messageType == MessageType.REASSIGN_CONSULTANT;
  }

  private void givenAValidMessageId() {
    messageId = RandomStringUtils.randomAlphanumeric(17);
  }

  private void givenAPatchSupportedReassignArg() {
    aliasArgs = new AliasArgs();
    var status = easyRandom.nextBoolean() ? ReassignStatus.REJECTED : ReassignStatus.CONFIRMED;
    aliasArgs.setStatus(status);
  }
}

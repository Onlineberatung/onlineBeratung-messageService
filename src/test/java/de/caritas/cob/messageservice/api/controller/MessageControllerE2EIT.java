package de.caritas.cob.messageservice.api.controller;

import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_USER_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.createSuccessfulMessageResult;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.RocketChatUserNotInitializedException;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.AliasOnlyMessageDTO;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.api.model.ForwardMessageDTO;
import de.caritas.cob.messageservice.api.model.MessageDTO;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.ReassignStatus;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO.EventTypeEnum;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GroupDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageWrapper;
import de.caritas.cob.messageservice.api.service.EncryptionService;
import de.caritas.cob.messageservice.api.service.LiveEventNotificationService;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import de.caritas.cob.messageservice.api.service.helper.RocketChatCredentialsHelper;
import de.caritas.cob.messageservice.api.service.statistics.StatisticsService;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import org.apache.commons.lang3.RandomStringUtils;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testing")
@AutoConfigureTestDatabase
public class MessageControllerE2EIT {

  private static final EasyRandom easyRandom = new EasyRandom();
  private static final String CSRF_HEADER = "X-CSRF-TOKEN";
  private static final String CSRF_VALUE = "test";
  private static final Cookie CSRF_COOKIE = new Cookie("CSRF-TOKEN", CSRF_VALUE);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EncryptionService encryptionService;

  @Autowired
  private RocketChatService rocketChatService;

  @MockBean
  private RestTemplate restTemplate;

  @MockBean
  @SuppressWarnings("unused")
  private AuthenticatedUser authenticatedUser;

  @MockBean
  private RocketChatCredentialsHelper chatCredentialsHelper;

  @MockBean
  @SuppressWarnings("unused")
  private LiveEventNotificationService liveEventNotificationService;

  @MockBean
  @SuppressWarnings("unused")
  private StatisticsService statisticsService;

  @Captor
  private ArgumentCaptor<HttpEntity<SendMessageWrapper>> sendMessagePayloadCaptor;

  private AliasOnlyMessageDTO aliasOnlyMessage;
  private List<MessagesDTO> messages;
  private ConsultantReassignment consultantReassignment;

  @AfterEach
  void reset() {
    aliasOnlyMessage = null;
    encryptionService.updateMasterKey("initialMasterKey");
    messages = null;
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void getMessagesShouldRespondWithMutedUnmutedAlias() throws Exception {
    givenSomeMessagesWithMutedUnmutedType();

    mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("messages", hasSize(5)))
        .andExpect(jsonPath("messages[0].alias.messageType", is(not("USER_MUTED"))))
        .andExpect(jsonPath("messages[0].alias.messageType", is(not("USER_UNMUTED"))))
        .andExpect(jsonPath("messages[1].alias.messageType", is("USER_MUTED")))
        .andExpect(jsonPath("messages[2].alias.messageType", is(not("USER_MUTED"))))
        .andExpect(jsonPath("messages[2].alias.messageType", is(not("USER_UNMUTED"))))
        .andExpect(jsonPath("messages[3].alias.messageType", is("USER_UNMUTED")))
        .andExpect(jsonPath("messages[4].alias.messageType", is(not("USER_MUTED"))))
        .andExpect(jsonPath("messages[4].alias.messageType", is(not("USER_UNMUTED"))));
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  public void getMessagesShouldRespondWithAliasArgsConsultantReassign() throws Exception {
    givenAMasterKey();
    var groupId = RandomStringUtils.randomAlphabetic(16);
    givenAMessageWithAnEncryptedConsultantReassignment(groupId);

    var response = mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", groupId)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("messages", hasSize(1)))
        .andExpect(jsonPath("messages[0].alias.messageType", is("REASSIGN_CONSULTANT")))
        .andExpect(jsonPath("messages[0].rid", is(groupId)))
        .andExpect(jsonPath("messages[0].msg").isNotEmpty())
        .andReturn().getResponse().getContentAsString();

    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    var messagesResponse = objectMapper.readValue(response, MessageStreamDTO.class);
    var message = messagesResponse.getMessages().get(1).getMsg(); // 1 due to split before
    var consultantReassignment = objectMapper.readValue(message, ConsultantReassignment.class);

    assertEquals(this.consultantReassignment, consultantReassignment);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void getMessagesShouldRespondWithEmptyAlias() throws Exception {
    givenMessagesWithoutClearAlias();

    mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("messages", hasSize(5)))
        .andExpect(jsonPath("messages[0].alias").isEmpty())
        .andExpect(jsonPath("messages[1].alias").isEmpty())
        .andExpect(jsonPath("messages[2].alias").isEmpty())
        .andExpect(jsonPath("messages[3].alias").isEmpty())
        .andExpect(jsonPath("messages[4].alias").isEmpty());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void getMessagesShouldContainOrgMessage() throws Exception {
    givenMessages();

    mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("messages", hasSize(5)))
        .andExpect(jsonPath("messages[0].org").isNotEmpty())
        .andExpect(jsonPath("messages[0].msg").isNotEmpty())
        .andExpect(jsonPath("messages[1].org").isNotEmpty())
        .andExpect(jsonPath("messages[1].msg").isNotEmpty())
        .andExpect(jsonPath("messages[2].org").isNotEmpty())
        .andExpect(jsonPath("messages[2].msg").isNotEmpty())
        .andExpect(jsonPath("messages[3].org").isNotEmpty())
        .andExpect(jsonPath("messages[3].msg").isNotEmpty())
        .andExpect(jsonPath("messages[4].org").isNotEmpty())
        .andExpect(jsonPath("messages[4].msg").isNotEmpty());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void sendMessageShouldTransmitTypeOfMessage() throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    var rcGroupId = RandomStringUtils.randomAlphabetic(16);
    givenSuccessfulSendMessageResponse("e2e", rcGroupId);
    givenAMasterKey();
    MessageDTO encryptedMessage = createMessage("enc.secret_message", "e2e");

    mockMvc.perform(
            post("/messages/new")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .header("rcGroupId", rcGroupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(encryptedMessage))
        )
        .andExpect(status().isCreated());

    var body = sendMessagePayloadCaptor.getValue().getBody();
    assertThat(body).isNotNull();
    var sendMessageRequest = body.getMessage();
    assertThat(sendMessageRequest.getRid()).isEqualTo(rcGroupId);
    assertThat(sendMessageRequest.getAlias()).isNull();
    assertThat(sendMessageRequest.getT()).isEqualTo("e2e");
    assertThat(sendMessageRequest.getMsg()).startsWith("enc:");
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  @DirtiesContext
  public void sendMessageShouldTransmitOrgMessage() throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    var rcGroupId = RandomStringUtils.randomAlphabetic(16);
    givenSuccessfulSendMessageResponse("e2e", rcGroupId);
    var encMessageWithOrg = createMessage("enc.secret_message", "e2e")
        .org("plain text message");
    givenEncryptionCapturing(encMessageWithOrg.getMessage(), encMessageWithOrg.getOrg());

    mockMvc.perform(
            post("/messages/new")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .header("rcGroupId", rcGroupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(encMessageWithOrg))
        )
        .andExpect(status().isCreated());

    var body = sendMessagePayloadCaptor.getValue().getBody();
    assertThat(body).isNotNull();
    var sendMessageRequest = body.getMessage();
    assertThat(sendMessageRequest.getRid()).isEqualTo(rcGroupId);
    assertThat(sendMessageRequest.getAlias()).isNull();
    assertThat(sendMessageRequest.getT()).isEqualTo("e2e");
    assertThat(sendMessageRequest.getMsg()).isEqualTo("encCameIn");
    assertThat(sendMessageRequest.getOrg()).isEqualTo("plainCameIn");
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void sendMessageShouldReturnSendMessageResultOnSuccessfulRequest() throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    var rcGroupId = RandomStringUtils.randomAlphabetic(16);
    givenSuccessfulSendMessageResponse("e2e", rcGroupId);
    givenAMasterKey();

    MessageDTO encryptedMessage = createMessage("enc.secret_message", "e2e");

    mockMvc.perform(
            post("/messages/new")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .header("rcGroupId", rcGroupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(encryptedMessage))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("ts").isNotEmpty())
        .andExpect(jsonPath("_updatedAt").isNotEmpty())
        .andExpect(jsonPath("t", is("e2e")))
        .andExpect(jsonPath("rid", is(rcGroupId)))
        .andExpect(jsonPath("_id").isNotEmpty());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.CONSULTANT_DEFAULT})
  public void createVideoHintMessageShouldReturnSendMessageResultOnSuccessfulRequest()
      throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    var rcGroupId = RandomStringUtils.randomAlphabetic(16);
    givenSuccessfulSendMessageResponse(null, rcGroupId);
    VideoCallMessageDTO vcm = createVideoCallMessage();
    givenAMasterKey();

    mockMvc.perform(
            post("/messages/videohint/new")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", rcGroupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vcm))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("ts").isNotEmpty())
        .andExpect(jsonPath("_updatedAt").isNotEmpty())
        .andExpect(jsonPath("rid", is(rcGroupId)))
        .andExpect(jsonPath("t", is(nullValue())))
        .andExpect(jsonPath("_id").isNotEmpty());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USE_FEEDBACK})
  public void createFeedbackMessageShouldReturnSendMessageResultOnSuccessfulRequest()
      throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    givenAFeedbackGroupResponse();
    var rcFeedbackGroupId = RandomStringUtils.randomAlphabetic(16);
    givenSuccessfulSendMessageResponse(null, rcFeedbackGroupId);
    MessageDTO feedbackMessage = createMessage("a feedback message", null);
    givenAMasterKey();

    mockMvc.perform(
            post("/messages/feedback/new")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .header("rcFeedbackGroupId", rcFeedbackGroupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(feedbackMessage))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("ts").isNotEmpty())
        .andExpect(jsonPath("_updatedAt").isNotEmpty())
        .andExpect(jsonPath("rid", is(rcFeedbackGroupId)))
        .andExpect(jsonPath("t", is(nullValue())))
        .andExpect(jsonPath("_id").isNotEmpty());
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USE_FEEDBACK})
  public void forwardMessageShouldReturnSendMessageResultOnSuccessfulRequest()
      throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    givenAFeedbackGroupResponse();
    givenSuccessfulSendMessageResponse("e2e", RC_GROUP_ID);
    givenAMasterKey();
    ForwardMessageDTO forwardMessage = createForwardMessage();

    mockMvc.perform(
            post("/messages/forward")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forwardMessage))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("ts").isNotEmpty())
        .andExpect(jsonPath("_updatedAt").isNotEmpty())
        .andExpect(jsonPath("rid", is(RC_GROUP_ID)))
        .andExpect(jsonPath("t", is("e2e")))
        .andExpect(jsonPath("_id").isNotEmpty());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.CONSULTANT_DEFAULT)
  void saveAliasOnlyMessageShouldReturnSendMessageResultWhenNoMessage() throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    givenAnAliasOnlyMessage(false);
    givenSuccessfulSendMessageResponse(null, RC_GROUP_ID);
    givenAMasterKey();

    mockMvc.perform(
            post("/messages/aliasonly/new")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessage))
                .accept(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("ts").isNotEmpty())
        .andExpect(jsonPath("_updatedAt").isNotEmpty())
        .andExpect(jsonPath("rid", is(RC_GROUP_ID)))
        .andExpect(jsonPath("t", is(nullValue())))
        .andExpect(jsonPath("e2e", is(nullValue())))
        .andExpect(jsonPath("_id").isNotEmpty());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.CONSULTANT_DEFAULT)
  void saveAliasOnlyMessageShouldReturnSendMessageResultWhenWithMessage() throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    givenAnAliasOnlyMessageWithSupportedMessage();
    givenSuccessfulSendMessageResponse(null, RC_GROUP_ID);
    givenAMasterKey();

    mockMvc.perform(
            post("/messages/aliasonly/new")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessage))
                .accept(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("ts").isNotEmpty())
        .andExpect(jsonPath("_updatedAt").isNotEmpty())
        .andExpect(jsonPath("rid", is(RC_GROUP_ID)))
        .andExpect(jsonPath("t", is(nullValue())))
        .andExpect(jsonPath("e2e", is(nullValue())))
        .andExpect(jsonPath("_id").isNotEmpty());

    var body = sendMessagePayloadCaptor.getValue().getBody();
    assertThat(body).isNotNull();
    var sendMessageRequest = body.getMessage();
    assertThat(sendMessageRequest.getAlias()).containsSequence("messageType");
    assertThat(sendMessageRequest.getAlias()).containsSequence("REASSIGN_CONSULTANT");
    assertThat(sendMessageRequest.getMsg()).startsWith("enc:");
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.CONSULTANT_DEFAULT)
  void saveAliasOnlyMessageShouldReturnBadRequestIfTypeIsProtected() throws Exception {
    givenAuthenticatedUser();
    givenAnAliasOnlyMessage(true);

    mockMvc.perform(
        post("/messages/aliasonly/new")
            .cookie(CSRF_COOKIE)
            .header(CSRF_HEADER, CSRF_VALUE)
            .header("rcGroupId", RC_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(aliasOnlyMessage))
            .accept(MediaType.APPLICATION_JSON)
    ).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.CONSULTANT_DEFAULT)
  void saveAliasOnlyMessageShouldReturnBadRequestIfMessageTypeDoesNotSupportAMessage()
      throws Exception {
    givenAuthenticatedUser();
    givenAnAliasOnlyMessageWithUnsupportedMessage();

    mockMvc.perform(
        post("/messages/aliasonly/new")
            .cookie(CSRF_COOKIE)
            .header(CSRF_HEADER, CSRF_VALUE)
            .header("rcGroupId", RC_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(aliasOnlyMessage))
            .accept(MediaType.APPLICATION_JSON)
    ).andExpect(status().isBadRequest());
  }

  private void givenAMasterKey() {
    encryptionService.updateMasterKey(RandomStringUtils.randomAlphanumeric(16));
  }

  private void givenEncryptionCapturing(String encMessage, String plainMessage)
      throws CustomCryptoException {
    var encServiceMock = Mockito.mock(EncryptionService.class);
    ReflectionTestUtils.setField(rocketChatService, "encryptionService", encServiceMock);
    when(encServiceMock.encrypt(eq(encMessage), anyString())).thenReturn("encCameIn");
    when(encServiceMock.encrypt(eq(plainMessage), anyString())).thenReturn("plainCameIn");
  }

  private void givenSomeMessagesWithMutedUnmutedType() {
    var messages = easyRandom.objects(MessagesDTO.class, 5).collect(Collectors.toList());
    messages.get(1).setT("user-muted");
    messages.get(3).setT("user-unmuted");
    messages.forEach(message -> {
      var userMutedUnmuted = Set.of(MessageType.USER_MUTED, MessageType.USER_UNMUTED);
      if (userMutedUnmuted.contains(message.getAlias().getMessageType())) {
        var type = easyRandom.nextBoolean() ? MessageType.VIDEOCALL : MessageType.FURTHER_STEPS;
        message.getAlias().setMessageType(type);
      }
    });

    var messageStreamDTO = new MessageStreamDTO();
    messageStreamDTO.setMessages(messages);

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(), eq(MessageStreamDTO.class)))
        .thenReturn(new ResponseEntity<>(messageStreamDTO, HttpStatus.OK));
  }

  private void givenMessagesWithoutClearAlias() {
    var messages = easyRandom.objects(MessagesDTO.class, 5).collect(Collectors.toList());
    messages.forEach(message -> message.setAlias(null));

    var messageStreamDTO = new MessageStreamDTO();
    messageStreamDTO.setMessages(messages);

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(), eq(MessageStreamDTO.class)))
        .thenReturn(new ResponseEntity<>(messageStreamDTO, HttpStatus.OK));
  }

  private void givenAMessageWithAnEncryptedConsultantReassignment(String groupId) {
    consultantReassignment = new ConsultantReassignment();
    consultantReassignment.setToConsultantId(UUID.randomUUID());
    consultantReassignment.setStatus(ReassignStatus.REQUESTED);

    var message = easyRandom.nextObject(MessagesDTO.class);
    message.setOrg(null);
    message.setRid(groupId);
    var alias = new AliasMessageDTO();
    alias.setMessageType(MessageType.REASSIGN_CONSULTANT);
    message.setAlias(alias);

    try {
      var argsString = objectMapper.writeValueAsString(consultantReassignment);
      var encryptedMessage = encryptionService.encrypt(argsString, groupId);
      message.setMsg(encryptedMessage);
    } catch (CustomCryptoException | JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    messages = List.of(message);
    var messageStreamDTO = new MessageStreamDTO();
    messageStreamDTO.setMessages(messages);

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(), eq(MessageStreamDTO.class)))
        .thenReturn(new ResponseEntity<>(messageStreamDTO, HttpStatus.OK));
  }

  private void givenMessages() {
    var messages = easyRandom.objects(MessagesDTO.class, 5).collect(Collectors.toList());
    var messageStreamDTO = new MessageStreamDTO();
    messageStreamDTO.setMessages(messages);
    when(restTemplate.exchange(any(), any(HttpMethod.class), any(), eq(MessageStreamDTO.class)))
        .thenReturn(new ResponseEntity<>(messageStreamDTO, HttpStatus.OK));
  }

  private void givenAuthenticatedUser() {
    when(authenticatedUser.getUserId()).thenReturn(RandomStringUtils.randomAlphabetic(16));
  }

  private void givenRocketChatSystemUser() throws RocketChatUserNotInitializedException {
    var rcCredentials = new RocketChatCredentials();
    rcCredentials.setRocketChatToken(RandomStringUtils.randomAlphabetic(16));
    rcCredentials.setRocketChatUserId(RandomStringUtils.randomAlphabetic(16));
    when(chatCredentialsHelper.getSystemUser()).thenReturn(rcCredentials);
  }

  private void givenSuccessfulSendMessageResponse(String type, String roomId) {
    var successfulResponse = createSuccessfulMessageResult(type, roomId);
    when(restTemplate.postForObject(anyString(), sendMessagePayloadCaptor.capture(),
        eq(SendMessageResponseDTO.class))).thenReturn(successfulResponse);
  }

  private void givenAnAliasOnlyMessageWithUnsupportedMessage() {
    aliasOnlyMessage = easyRandom.nextObject(AliasOnlyMessageDTO.class);
    var messageType = easyRandom.nextBoolean()
        ? MessageType.FURTHER_STEPS
        : MessageType.E2EE_ACTIVATED;
    aliasOnlyMessage.setMessageType(messageType);
  }

  private void givenAnAliasOnlyMessageWithSupportedMessage() {
    aliasOnlyMessage = easyRandom.nextObject(AliasOnlyMessageDTO.class);
    aliasOnlyMessage.setMessageType(MessageType.REASSIGN_CONSULTANT);
    aliasOnlyMessage.getArgs().setStatus(ReassignStatus.REQUESTED);
  }

  private void givenAnAliasOnlyMessage(boolean muteUnmute) {
    aliasOnlyMessage = easyRandom.nextObject(AliasOnlyMessageDTO.class);
    aliasOnlyMessage.setArgs(null);

    MessageType messageType;
    if (muteUnmute) {
      messageType = easyRandom.nextBoolean() ? MessageType.USER_MUTED : MessageType.USER_UNMUTED;
    } else {
      do {
        messageType = easyRandom.nextObject(MessageType.class);
      } while (
          messageType.equals(MessageType.USER_MUTED) || messageType.equals(MessageType.USER_UNMUTED)
      );
    }

    aliasOnlyMessage.setMessageType(messageType);
  }

  private void givenAFeedbackGroupResponse() {
    var getGroupInfoDto = new GetGroupInfoDto();
    var feedbackGroup = new GroupDto();
    feedbackGroup.setName("feedback chat Akajsdhn");
    getGroupInfoDto.setGroup(feedbackGroup);
    var getGroupInfoDtoHttpEntity = new ResponseEntity<>(getGroupInfoDto, HttpStatus.OK);
    when(
        restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(),
            eq(GetGroupInfoDto.class))).thenReturn(
        getGroupInfoDtoHttpEntity);
  }

  private MessageDTO createMessage(String text, String type) {
    var feedbackMessage = new MessageDTO();
    feedbackMessage.setMessage(text);
    feedbackMessage.setT(type);
    return feedbackMessage;
  }

  private VideoCallMessageDTO createVideoCallMessage() {
    var consultantId = RandomStringUtils.randomAlphabetic(16);
    var vcm = new VideoCallMessageDTO();
    vcm.setRcUserId(RC_USER_ID);
    vcm.setInitiatorUserName("Heinrich");
    vcm.setInitiatorRcUserId(consultantId);
    vcm.setEventType(EventTypeEnum.IGNORED_CALL);
    return vcm;
  }

  private ForwardMessageDTO createForwardMessage() {
    var forwardMessage = new ForwardMessageDTO();
    forwardMessage.setMessage("plx forward");
    forwardMessage.setTimestamp(new Date().toString());
    forwardMessage.setT("e2e");
    forwardMessage.setUsername("Heinrich");
    forwardMessage.setRcUserId(RC_USER_ID);
    forwardMessage.setDisplayName("hk");
    return forwardMessage;
  }
}

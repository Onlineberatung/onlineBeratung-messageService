package de.caritas.cob.messageservice.api.controller;

import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_USER_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.createSuccessfulMessageResult;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.RocketChatUserNotInitializedException;
import de.caritas.cob.messageservice.api.facade.EmailNotificationFacade;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.model.AliasArgs;
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
import de.caritas.cob.messageservice.api.model.draftmessage.entity.DraftMessage;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GetGroupInfoDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.group.GroupDto;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageWrapper;
import de.caritas.cob.messageservice.api.repository.DraftMessageRepository;
import de.caritas.cob.messageservice.api.service.EncryptionService;
import de.caritas.cob.messageservice.api.service.LiveEventNotificationService;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import de.caritas.cob.messageservice.api.service.SessionService;
import de.caritas.cob.messageservice.api.service.dto.Message;
import de.caritas.cob.messageservice.api.service.dto.MessageResponse;
import de.caritas.cob.messageservice.api.service.dto.StringifiedMessageResponse;
import de.caritas.cob.messageservice.api.service.helper.RocketChatCredentialsHelper;
import de.caritas.cob.messageservice.api.service.statistics.StatisticsService;
import de.caritas.cob.messageservice.userservice.generated.web.model.GroupSessionListResponseDTO;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testing")
@AutoConfigureTestDatabase
class MessageControllerE2EIT {

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
  @SuppressWarnings("unused")
  private RocketChatService rocketChatService;

  @Autowired
  private DraftMessageRepository draftMessageRepository;

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

  @MockBean
  @SuppressWarnings("unused")
  private EmailNotificationFacade emailNotificationFacade;

  @Captor
  private ArgumentCaptor<HttpEntity<SendMessageWrapper>> sendMessagePayloadCaptor;

  @Captor
  private ArgumentCaptor<URI> uriArgumentCaptor;

  @MockBean
  SessionService sessionService;

  private AliasOnlyMessageDTO aliasOnlyMessage;
  private List<MessagesDTO> messages;
  private ConsultantReassignment consultantReassignment;
  private String messageId;
  private AliasArgs aliasArgs;
  private Message message;
  private MessagesDTO messagesDTO;
  private MessageType messageType;
  private DraftMessage draftMessage;

  @AfterEach
  void reset() {
    aliasOnlyMessage = null;
    encryptionService.updateMasterKey("initialMasterKey");
    messages = null;
    messageId = null;
    message = null;
    messageType = null;
    aliasArgs = null;

    if (nonNull(draftMessage)) {
      draftMessageRepository.deleteById(draftMessage.getId());
      draftMessage = null;
    }
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  void findMessagesShouldRespondWithMutedUnmutedAlias() throws Exception {
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

    assertGroupCall();
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void findMessagesShouldRespondWithAliasArgsConsultantReassign() throws Exception {
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
    assertGroupCall();
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  void findMessagesShouldRespondWithEmptyAlias() throws Exception {
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

    assertGroupCall();
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  void findMessagesShouldContainOrgMessage() throws Exception {
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
        .andExpect(jsonPath("messages[0].org").doesNotExist())
        .andExpect(jsonPath("messages[0].msg").isNotEmpty())
        .andExpect(jsonPath("messages[1].org").doesNotExist())
        .andExpect(jsonPath("messages[1].msg").isNotEmpty())
        .andExpect(jsonPath("messages[2].org").doesNotExist())
        .andExpect(jsonPath("messages[2].msg").isNotEmpty())
        .andExpect(jsonPath("messages[3].org").doesNotExist())
        .andExpect(jsonPath("messages[3].msg").isNotEmpty())
        .andExpect(jsonPath("messages[4].org").doesNotExist())
        .andExpect(jsonPath("messages[4].msg").isNotEmpty());

    assertGroupCall();
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void findMessagesShouldReturnBadRequestIfOffsetIsNegative() throws Exception {
    givenMessages();

    mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", RandomStringUtils.randomAlphabetic(16))
                .param("offset", String.valueOf(-1))
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void findMessagesShouldReturnBadRequestIfCountIsNegative() throws Exception {
    givenMessages();

    mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", RandomStringUtils.randomAlphabetic(16))
                .param("count", String.valueOf(-1))
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void findMessagesShouldReturnBadRequestIfSinceIsNotIso8601() throws Exception {
    givenMessages();

    mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", RandomStringUtils.randomAlphabetic(16))
                .param("since", RandomStringUtils.randomNumeric(10))
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void findMessagesShouldPassOffsetCountSinceAndUserFilterToChatApi() throws Exception {
    givenMessages();
    var offset = easyRandom.nextInt(9) + 1;
    var count = easyRandom.nextInt(9) + 1;
    var since = Instant.now();

    mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", RandomStringUtils.randomAlphabetic(16))
                .param("offset", String.valueOf(offset))
                .param("count", String.valueOf(count))
                .param("since", since.toString())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("messages", hasSize(5)));

    assertGroupCall(offset, count, since);
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void findMessageShouldRespondWithOkAndFullMessageIfItExists() throws Exception {
    givenAuthenticatedUser();
    givenAMasterKey();
    givenAValidMessageId();
    givenMessage(messageId, true);

    mockMvc.perform(
            get("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("_id", is(messageId)))
        .andExpect(jsonPath("alias.messageType", is(messageType.toString())))
        .andExpect(jsonPath("rid", is(message.getRid())))
        .andExpect(jsonPath("msg", is(message.getMsg())))
        .andExpect(jsonPath("ts", is(messagesDTO.getTs())))
        .andExpect(jsonPath("u._id", is(messagesDTO.getU().get_id())))
        .andExpect(jsonPath("u.username", is(messagesDTO.getU().getUsername())))
        .andExpect(jsonPath("u.name", is(messagesDTO.getU().getName())))
        .andExpect(jsonPath("unread", is(messagesDTO.isUnread())))
        .andExpect(jsonPath("_updatedAt", is(messagesDTO.get_updatedAt())))
        .andExpect(jsonPath("attachments", hasSize(messagesDTO.getAttachments().length)))
        .andExpect(jsonPath("attachments[0].title", is(messagesDTO.getAttachments()[0].getTitle())))
        .andExpect(jsonPath("file._id", is(messagesDTO.getFile().getId())))
        .andExpect(jsonPath("file.name", is(messagesDTO.getFile().getName())))
        .andExpect(jsonPath("file.type", is(messagesDTO.getFile().getType())))
        .andExpect(jsonPath("t", is(messagesDTO.getT())))
        .andExpect(jsonPath("org").doesNotExist());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.CONSULTANT_DEFAULT)
  void findMessageShouldRespondWithOkForAConsultant() throws Exception {
    givenAuthenticatedUser();
    givenAMasterKey();
    givenAValidMessageId();
    givenMessage(messageId, true);

    mockMvc.perform(
            get("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.ANONYMOUS_DEFAULT)
  void findMessageShouldRespondWithOkForAnonymous() throws Exception {
    givenAMasterKey();
    givenAValidMessageId();
    givenMessage(messageId, true);

    mockMvc.perform(
            get("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void findMessageShouldRespondWithOkAndMinimumMessageIfItExists() throws Exception {
    givenAuthenticatedUser();
    givenAMasterKey();
    givenAValidMessageId();
    givenMessage(messageId, false);

    mockMvc.perform(
            get("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("_id", is(messageId)))
        .andExpect(jsonPath("alias").doesNotExist())
        .andExpect(jsonPath("rid", is(message.getRid())))
        .andExpect(jsonPath("msg", is(message.getMsg())))
        .andExpect(jsonPath("ts").doesNotExist())
        .andExpect(jsonPath("u").doesNotExist())
        .andExpect(jsonPath("unread", is(false)))
        .andExpect(jsonPath("_updatedAt").doesNotExist())
        .andExpect(jsonPath("attachments").doesNotExist())
        .andExpect(jsonPath("file").doesNotExist())
        .andExpect(jsonPath("t").doesNotExist())
        .andExpect(jsonPath("org").doesNotExist());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void findMessageShouldRespondWithNotFoundIfItDoesNotExists() throws Exception {
    givenAuthenticatedUser();
    givenAMasterKey();
    givenAValidMessageId();
    givenAGetChatMessageNotFoundResponse(messageId);

    mockMvc.perform(
            get("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void findDraftMessageShouldRespondWithE2eEncryptedMessage() throws Exception {
    givenAMasterKey();
    givenASuccessfulE2eDraftMessageResponse();
    givenAuthenticatedUser(draftMessage.getUserId());

    mockMvc.perform(
            get("/messages/draft")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", draftMessage.getUserId())
                .header("rcGroupId", draftMessage.getRcGroupId())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("message", is(draftMessage.getMessage())))
        .andExpect(jsonPath("t", is("e2e")));
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void patchMessageShouldRespondWithClientErrorWhenMessageIdHasWrongFormat()
      throws Exception {
    givenAuthenticatedUser();
    givenAPatchSupportedReassignArg();
    givenAWronglyFormattedMessageId();

    mockMvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs))
        )
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void patchMessageShouldRespondWithBadRequestWhenRcTokenMissing() throws Exception {
    givenAuthenticatedUser();
    givenAPatchSupportedReassignArg();
    givenAValidMessageId();

    mockMvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs))
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void patchMessageShouldRespondWithBadRequestWhenRcUserIdMissing() throws Exception {
    givenAuthenticatedUser();
    givenAPatchSupportedReassignArg();
    givenAValidMessageId();

    mockMvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs))
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void patchMessageShouldRespondWithBadRequestIfStatusIsInitial() throws Exception {
    givenAuthenticatedUser();
    givenARequestedReassignArg();
    givenAValidMessageId();

    mockMvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs))
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void patchMessageShouldRespondWithBadRequestIfStatusIsArbitrary() throws Exception {
    givenAuthenticatedUser();
    givenAValidMessageId();
    var content = "{ \"status\": \"" + RandomStringUtils.randomAlphabetic(16) + "\" }";

    mockMvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void patchMessageShouldRespondWithBadRequestIfFoundMessageIsNotReassignmentEvent()
      throws Exception {
    givenAuthenticatedUser();
    givenAPatchSupportedReassignArg();
    givenAValidMessageId();
    givenANonEventGetChatMessageResponse(messageId);

    mockMvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs))
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void patchMessageShouldRespondWithNotFoundIfMessageDoesNotExist() throws Exception {
    givenAuthenticatedUser();
    givenAPatchSupportedReassignArg();
    givenAValidMessageId();
    givenAGetChatMessageNotFoundResponse(messageId);

    mockMvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs))
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void patchMessageShouldRespondWithInternalServerErrorIfRocketChatResponseSevereError()
      throws Exception {
    givenAuthenticatedUser();
    givenAPatchSupportedReassignArg();
    givenAValidMessageId();
    givenAGetChatMessageSevereErrorResponse(messageId);

    mockMvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs))
        )
        .andExpect(status().isInternalServerError());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void patchMessageShouldRespondWithNoContent() throws Exception {
    givenAuthenticatedUser();
    givenAPatchSupportedReassignArg();
    givenAValidMessageId();
    givenAMasterKey();
    givenRocketChatSystemUser();
    givenASuccessfulGetChatMessageReassignmentResponse(messageId);
    givenASuccessfulUpdateChatMessageResponse();

    mockMvc.perform(
            patch("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasArgs))
        )
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void deleteMessageShouldRespondWithNotFoundIfMessageDoesNotExist() throws Exception {
    givenAuthenticatedUser();
    givenAValidMessageId();
    givenAGetChatMessageNotFoundResponse(messageId);

    mockMvc.perform(
            delete("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isNotFound());

    verify(restTemplate, never()).postForEntity(
        endsWith("/api/v1/method.call/deleteMessage"), any(), eq(StringifiedMessageResponse.class)
    );
    verify(restTemplate, never()).postForEntity(
        endsWith("/api/v1/method.call/deleteFileMessage"), any(),
        eq(StringifiedMessageResponse.class)
    );
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void deleteMessageShouldRespondWithForbiddenIfMessageIsNotFromUser() throws Exception {
    givenAuthenticatedUser();
    givenAValidMessageId();
    givenAMasterKey();
    givenMessage(messageId, true, RandomStringUtils.randomAlphabetic(16));

    mockMvc.perform(
            delete("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isForbidden());

    verify(restTemplate, never()).postForEntity(
        endsWith("/api/v1/method.call/deleteMessage"), any(), eq(StringifiedMessageResponse.class)
    );
    verify(restTemplate, never()).postForEntity(
        endsWith("/api/v1/method.call/deleteFileMessage"), any(),
        eq(StringifiedMessageResponse.class)
    );
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void deleteMessageShouldRespondWithInternalServerErrorIfDeletionFails() throws Exception {
    givenAuthenticatedUser();
    givenAValidMessageId();
    givenAMasterKey();
    var rcUserId = RandomStringUtils.randomAlphabetic(16);
    givenMessage(messageId, true, rcUserId);
    givenDeletableMessage(false);

    mockMvc.perform(
            delete("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", rcUserId)
        )
        .andExpect(status().isInternalServerError());

    verify(restTemplate).postForEntity(
        endsWith("/api/v1/method.call/deleteMessage"), any(), eq(StringifiedMessageResponse.class)
    );
    verify(restTemplate, never()).postForEntity(
        endsWith("/api/v1/method.call/deleteFileMessage"), any(),
        eq(StringifiedMessageResponse.class)
    );
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void deleteMessageShouldRespondWithNoContentIfDeleteMessageSucceeds() throws Exception {
    givenAuthenticatedUser();
    givenAValidMessageId();
    givenAMasterKey();
    var rcUserId = RandomStringUtils.randomAlphabetic(16);
    givenMessage(messageId, true, rcUserId);
    givenDeletableMessage(true);
    givenDeletableFile(true);

    mockMvc.perform(
            delete("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", rcUserId)
        )
        .andExpect(status().isNoContent());

    verify(restTemplate).postForEntity(
        endsWith("/api/v1/method.call/deleteMessage"), any(), eq(StringifiedMessageResponse.class)
    );
    verify(restTemplate).postForEntity(
        endsWith("/api/v1/method.call/deleteFileMessage"), any(),
        eq(StringifiedMessageResponse.class)
    );
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void deleteMessageShouldRespondWithNoContentIfDeleteMessageAndAttachmentSucceed()
      throws Exception {
    givenAuthenticatedUser();
    givenAValidMessageId();
    givenAMasterKey();
    var rcUserId = RandomStringUtils.randomAlphabetic(16);
    givenMessage(messageId, true, rcUserId);
    givenDeletableMessage(true);
    givenDeletableFile(true);

    mockMvc.perform(
            delete("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", rcUserId)
        )
        .andExpect(status().isNoContent());

    verify(restTemplate).postForEntity(
        endsWith("/api/v1/method.call/deleteMessage"), any(), eq(StringifiedMessageResponse.class)
    );
    verify(restTemplate).postForEntity(
        endsWith("/api/v1/method.call/deleteFileMessage"), any(),
        eq(StringifiedMessageResponse.class)
    );
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void deleteMessageShouldRespondWithNoContentIfDeleteMessageSucceedsButMessageHasNoFile()
      throws Exception {
    givenAuthenticatedUser();
    givenAValidMessageId();
    givenAMasterKey();
    var rcUserId = RandomStringUtils.randomAlphabetic(16);
    givenMessage(messageId, true, rcUserId, false);
    givenDeletableMessage(true);

    mockMvc.perform(
            delete("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", rcUserId)
        )
        .andExpect(status().isNoContent());

    verify(restTemplate).postForEntity(
        endsWith("/api/v1/method.call/deleteMessage"), any(), eq(StringifiedMessageResponse.class)
    );
    verify(restTemplate, never()).postForEntity(
        endsWith("/api/v1/method.call/deleteFileMessage"), any(),
        eq(StringifiedMessageResponse.class)
    );
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.USER_DEFAULT)
  void deleteMessageShouldRespondWithMultiStatusIfDeleteMessageSucceedsButDeleteAttachmentFails()
      throws Exception {
    givenAuthenticatedUser();
    givenAValidMessageId();
    givenAMasterKey();
    var rcUserId = RandomStringUtils.randomAlphabetic(16);
    givenMessage(messageId, true, rcUserId);
    givenDeletableMessage(true);
    givenDeletableFile(false);

    mockMvc.perform(
            delete("/messages/{messageId}", messageId)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", rcUserId)
        )
        .andExpect(status().isMultiStatus());

    verify(restTemplate).postForEntity(
        endsWith("/api/v1/method.call/deleteMessage"), any(), eq(StringifiedMessageResponse.class)
    );
    verify(restTemplate).postForEntity(
        endsWith("/api/v1/method.call/deleteFileMessage"), any(),
        eq(StringifiedMessageResponse.class)
    );
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  void sendMessageShouldTransmitTypeOfMessage() throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    when(sessionService.findSessionBelongingToRcGroupId(
        Mockito.anyString(), Mockito.anyString())).thenReturn(new GroupSessionListResponseDTO());
    var rcGroupId = RandomStringUtils.randomAlphabetic(16);
    givenSuccessfulSendMessageResponse("p", rcGroupId);
    givenAMasterKey();
    MessageDTO encryptedMessage = createMessage("enc.secret_message", "p");

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
    assertThat(sendMessageRequest.getT()).isEqualTo("p");
    assertThat(sendMessageRequest.getMsg()).startsWith("enc:");
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  void sendMessageShouldReturnSendMessageResultOnSuccessfulRequest() throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    var rcGroupId = RandomStringUtils.randomAlphabetic(16);
    givenSuccessfulSendMessageResponse("e2e", rcGroupId);
    givenAMasterKey();
    when(sessionService.findSessionBelongingToRcGroupId(
        Mockito.anyString(), Mockito.anyString())).thenReturn(new GroupSessionListResponseDTO().sessions(
        Lists.newArrayList(new de.caritas.cob.messageservice.userservice.generated.web.model.GroupSessionResponseDTO()
                .user(new de.caritas.cob.messageservice.userservice.generated.web.model.SessionUserDTO().id("userId")))));

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
  void createVideoHintMessageShouldReturnSendMessageResultOnSuccessfulRequest()
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
  void createFeedbackMessageShouldReturnSendMessageResultOnSuccessfulRequest()
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
        .andExpect(jsonPath("org").doesNotExist())
        .andExpect(jsonPath("t", is(nullValue())))
        .andExpect(jsonPath("_id").isNotEmpty());

    var messageRequestPayload = sendMessagePayloadCaptor.getValue().getBody();
    assertNotNull(messageRequestPayload);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USE_FEEDBACK})
  void forwardMessageShouldReturnSendMessageResultOnSuccessfulRequest()
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
  void saveAliasOnlyMessageShouldReturnBadRequestIfReassignHasNoConsultantId() throws Exception {
    givenAuthenticatedUser();
    givenAReassignmentEventWithNoConsultantId();

    mockMvc.perform(
            post("/messages/aliasonly/new")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessage))
                .accept(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isBadRequest());
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

    var decryptedMsg = encryptionService.decrypt(sendMessageRequest.getMsg(), RC_GROUP_ID);
    var decryptedConsultantReassignment =
        objectMapper.readValue(decryptedMsg, ConsultantReassignment.class);
    assertEquals(
        aliasOnlyMessage.getArgs().getFromConsultantId(),
        decryptedConsultantReassignment.getFromConsultantId()
    );
  }

  @Test
  @WithMockUser(authorities = AuthorityValue.CONSULTANT_DEFAULT)
  void saveAliasOnlyMessageShouldReturnBadRequest_When_aliasIsEmpty() throws Exception {
    givenAuthenticatedUser();
    givenRocketChatSystemUser();
    givenAnAliasOnlyMessageWithSupportedMessageAndEmptyArgs();
    givenSuccessfulSendMessageResponse(null, RC_GROUP_ID);
    givenAMasterKey();

    mockMvc.perform(
            post("/messages/aliasonly/new")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcGroupId", RC_GROUP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(aliasOnlyMessage))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
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

  private void givenMessage(String id, boolean full)
      throws JsonProcessingException, CustomCryptoException {
    givenMessage(id, full, null);
  }

  private void givenMessage(String id, boolean full, String userId)
      throws CustomCryptoException, JsonProcessingException {
    givenMessage(id, full, userId, true);
  }

  private void givenMessage(String id, boolean full, String userId, boolean hasFileId)
      throws JsonProcessingException, CustomCryptoException {
    var response = new MessageResponse();
    response.setSuccess(true);

    message = easyRandom.nextObject(Message.class);
    message.setId(id);
    message.setAlias(null);

    if (full) {
      var alias = easyRandom.nextObject(AliasMessageDTO.class);
      messageType = alias.getMessageType();
      var aliasString = objectMapper.writeValueAsString(alias);
      var encodedAlias = URLEncoder.encode(aliasString, StandardCharsets.UTF_8);
      message.setAlias(encodedAlias);

      messagesDTO = easyRandom.nextObject(MessagesDTO.class);
      if (nonNull(userId)) {
        messagesDTO.getU().set_id(userId);
      }

      var props = message.getOtherProperties();
      props.put("u", messagesDTO.getU());
      props.put("attachments", messagesDTO.getAttachments());
      if (hasFileId) {
        props.put("file", messagesDTO.getFile());
      }
      props.put("org", encryptionService.encrypt(message.getMsg(), message.getRid()));
      props.put("_updatedAt", messagesDTO.get_updatedAt());
      props.put("t", messagesDTO.getT());
      props.put("ts", messagesDTO.getTs());
      props.put("unread", messagesDTO.isUnread());
    }
    response.setMessage(message);

    var urlSuffix = "/chat.getMessage?msgId=" + id;
    when(restTemplate.exchange(endsWith(urlSuffix), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(MessageResponse.class))).thenReturn(ResponseEntity.ok().body(response));
  }

  private void givenDeletableMessage(boolean success) {
    var urlSuffix = "/api/v1/method.call/deleteMessage";
    var messageResponse = easyRandom.nextObject(StringifiedMessageResponse.class);
    messageResponse.setSuccess(true);
    if (success) {
      while (messageResponse.getMessage().contains("\"error\"")) {
        messageResponse.setMessage(RandomStringUtils.randomAlphanumeric(32));
      }
    } else {
      messageResponse.setMessage(
          RandomStringUtils.randomAlphanumeric(12)
              + "\"error\""
              + RandomStringUtils.randomAlphanumeric(12));
    }

    when(restTemplate.postForEntity(
        endsWith(urlSuffix), any(HttpEntity.class), eq(StringifiedMessageResponse.class)))
        .thenReturn(ResponseEntity.ok(messageResponse));
  }

  private void givenDeletableFile(boolean success) {
    var urlSuffix = "/api/v1/method.call/deleteFileMessage";
    var messageResponse = easyRandom.nextObject(StringifiedMessageResponse.class);
    messageResponse.setSuccess(true);
    if (success) {
      while (messageResponse.getMessage().contains("\"error\"")) {
        messageResponse.setMessage(RandomStringUtils.randomAlphanumeric(32));
      }
    } else {
      messageResponse.setMessage(
          RandomStringUtils.randomAlphanumeric(12)
              + "\"error\""
              + RandomStringUtils.randomAlphanumeric(12));
    }

    when(restTemplate.postForEntity(
        endsWith(urlSuffix), any(HttpEntity.class), eq(StringifiedMessageResponse.class)))
        .thenReturn(ResponseEntity.ok(messageResponse));
  }

  private void givenAWronglyFormattedMessageId() {
    int idLength = 0;
    while (idLength < 1 || idLength == 17) {
      idLength = easyRandom.nextInt(31) + 1;
    }
    messageId = RandomStringUtils.randomAlphanumeric(idLength);
  }

  private void givenAValidMessageId() {
    messageId = RandomStringUtils.randomAlphanumeric(17);
  }

  private void givenAPatchSupportedReassignArg() {
    aliasArgs = new AliasArgs();
    var status = easyRandom.nextBoolean() ? ReassignStatus.REJECTED : ReassignStatus.CONFIRMED;
    aliasArgs.setStatus(status);
  }

  private void givenARequestedReassignArg() {
    aliasArgs = new AliasArgs();
    aliasArgs.setStatus(ReassignStatus.REQUESTED);
  }

  private void givenAuthenticatedUser() {
    when(authenticatedUser.getUserId()).thenReturn(RandomStringUtils.randomAlphabetic(16));
  }

  private void givenAuthenticatedUser(String userId) {
    when(authenticatedUser.getUserId()).thenReturn(userId);
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

  private void givenASuccessfulGetChatMessageReassignmentResponse(String messageId)
      throws JsonProcessingException, CustomCryptoException {
    var response = new MessageResponse();
    response.setSuccess(true);

    var message = easyRandom.nextObject(Message.class);
    message.setId(messageId);

    var alias = new AliasMessageDTO();
    alias.setMessageType(MessageType.REASSIGN_CONSULTANT);
    var aliasString = objectMapper.writeValueAsString(alias);
    var encodedAlias = URLEncoder.encode(aliasString, StandardCharsets.UTF_8);
    message.setAlias(encodedAlias);

    var consultantReassignment = new ConsultantReassignment();
    consultantReassignment.setStatus(ReassignStatus.REQUESTED);
    consultantReassignment.setToConsultantId(UUID.randomUUID());
    var msg = objectMapper.writeValueAsString(consultantReassignment);
    var encryptedMessage = encryptionService.encrypt(msg, message.getRid());
    message.setMsg(encryptedMessage);
    response.setMessage(message);

    var urlSuffix = "/chat.getMessage?msgId=" + messageId;
    when(restTemplate.exchange(endsWith(urlSuffix), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(MessageResponse.class))).thenReturn(ResponseEntity.ok().body(response));
  }

  private void givenANonEventGetChatMessageResponse(String messageId) {
    var response = new MessageResponse();
    response.setSuccess(true);

    var message = easyRandom.nextObject(Message.class);
    message.setId(messageId);
    response.setMessage(message);

    var urlSuffix = "/chat.getMessage?msgId=" + messageId;
    when(restTemplate.exchange(endsWith(urlSuffix), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(MessageResponse.class))).thenReturn(ResponseEntity.ok().body(response));
  }

  private void givenASuccessfulUpdateChatMessageResponse() {
    var response = new MessageResponse();
    response.setSuccess(true);

    var urlSuffix = "/api/v1/chat.update";
    when(restTemplate.postForObject(endsWith(urlSuffix), any(HttpEntity.class),
        eq(MessageResponse.class))).thenReturn(response);
  }

  private void givenASuccessfulE2eDraftMessageResponse() {
    draftMessage = easyRandom.nextObject(DraftMessage.class);
    draftMessage.setId(null);
    draftMessage.setUserId(UUID.randomUUID().toString());
    draftMessage.setT("e2e");
    draftMessage.setMessage("enc." + RandomStringUtils.randomAlphanumeric(32));

    draftMessageRepository.save(draftMessage);
  }

  private void givenAGetChatMessageNotFoundResponse(String messageId)
      throws JsonProcessingException {
    var payload = new MessageResponse();
    payload.setSuccess(false);
    var body = objectMapper.writeValueAsString(payload).getBytes();
    var statusText = HttpStatus.BAD_REQUEST.getReasonPhrase();
    var exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST, statusText, body, null);

    var urlSuffix = "/chat.getMessage?msgId=" + messageId;
    when(restTemplate.exchange(endsWith(urlSuffix), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(MessageResponse.class))).thenThrow(exception);
  }

  private void givenAGetChatMessageSevereErrorResponse(String messageId) {
    var exception = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);

    var urlSuffix = "/chat.getMessage?msgId=" + messageId;
    when(restTemplate.exchange(endsWith(urlSuffix), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(MessageResponse.class))).thenThrow(exception);
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

  private void givenAnAliasOnlyMessageWithSupportedMessageAndEmptyArgs() {
    aliasOnlyMessage = easyRandom.nextObject(AliasOnlyMessageDTO.class);
    aliasOnlyMessage.setMessageType(MessageType.REASSIGN_CONSULTANT);
    aliasOnlyMessage.setArgs(null);
  }

  private void givenAReassignmentEventWithNoConsultantId() {
    aliasOnlyMessage = easyRandom.nextObject(AliasOnlyMessageDTO.class);
    var args = new AliasArgs();
    args.setStatus(ReassignStatus.REQUESTED);
    aliasOnlyMessage.setArgs(args);
    aliasOnlyMessage.setMessageType(MessageType.REASSIGN_CONSULTANT);
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
          messageType == MessageType.USER_MUTED || messageType == MessageType.USER_UNMUTED
              || messageType == MessageType.REASSIGN_CONSULTANT
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

  private void assertGroupCall() {
    assertGroupCall(0, 0, Instant.MIN);
  }

  private void assertGroupCall(int offset, int count, Instant instant) {
    verify(restTemplate).exchange(uriArgumentCaptor.capture(), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(MessageStreamDTO.class));

    var uri = uriArgumentCaptor.getValue();
    assertEquals("/api/v1/groups.messages", uri.getPath());

    var query = uri.getQuery();
    var offsetPair = "offset=" + offset;
    assertTrue(query.contains(offsetPair + "&") || query.endsWith(offsetPair));
    var countPair = "count=" + count;
    assertTrue(query.contains(countPair + "&") || query.endsWith(countPair));
    var queryPair = "query={\"$and\":["
        + "{\"ts\":{\"$gt\":{\"$date\":\"" + instant + "\"}}},"
        + "{\"u.username\":{\"$ne\":\"rcTechUserName\"}}"
        + "]}";
    assertTrue(query.contains(queryPair + "&") || query.endsWith(queryPair));
  }
}

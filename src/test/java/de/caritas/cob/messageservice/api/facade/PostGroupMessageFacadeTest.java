package de.caritas.cob.messageservice.api.facade;

import static de.caritas.cob.messageservice.testhelper.TestConstants.DONT_SEND_NOTIFICATION;
import static de.caritas.cob.messageservice.testhelper.TestConstants.GET_GROUP_INFO_DTO;
import static de.caritas.cob.messageservice.testhelper.TestConstants.GET_GROUP_INFO_DTO_FEEDBACK_CHAT;
import static de.caritas.cob.messageservice.testhelper.TestConstants.SEND_NOTIFICATION;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import de.caritas.cob.messageservice.api.authorization.Role;
import de.caritas.cob.messageservice.api.exception.BadRequestException;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatSendMessageException;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ChatMessage;
import de.caritas.cob.messageservice.api.model.ChatMessage.ChatMessageBuilder;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResultDTO;
import de.caritas.cob.messageservice.api.service.DraftMessageService;
import de.caritas.cob.messageservice.api.service.LiveEventNotificationService;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import de.caritas.cob.messageservice.api.service.statistics.StatisticsService;
import de.caritas.cob.messageservice.api.service.statistics.event.CreateMessageStatisticsEvent;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.UserRole;
import java.util.Objects;
import org.apache.commons.collections4.SetUtils;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class PostGroupMessageFacadeTest {

  private static final String RC_TOKEN = "r94qMDk8gtgVNzqCq9zD2hELK-eXGB5VHlUVBgE8a8f";
  private static final String RC_USER_ID = "pptLwARyTMzbTTRdg";
  private static final String CONSULTANT_ID = "d63f4cc0-215d-40e2-a866-2d3e910f0590";
  private static final String RC_GROUP_ID = "fR2Rz7dmWmHdXE8uz";
  private static final String RC_FEEDBACK_GROUP_ID = "fR2Rz7dmWmHdXE8uz";
  private static final String MESSAGE = "Lorem ipsum";
  private static final String RC_SYSTEM_USER_ID = "systemUserId";
  private static final SendMessageResponseDTO POST_MESSAGE_RESPONSE_DTO = new SendMessageResponseDTO(
      new SendMessageResultDTO(), true, null, null);
  private static final SendMessageResponseDTO POST_MESSAGE_RESPONSE_DTO_UNSUCCESSFUL = new SendMessageResponseDTO(
      new SendMessageResultDTO(), false, null, null);
  @InjectMocks
  private PostGroupMessageFacade postGroupMessageFacade;

  @Mock
  private RocketChatService rocketChatService;

  @Mock
  private EmailNotificationFacade emailNotificationFacade;

  @Mock
  private LiveEventNotificationService liveEventNotificationService;

  @Mock
  private DraftMessageService draftMessageService;

  @Mock
  private StatisticsService statisticsService;

  @Mock
  private AuthenticatedUser authenticatedUser;

  @Before
  public void setup() {
    setField(this.postGroupMessageFacade, "rocketChatSystemUserId", RC_SYSTEM_USER_ID);
    when(authenticatedUser.getRoles())
        .thenReturn(SetUtils.unmodifiableSet(Role.CONSULTANT.getRoleName()));
    when(authenticatedUser.getUserId())
        .thenReturn(CONSULTANT_ID);
  }

  /**
   * Tests for method: postGroupMessage
   */
  @Test(expected = InternalServerErrorException.class)
  public void postGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatPostMessageFails()
      throws CustomCryptoException {

    RocketChatSendMessageException rocketChatSendMessageException =
        new RocketChatSendMessageException(new Exception());

    var groupMessage = createGroupMessage().build();
    when(rocketChatService.postGroupMessage(groupMessage)).thenThrow(
        rocketChatSendMessageException);

    postGroupMessageFacade.postGroupMessage(groupMessage);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatServiceReturnsEmptyDTO()
      throws CustomCryptoException {

    var groupMessage = createGroupMessage().build();
    when(rocketChatService.postGroupMessage(groupMessage)).thenReturn(null);

    postGroupMessageFacade.postGroupMessage(groupMessage);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatServiceReturnsUnsuccessful()
      throws CustomCryptoException {

    var groupMessage = createGroupMessage().build();
    when(rocketChatService.postGroupMessage(groupMessage)).thenReturn(
        POST_MESSAGE_RESPONSE_DTO_UNSUCCESSFUL);

    postGroupMessageFacade.postGroupMessage(groupMessage);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postGroupMessage_Should_ReturnRocketChatPostMarkGroupAsReadExceptionAndNotSendNotification_When_RocketChatMarkGroupAsReadFails()
      throws CustomCryptoException {

    var groupMessage = createGroupMessage().build();
    when(rocketChatService.postGroupMessage(groupMessage)).thenReturn(null);

    postGroupMessageFacade.postGroupMessage(groupMessage);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_ReturnCreatedAndSendNotification_When_MessageWasSentAndNotificationIsSetToTrue()
      throws CustomCryptoException {

    var groupMessage = createGroupMessage().sendNotification(SEND_NOTIFICATION).build();
    when(rocketChatService.postGroupMessage(groupMessage)).thenReturn(POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postGroupMessage(groupMessage);

    verify(emailNotificationFacade, times(1)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_ReturnCreatedAndNotSendNotification_When_MessageWasSentAndNotificationIsSetToFalse()
      throws CustomCryptoException {

    var groupMessage = createGroupMessage().build();
    when(rocketChatService.postGroupMessage(groupMessage)).thenReturn(POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postGroupMessage(groupMessage);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(Mockito.anyString());
  }

  /**
   * Tests for method: postFeedbackGroupMessage
   */
  @Test(expected = InternalServerErrorException.class)
  public void postFeedbackGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatPostMessageFails()
      throws CustomCryptoException {

    RocketChatSendMessageException rocketChatSendMessageException =
        new RocketChatSendMessageException(new Exception());

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    var feedbackGroupMessage = createFeedbackGroupMessage().build();
    when(rocketChatService.postGroupMessage(feedbackGroupMessage)).thenThrow(
        rocketChatSendMessageException);

    postGroupMessageFacade.postFeedbackGroupMessage(feedbackGroupMessage);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postFeedbackGroupMessage_Should_ReturnInternalServerErrorAndNotSendFeedbackNotification_When_RocketChatServiceReturnsEmptyDTO()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    var feedbackGroupMessage = createFeedbackGroupMessage().build();
    when(rocketChatService.postGroupMessage(feedbackGroupMessage))
        .thenReturn(null);

    postGroupMessageFacade.postFeedbackGroupMessage(feedbackGroupMessage);

    verify(emailNotificationFacade, times(0)).sendFeedbackEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postFeedbackGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatServiceReturnsUnsuccessful()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    var groupMessage = createGroupMessage().build();
    when(rocketChatService.postGroupMessage(groupMessage)).thenReturn(
        POST_MESSAGE_RESPONSE_DTO_UNSUCCESSFUL);
    var feedbackGroupMessage = createFeedbackGroupMessage().build();

    postGroupMessageFacade.postFeedbackGroupMessage(feedbackGroupMessage);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postFeedbackGroupMessage_Should_ReturnRocketChatPostMarkGroupAsReadExceptionAndNotSendFeedbackNotification_When_RocketChatMarkGroupAsReadFails()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    var feedbackGroupMessage = createFeedbackGroupMessage().build();
    when(rocketChatService.postGroupMessage(feedbackGroupMessage)).thenReturn(null);

    postGroupMessageFacade.postFeedbackGroupMessage(feedbackGroupMessage);

    verify(emailNotificationFacade, times(0)).sendFeedbackEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test
  public void postFeedbackGroupMessage_Should_ReturnCreatedAndSendFeedbackNotification_When_RocketChatServiceSucceeds()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    var feedbackGroupMessage = createFeedbackGroupMessage().build();
    when(rocketChatService.postGroupMessage(feedbackGroupMessage)).thenReturn(
        POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postFeedbackGroupMessage(feedbackGroupMessage);

    verify(emailNotificationFacade, times(1)).sendFeedbackEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test(expected = BadRequestException.class)
  public void postFeedbackGroupMessage_Should_ReturnBadRequestAndNotSendFeedbackNotification_When_GroupIdIsNoFeedbackChat() {
    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO);
    var groupMessage = createGroupMessage().build();

    postGroupMessageFacade.postFeedbackGroupMessage(groupMessage);

    verify(emailNotificationFacade, times(0)).sendFeedbackEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_sendLiveNotification_When_RocketChatServiceSucceeds()
      throws CustomCryptoException {

    var groupMessage = createGroupMessage().build();
    when(rocketChatService.postGroupMessage(groupMessage)).thenReturn(POST_MESSAGE_RESPONSE_DTO);
    var noNotificationMessage = createGroupMessage().sendNotification(DONT_SEND_NOTIFICATION)
        .build();

    postGroupMessageFacade.postGroupMessage(noNotificationMessage);

    verify(this.liveEventNotificationService, times(1)).sendLiveEvent(RC_GROUP_ID);
  }

  @Test
  public void postFeedbackGroupMessage_Should_sendLiveNotification_When_RocketChatServiceSucceeds()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    var feedbackGroupMessage = createFeedbackGroupMessage().build();
    when(rocketChatService.postGroupMessage(feedbackGroupMessage)).thenReturn(
        POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postFeedbackGroupMessage(feedbackGroupMessage);

    verify(this.liveEventNotificationService, times(1)).sendLiveEvent(RC_FEEDBACK_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_notSendLiveNotification_When_userIsRocketChatSystemUser()
      throws CustomCryptoException {

    var systemFeedbackMessage = createFeedbackGroupMessage().rcUserId(RC_SYSTEM_USER_ID).build();
    when(rocketChatService.postGroupMessage(systemFeedbackMessage)).thenReturn(
        POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postGroupMessage(systemFeedbackMessage);

    verifyNoInteractions(this.liveEventNotificationService);
  }

  @Test
  public void postGroupMessage_Should_deleteDraftMessage_When_RocketChatServiceSucceeds()
      throws CustomCryptoException {

    var groupMessage = createGroupMessage().build();
    when(rocketChatService.postGroupMessage(groupMessage))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postGroupMessage(groupMessage);

    verify(this.draftMessageService, times(1)).deleteDraftMessageIfExist(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_FireCreateMessageStatisticsEvent()
      throws CustomCryptoException {

    var groupMessage = createGroupMessage().build();
    when(rocketChatService.postGroupMessage(groupMessage)).thenReturn(POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postGroupMessage(groupMessage);

    verify(statisticsService, times(1))
        .fireEvent(any(CreateMessageStatisticsEvent.class));

    ArgumentCaptor<CreateMessageStatisticsEvent> captor = ArgumentCaptor.forClass(
        CreateMessageStatisticsEvent.class);
    verify(statisticsService, times(1)).fireEvent(captor.capture());
    String userId = Objects.requireNonNull(
        ReflectionTestUtils.getField(captor.getValue(), "userId")).toString();
    assertThat(userId, is(CONSULTANT_ID));
    String userRole = Objects.requireNonNull(
        ReflectionTestUtils.getField(captor.getValue(), "userRole")).toString();
    assertThat(userRole, is(UserRole.CONSULTANT.toString()));
    String rcGroupId = Objects.requireNonNull(
        ReflectionTestUtils.getField(captor.getValue(), "rcGroupId")).toString();
    assertThat(rcGroupId, is(RC_GROUP_ID));
    boolean hasAttachment = Boolean.parseBoolean(Objects.requireNonNull(
        ReflectionTestUtils.getField(captor.getValue(), "hasAttachment")).toString());
    assertThat(hasAttachment, is(false));

  }

  @Test
  public void postFeedbackGroupMessage_Should_deleteDraftMessage_When_RocketChatServiceSucceeds()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    var feedbackGroupMessage = createFeedbackGroupMessage().build();
    when(rocketChatService.postGroupMessage(feedbackGroupMessage)).thenReturn(
        POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postFeedbackGroupMessage(feedbackGroupMessage);

    verify(this.draftMessageService, times(1)).deleteDraftMessageIfExist(RC_FEEDBACK_GROUP_ID);
  }

  @Test
  public void createVideoHintMessage_Should_triggerRocketChatPost_When_paramsAreGiven() {
    VideoCallMessageDTO callMessageDTO = new EasyRandom().nextObject(VideoCallMessageDTO.class);

    this.postGroupMessageFacade.createVideoHintMessage("rcGroupId", callMessageDTO);

    verify(this.rocketChatService, times(1)).postAliasOnlyMessageAsSystemUser(anyString(),
        any());
  }

  @Test
  public void postAliasOnlyMessage_Should_triggerRocketChatPostWithCorrectMessageType_When_RcGroupIdIsGiven() {
    this.postGroupMessageFacade.postAliasOnlyMessage(RC_GROUP_ID, MessageType.FURTHER_STEPS);

    ArgumentCaptor<AliasMessageDTO> captor = ArgumentCaptor.forClass(AliasMessageDTO.class);
    verify(rocketChatService).postAliasOnlyMessageAsSystemUser(anyString(), captor.capture());
    verify(this.rocketChatService, times(1)).postAliasOnlyMessageAsSystemUser(anyString(),
        any());
    assertThat(captor.getValue().getMessageType(), is(MessageType.FURTHER_STEPS));
  }

  private ChatMessageBuilder createFeedbackGroupMessage() {
    return ChatMessage.builder().rcToken(RC_TOKEN).rcUserId(RC_USER_ID)
        .rcGroupId(RC_FEEDBACK_GROUP_ID).text(MESSAGE);
  }

  private ChatMessageBuilder createGroupMessage() {
    return ChatMessage.builder().rcToken(RC_TOKEN).rcUserId(RC_USER_ID)
        .rcGroupId(RC_GROUP_ID).text(MESSAGE);
  }
}

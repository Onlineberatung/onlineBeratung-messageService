package de.caritas.cob.messageservice.api.facade;

import static de.caritas.cob.messageservice.testhelper.TestConstants.GET_GROUP_INFO_DTO;
import static de.caritas.cob.messageservice.testhelper.TestConstants.GET_GROUP_INFO_DTO_FEEDBACK_CHAT;
import static de.caritas.cob.messageservice.testhelper.TestConstants.MESSAGE_DTO_WITHOUT_NOTIFICATION;
import static de.caritas.cob.messageservice.testhelper.TestConstants.MESSAGE_DTO_WITH_NOTIFICATION;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.messageservice.api.exception.BadRequestException;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMessageException;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PostGroupMessageFacadeTest {

  private final String RC_TOKEN = "r94qMDk8gtgVNzqCq9zD2hELK-eXGB5VHlUVBgE8a8f";
  private final String RC_USER_ID = "pptLwARyTMzbTTRdg";
  private final String RC_GROUP_ID = "fR2Rz7dmWmHdXE8uz";
  private final String RC_FEEDBACK_GROUP_ID = "fR2Rz7dmWmHdXE8uz";
  private final String MESSAGE = "Lorem ipsum";
  private final String CHANNEL = "channel";
  private final PostMessageResponseDTO POST_MESSAGE_RESPONSE_DTO =
      new PostMessageResponseDTO(new Date(), CHANNEL, true, "", "");
  private final PostMessageResponseDTO POST_MESSAGE_RESPONSE_DTO_UNSUCCESSFUL =
      new PostMessageResponseDTO(new Date(), CHANNEL, false, "", "");

  @InjectMocks
  private PostGroupMessageFacade postGroupMessageFacade;
  @Mock
  private RocketChatService rocketChatService;
  @Mock
  private EmailNotificationFacade emailNotificationFacade;

  /**
   * Tests for method: postGroupMessage
   */
  @Test(expected = InternalServerErrorException.class)
  public void postGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatPostMessageFails()
      throws CustomCryptoException {

    RocketChatPostMessageException rocketChatPostMessageException =
        new RocketChatPostMessageException(new Exception());

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenThrow(rocketChatPostMessageException);

    postGroupMessageFacade.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE_DTO_WITH_NOTIFICATION);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatServiceReturnsEmptyDTO()
      throws CustomCryptoException {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(null);

    postGroupMessageFacade.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE_DTO_WITH_NOTIFICATION);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatServiceReturnsUnsuccessful()
      throws CustomCryptoException {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO_UNSUCCESSFUL);

    postGroupMessageFacade.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE_DTO_WITH_NOTIFICATION);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postGroupMessage_Should_ReturnRocketChatPostMarkGroupAsReadExceptionAndNotSendNotification_When_RocketChatMarkGroupAsReadFails()
      throws CustomCryptoException {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(null);

    postGroupMessageFacade.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE_DTO_WITH_NOTIFICATION);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_ReturnCreatedAndSendNotification_When_MessageWasSentAndNotificationIsSetToTrue()
      throws CustomCryptoException {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE_DTO_WITH_NOTIFICATION);

    verify(emailNotificationFacade, times(1)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_ReturnCreatedAndNotSendNotification_When_MessageWasSentAndNotificationIsSetToFalse()
      throws CustomCryptoException {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE_DTO_WITHOUT_NOTIFICATION);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(Mockito.anyString());
  }

  /**
   * Tests for method: postFeedbackGroupMessage
   */
  @Test(expected = InternalServerErrorException.class)
  public void postFeebackGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatPostMessageFails()
      throws CustomCryptoException {

    RocketChatPostMessageException rocketChatPostMessageException =
        new RocketChatPostMessageException(new Exception());

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    when(rocketChatService.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null))
        .thenThrow(rocketChatPostMessageException);

    postGroupMessageFacade.postFeedbackGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postFeedbackGroupMessage_Should_ReturnInternalServerErrorAndNotSendFeedbackNotification_When_RocketChatServiceReturnsEmptyDTO()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    when(rocketChatService.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null))
        .thenReturn(null);

    postGroupMessageFacade.postFeedbackGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    verify(emailNotificationFacade, times(0)).sendFeedbackEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postFeedbackGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_When_RocketChatServiceReturnsUnsuccessful()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO_UNSUCCESSFUL);

    postGroupMessageFacade.postFeedbackGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void postFeedbackGroupMessage_Should_ReturnRocketChatPostMarkGroupAsReadExceptionAndNotSendFeedbackNotification_When_RocketChatMarkGroupAsReadFails()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    when(rocketChatService.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null))
        .thenReturn(null);

    postGroupMessageFacade.postFeedbackGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    verify(emailNotificationFacade, times(0)).sendFeedbackEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test
  public void postFeedbackGroupMessage_Should_ReturnCreatedAndSendFeedbackNotification_When_RocketChatServiceSucceeds()
      throws CustomCryptoException {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO_FEEDBACK_CHAT);
    when(rocketChatService.postGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO);

    postGroupMessageFacade.postFeedbackGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    verify(emailNotificationFacade, times(1)).sendFeedbackEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test(expected = BadRequestException.class)
  public void postFeedbackGroupMessage_Should_ReturnBadRequestAndNotSendFeedbackNotification_When_GroupIdIsNoFeedbackChat() {

    when(rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_GROUP_ID))
        .thenReturn(GET_GROUP_INFO_DTO);

    postGroupMessageFacade.postFeedbackGroupMessage(
        RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null);

    verify(emailNotificationFacade, times(0)).sendFeedbackEmailNotification(RC_GROUP_ID);
  }
}

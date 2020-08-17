package de.caritas.cob.messageservice.api.facade;

import static de.caritas.cob.messageservice.testHelper.TestConstants.MESSAGE_DTO_WITHOUT_NOTIFICATION;
import static de.caritas.cob.messageservice.testHelper.TestConstants.MESSAGE_DTO_WITH_NOTIFICATION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMarkGroupAsReadException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMessageException;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.RocketChatService;

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
   * 
   * 
   * Tests for method: postGroupMessage
   *
   *
   */

  @Test
  public void postGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_WhenRocketChatPostMessageFails() {

    RocketChatPostMessageException rocketChatPostMessageException =
        new RocketChatPostMessageException(new Exception());

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenThrow(rocketChatPostMessageException);

    HttpStatus result = postGroupMessageFacade.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        MESSAGE_DTO_WITH_NOTIFICATION);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_WhenRocketChatServiceReturnsEmptyDTO() {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(null);

    HttpStatus result = postGroupMessageFacade.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        MESSAGE_DTO_WITH_NOTIFICATION);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_WhenRocketChatServiceReturnsUnsuccessful() {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO_UNSUCCESSFUL);

    HttpStatus result = postGroupMessageFacade.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        MESSAGE_DTO_WITH_NOTIFICATION);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_ReturnRocketChatPostMarkGroupAsReadExceptionAndNotSendNotification_WhenRocketChatMarkGroupAsReadFails() {

    RocketChatPostMarkGroupAsReadException ex =
        new RocketChatPostMarkGroupAsReadException(new Exception());

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(null);
    when(rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID)).thenThrow(ex);

    HttpStatus result = postGroupMessageFacade.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        MESSAGE_DTO_WITH_NOTIFICATION);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_ReturnCreatedAndSendNotification_When_MessageWasSentAndNotificationIsSetToTrue() {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO);

    HttpStatus result = postGroupMessageFacade.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        MESSAGE_DTO_WITH_NOTIFICATION);

    assertEquals(HttpStatus.CREATED, result);

    verify(emailNotificationFacade, times(1)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postGroupMessage_Should_ReturnCreatedAndNotSendNotification_When_MessageWasSentAndNotificationIsSetToFalse() {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO);

    HttpStatus result = postGroupMessageFacade.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        MESSAGE_DTO_WITHOUT_NOTIFICATION);

    assertEquals(HttpStatus.CREATED, result);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(Mockito.anyString());
  }

  /**
   * 
   * 
   * Tests for method: postFeedbackGroupMessage
   *
   *
   */

  @Test
  public void postFeebackGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_WhenRocketChatPostMessageFails() {

    RocketChatPostMessageException rocketChatPostMessageException =
        new RocketChatPostMessageException(new Exception());

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE,
        null)).thenThrow(rocketChatPostMessageException);

    HttpStatus result = postGroupMessageFacade.postFeedbackGroupMessage(RC_TOKEN, RC_USER_ID,
        RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_GROUP_ID);
  }

  @Test
  public void postFeedbackGroupMessage_Should_ReturnInternalServerErrorAndNotSendFeedbackNotification_WhenRocketChatServiceReturnsEmptyDTO() {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE,
        null)).thenReturn(null);

    HttpStatus result = postGroupMessageFacade.postFeedbackGroupMessage(RC_TOKEN, RC_USER_ID,
        RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result);

    verify(emailNotificationFacade, times(0)).sendFeedbackEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test
  public void postFeedbackGroupMessage_Should_ReturnInternalServerErrorAndNotSendNotification_WhenRocketChatServiceReturnsUnsuccessful() {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, MESSAGE, null))
        .thenReturn(POST_MESSAGE_RESPONSE_DTO_UNSUCCESSFUL);

    HttpStatus result = postGroupMessageFacade.postFeedbackGroupMessage(RC_TOKEN, RC_USER_ID,
        RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result);

    verify(emailNotificationFacade, times(0)).sendEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test
  public void postFeedbackGroupMessage_Should_ReturnRocketChatPostMarkGroupAsReadExceptionAndNotSendFeedbackNotification_WhenRocketChatMarkGroupAsReadFails() {

    RocketChatPostMarkGroupAsReadException ex =
        new RocketChatPostMarkGroupAsReadException(new Exception());

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE,
        null)).thenReturn(null);
    when(rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID)).thenThrow(ex);

    HttpStatus result = postGroupMessageFacade.postFeedbackGroupMessage(RC_TOKEN, RC_USER_ID,
        RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result);

    verify(emailNotificationFacade, times(0)).sendFeedbackEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

  @Test
  public void postFeedbackGroupMessage_Should_ReturnCreatedAndSendFeedbackNotification_WhenRocketChatServiceSucceeds() {

    when(rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_FEEDBACK_GROUP_ID, MESSAGE,
        null)).thenReturn(POST_MESSAGE_RESPONSE_DTO);

    HttpStatus result = postGroupMessageFacade.postFeedbackGroupMessage(RC_TOKEN, RC_USER_ID,
        RC_FEEDBACK_GROUP_ID, MESSAGE, null);

    assertEquals(HttpStatus.CREATED, result);

    verify(emailNotificationFacade, times(1)).sendFeedbackEmailNotification(RC_FEEDBACK_GROUP_ID);
  }

}

package de.caritas.cob.MessageService.api.facade;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import de.caritas.cob.MessageService.api.helper.EmailNotificationHelper;

@RunWith(MockitoJUnitRunner.class)
public class EmailNotificationFacadeTest {

  private final String FIELD_NAME_NEW_MESSAGE_NOTIFICATION =
      "userServiceApiSendNewMessageNotificationUrl";
  private final String NOTIFICATION_API_URL =
      "http://caritas.local/service/users/mails/messages/new";
  private final String FIELD_NAME_NEW_FEEDBACK_MESSAGE_NOTIFICATION =
      "userServiceApiSendNewFeedbackMessageNotificationUrl";
  private final String FEEDBACK_NOTIFICATION_API_URL =
      "http://caritas.local/service/users/mails/messages/feedback/new";
  private final String RC_GROUP_ID = "fR2Rz7dmWmHdXE8uz";

  @Mock
  private EmailNotificationHelper emailNotificationHelper;
  @InjectMocks
  private EmailNotificationFacade emailNotificationFacade;

  @Before
  public void setup() throws NoSuchFieldException, SecurityException {
    FieldSetter.setField(emailNotificationFacade,
        emailNotificationFacade.getClass().getDeclaredField(FIELD_NAME_NEW_MESSAGE_NOTIFICATION),
        NOTIFICATION_API_URL);
    FieldSetter.setField(emailNotificationFacade, emailNotificationFacade.getClass()
        .getDeclaredField(FIELD_NAME_NEW_FEEDBACK_MESSAGE_NOTIFICATION),
        FEEDBACK_NOTIFICATION_API_URL);
  }

  @Test
  public void sendEmailNotification_Should_PassUserServiceApiSendNewMessageNotificationUrlToEmailNotificationHelper() {

    emailNotificationFacade.sendEmailNotification(RC_GROUP_ID);

    verify(emailNotificationHelper, times(1)).sendEmailNotificationViaUserService(RC_GROUP_ID,
        NOTIFICATION_API_URL);

  }

  @Test
  public void sendFeedbackEmailNotification_Should_PassUserServiceApiSendNewFeedbackMessageNotificationUrlToEmailNotificationHelper() {

    emailNotificationFacade.sendFeedbackEmailNotification(RC_GROUP_ID);

    verify(emailNotificationHelper, times(1)).sendEmailNotificationViaUserService(RC_GROUP_ID,
        FEEDBACK_NOTIFICATION_API_URL);

  }

}

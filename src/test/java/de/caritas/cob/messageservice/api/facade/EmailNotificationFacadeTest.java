package de.caritas.cob.messageservice.api.facade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.api.model.ReassignStatus;
import de.caritas.cob.messageservice.api.service.helper.ServiceHelper;
import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
import de.caritas.cob.messageservice.userservice.generated.web.model.NewMessageNotificationDTO;
import de.caritas.cob.messageservice.userservice.generated.web.model.ReassignmentNotificationDTO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class EmailNotificationFacadeTest {

  private final String FIELD_NAME_NEW_MESSAGE_NOTIFICATION =
      "userServiceApiSendNewMessageNotificationUrl";
  private final String NOTIFICATION_API_URL =
      "http://caritas.local/service/users/mails/messages/new";
  private final String FIELD_NAME_NEW_FEEDBACK_MESSAGE_NOTIFICATION =
      "userServiceApiSendNewFeedbackMessageNotificationUrl";
  private final String FEEDBACK_NOTIFICATION_API_URL =
      "http://caritas.local/service/users/mails/messages/feedback/new";
  private final String RC_GROUP_ID = "fR2Rz7dmWmHdXE8uz";

  @InjectMocks
  private EmailNotificationFacade emailNotificationFacade;

  @Mock
  private ServiceHelper serviceHelper;

  @Mock
  private UserControllerApi userControllerApi;

  @Mock
  @SuppressWarnings("unused")
  private Environment environment;

  @BeforeEach
  public void setup() throws NoSuchFieldException, SecurityException {
    setField(emailNotificationFacade, "userServiceApiUrl", "http://localhost");
    when(serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders()).thenReturn(new HttpHeaders());
    when(userControllerApi.getApiClient()).thenReturn(mock(ApiClient.class));
  }

  @Test
  void sendEmailNotification_Should_sendExpectedNotificationMailViaUserService() {
    emailNotificationFacade.sendEmailAboutNewChatMessage(RC_GROUP_ID);

    var expectedMessage = new NewMessageNotificationDTO().rcGroupId(RC_GROUP_ID);
    verify(userControllerApi).sendNewMessageNotification(expectedMessage);
  }

  @Test
  void sendFeedbackEmailNotification_Should_sendExpectedFeedbackNotificationMailViaUserService() {
    emailNotificationFacade.sendEmailAboutNewFeedbackMessage(RC_GROUP_ID);

    var expectedMessage = new NewMessageNotificationDTO().rcGroupId(RC_GROUP_ID);
    verify(userControllerApi).sendNewFeedbackMessageNotification(expectedMessage);
  }

  @Test
  void sendEmailAboutReassignRequest_Should_sendExpectedReassignNotificationMailViaUserService() {
    var aliasArgs = new EasyRandom().nextObject(AliasArgs.class);

    emailNotificationFacade.sendEmailAboutReassignRequest(RC_GROUP_ID, aliasArgs);

    var expectedMessage = new ReassignmentNotificationDTO()
        .rcGroupId(RC_GROUP_ID)
        .fromConsultantName(aliasArgs.getFromConsultantName())
        .toConsultantId(aliasArgs.getToConsultantId())
        .isConfirmed(null);
    verify(userControllerApi).sendReassignmentNotification(expectedMessage);
  }

  @Test
  void sendEmailAboutReassignDecision_Should_sendExpectedReassignNotificationMailViaUserService() {
    var consultantReassignment = new EasyRandom().nextObject(ConsultantReassignment.class);

    emailNotificationFacade.sendEmailAboutReassignDecision(RC_GROUP_ID, consultantReassignment);

    var expectedMessage = new ReassignmentNotificationDTO()
        .rcGroupId(RC_GROUP_ID)
        .fromConsultantName(consultantReassignment.getFromConsultantName())
        .toConsultantId(consultantReassignment.getToConsultantId())
        .isConfirmed(consultantReassignment.getStatus() == ReassignStatus.CONFIRMED);
    verify(userControllerApi).sendReassignmentNotification(expectedMessage);
  }

}

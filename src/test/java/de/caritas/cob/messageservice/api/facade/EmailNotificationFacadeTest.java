package de.caritas.cob.messageservice.api.facade;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.api.model.ReassignStatus;
import de.caritas.cob.messageservice.api.service.helper.ServiceHelper;
import de.caritas.cob.messageservice.api.tenant.TenantContext;
import de.caritas.cob.messageservice.config.UserServiceApiControllerFactory;
import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
import de.caritas.cob.messageservice.userservice.generated.web.model.NewMessageNotificationDTO;
import de.caritas.cob.messageservice.userservice.generated.web.model.ReassignmentNotificationDTO;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailNotificationFacadeTest {

  private final String RC_GROUP_ID = "fR2Rz7dmWmHdXE8uz";

  @InjectMocks
  private EmailNotificationFacade emailNotificationFacade;

  @Mock
  private ServiceHelper serviceHelper;

  @Mock
  private UserControllerApi userControllerApi;

  @Mock
  private UserServiceApiControllerFactory userServiceApiControllerFactory;

  @Test
  void sendEmailNotification_Should_sendExpectedNotificationMailViaUserService() {
    // given
    givenApiClientAndHeadersAreConfigured();
    // when
    sendEmailNotification();
    // then
    var expectedMessage = new NewMessageNotificationDTO().rcGroupId(RC_GROUP_ID);
    verify(userControllerApi).sendNewMessageNotification(expectedMessage);
  }

  private void givenApiClientAndHeadersAreConfigured() {
    when(userServiceApiControllerFactory.createControllerApi()).thenReturn(userControllerApi);
    when(serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders(any(), any()))
        .thenReturn(new HttpHeaders());
    when(userControllerApi.getApiClient()).thenReturn(mock(ApiClient.class));
  }

  @Test
  void sendEmailAboutNewChatMessage_Should_sendExpectedNotificationMailViaUserServiceAndSetTenantContextFromACallingServiceForMultitenancyEnabled() {
    // given
    givenApiClientAndHeadersAreConfigured();
    TenantContext.clear();
    ReflectionTestUtils.setField(emailNotificationFacade, "multitenancy", true);
    // when
    emailNotificationFacade.sendEmailAboutNewChatMessage(RC_GROUP_ID, Optional.of(1L), null);

    // then
    var expectedMessage = new NewMessageNotificationDTO().rcGroupId(RC_GROUP_ID);
    assertThat(TenantContext.getCurrentTenant()).isEqualTo(1L);
    verify(userControllerApi).sendNewMessageNotification(expectedMessage);

    // clean up
    TenantContext.clear();
    ReflectionTestUtils.setField(emailNotificationFacade, "multitenancy", false);
  }

  @Test
  void sendEmailAboutNewChatMessage_Should_throwExceptionIfMultitenancyEnabledButNoTenantContextSet() {
    // given
    TenantContext.clear();
    ReflectionTestUtils.setField(emailNotificationFacade, "multitenancy", true);
    // when

    // when/ then
    assertThrows(NoSuchElementException.class, () -> {
      sendEmailNotification();
    });

    // clean up
    TenantContext.clear();
    ReflectionTestUtils.setField(emailNotificationFacade, "multitenancy", false);
  }

  private void sendEmailNotification() {
    emailNotificationFacade.sendEmailAboutNewChatMessage(RC_GROUP_ID, Optional.empty(), null);
  }

  @Test
  void sendFeedbackEmailNotification_Should_sendExpectedFeedbackNotificationMailViaUserService() {
    givenApiClientAndHeadersAreConfigured();
    // when
    emailNotificationFacade.sendEmailAboutNewFeedbackMessage(RC_GROUP_ID, Optional.empty(), null);

    // then
    var expectedMessage = new NewMessageNotificationDTO().rcGroupId(RC_GROUP_ID);
    verify(userControllerApi).sendNewFeedbackMessageNotification(expectedMessage);
  }

  @Test
  void sendEmailAboutReassignRequest_Should_sendExpectedReassignNotificationMailViaUserService() {
    // given
    givenApiClientAndHeadersAreConfigured();
    var aliasArgs = new EasyRandom().nextObject(AliasArgs.class);

    // when
    emailNotificationFacade.sendEmailAboutReassignRequest(RC_GROUP_ID, aliasArgs, Optional.empty(),
        null);

    // then
    var expectedMessage = new ReassignmentNotificationDTO()
        .rcGroupId(RC_GROUP_ID)
        .fromConsultantName(aliasArgs.getFromConsultantName())
        .toConsultantId(aliasArgs.getToConsultantId())
        .isConfirmed(null);
    verify(userControllerApi).sendReassignmentNotification(expectedMessage);
  }

  @Test
  void sendEmailAboutReassignDecision_Should_sendExpectedReassignNotificationMailViaUserService() {
    // given
    givenApiClientAndHeadersAreConfigured();
    var consultantReassignment = new EasyRandom().nextObject(ConsultantReassignment.class);

    // when
    emailNotificationFacade.sendEmailAboutReassignDecision(RC_GROUP_ID, consultantReassignment,
        Optional.empty(), null);

    // then
    var expectedMessage = new ReassignmentNotificationDTO()
        .rcGroupId(RC_GROUP_ID)
        .fromConsultantName(consultantReassignment.getFromConsultantName())
        .toConsultantId(consultantReassignment.getToConsultantId())
        .isConfirmed(consultantReassignment.getStatus() == ReassignStatus.CONFIRMED);
    verify(userControllerApi).sendReassignmentNotification(expectedMessage);
  }
}

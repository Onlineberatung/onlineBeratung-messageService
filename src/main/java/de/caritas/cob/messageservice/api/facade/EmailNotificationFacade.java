package de.caritas.cob.messageservice.api.facade;

import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.api.model.ReassignStatus;
import de.caritas.cob.messageservice.api.service.helper.ServiceHelper;
import de.caritas.cob.messageservice.api.tenant.TenantContext;
import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
import de.caritas.cob.messageservice.userservice.generated.web.model.NewMessageNotificationDTO;
import de.caritas.cob.messageservice.userservice.generated.web.model.ReassignmentNotificationDTO;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/*
 * Facade to encapsulate the steps for sending an email notification
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationFacade {

  private final @NonNull ServiceHelper serviceHelper;
  private final @NonNull UserControllerApi userControllerApi;
  private final @NonNull Environment environment;

  @Value("${multitenancy.enabled}")
  private boolean multitenancy;

  @Value("${user.service.api.liveproxy.url}")
  private String userServiceApiUrl;

  @EventListener(ApplicationReadyEvent.class)
  public void setBasePath() {
    if (!Set.of(environment.getActiveProfiles()).contains("testing")) {
      userControllerApi.getApiClient().setBasePath(userServiceApiUrl);
    }
  }

  /**
   * Sends a new message notification via the UserService (user data needed for sending the mail
   * will be read by the UserService, which in turn calls the MessageService).
   *
   * @param rcGroupId - Rocket.Chat group id
   */
  @Async
  public void sendEmailAboutNewChatMessage(String rcGroupId, Optional<Long> tenantId) {
    if (multitenancy) {
      TenantContext.setCurrentTenant(tenantId.orElseThrow());
    }
    addDefaultHeaders(userControllerApi.getApiClient());
    userControllerApi
        .sendNewMessageNotification(new NewMessageNotificationDTO().rcGroupId(rcGroupId));
  }

  private void addDefaultHeaders(ApiClient apiClient) {
    var headers = this.serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders();
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }

  /**
   * Sends a new feedback message notification via the UserService (user data needed for sending the
   * mail will be read by the UserService, which in turn calls the MessageService).
   *
   * @param rcGroupId - Rocket.Chat group id
   */
  public void sendEmailAboutNewFeedbackMessage(String rcGroupId) {
    addDefaultHeaders(userControllerApi.getApiClient());
    userControllerApi
        .sendNewFeedbackMessageNotification(new NewMessageNotificationDTO().rcGroupId(rcGroupId));
  }

  public void sendEmailAboutReassignRequest(String rcGroupId, AliasArgs aliasArgs) {
    var reassignmentNotification = new ReassignmentNotificationDTO()
        .rcGroupId(rcGroupId)
        .toConsultantId(aliasArgs.getToConsultantId())
        .fromConsultantName(aliasArgs.getFromConsultantName());
    addDefaultHeaders(userControllerApi.getApiClient());
    userControllerApi.sendReassignmentNotification(reassignmentNotification);
  }

  public void sendEmailAboutReassignDecision(String roomId,
      ConsultantReassignment consultantReassignment) {
    var reassignmentNotification = new ReassignmentNotificationDTO()
        .rcGroupId(roomId)
        .toConsultantId(consultantReassignment.getToConsultantId())
        .fromConsultantName(consultantReassignment.getFromConsultantName())
        .isConfirmed(consultantReassignment.getStatus() == ReassignStatus.CONFIRMED);
    addDefaultHeaders(userControllerApi.getApiClient());
    userControllerApi.sendReassignmentNotification(reassignmentNotification);
  }
}

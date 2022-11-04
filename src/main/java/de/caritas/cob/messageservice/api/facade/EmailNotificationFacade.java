package de.caritas.cob.messageservice.api.facade;

import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.api.model.ReassignStatus;
import de.caritas.cob.messageservice.api.service.helper.ServiceHelper;
import de.caritas.cob.messageservice.api.tenant.TenantContext;
import de.caritas.cob.messageservice.config.apiclient.UserServiceApiControllerFactory;
import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import de.caritas.cob.messageservice.userservice.generated.web.model.NewMessageNotificationDTO;
import de.caritas.cob.messageservice.userservice.generated.web.model.ReassignmentNotificationDTO;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/*
 * Facade to encapsulate the steps for sending an email notification
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationFacade {

  private final @NonNull ServiceHelper serviceHelper;

  private final @NonNull UserServiceApiControllerFactory clientFactory;

  @Value("${multitenancy.enabled}")
  private boolean multitenancy;

  /**
   * Sends a new message notification via the UserService (user data needed for sending the mail
   * will be read by the UserService, which in turn calls the MessageService).
   *
   * @param rcGroupId - Rocket.Chat group id
   */
  @Async
  public void sendEmailAboutNewChatMessage(String rcGroupId, Optional<Long> tenantId,
      String accessToken) {
    if (multitenancy) {
      TenantContext.setCurrentTenant(tenantId.orElseThrow());
    }

    var userControllerApi = clientFactory.userControllerApi();
    addDefaultHeaders(userControllerApi.getApiClient(), accessToken, tenantId);
    userControllerApi
        .sendNewMessageNotification(new NewMessageNotificationDTO().rcGroupId(rcGroupId));
  }

  private void addDefaultHeaders(ApiClient apiClient, String accessToken, Optional<Long> tenantId) {
    var headers = serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders(accessToken, tenantId);
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }

  /**
   * Sends a new feedback message notification via the UserService (user data needed for sending the
   * mail will be read by the UserService, which in turn calls the MessageService).
   *
   * @param rcGroupId - Rocket.Chat group id
   */
  @Async
  public void sendEmailAboutNewFeedbackMessage(String rcGroupId, Optional<Long> tenantId,
      String accessToken) {

    var userControllerApi = clientFactory.userControllerApi();
    addDefaultHeaders(userControllerApi.getApiClient(), accessToken, tenantId);
    userControllerApi
        .sendNewFeedbackMessageNotification(new NewMessageNotificationDTO().rcGroupId(rcGroupId));
  }

  @Async
  public void sendEmailAboutReassignRequest(String rcGroupId, AliasArgs aliasArgs,
      Optional<Long> tenantId, String accessToken) {
    var reassignmentNotification = new ReassignmentNotificationDTO()
        .rcGroupId(rcGroupId)
        .toConsultantId(aliasArgs.getToConsultantId())
        .fromConsultantName(aliasArgs.getFromConsultantName());

    var userControllerApi = clientFactory.userControllerApi();
    addDefaultHeaders(userControllerApi.getApiClient(), accessToken, tenantId);
    userControllerApi.sendReassignmentNotification(reassignmentNotification);
  }

  @Async
  public void sendEmailAboutReassignDecision(String roomId,
      ConsultantReassignment consultantReassignment, Optional<Long> tenantId, String accessToken) {
    var reassignmentNotification = new ReassignmentNotificationDTO()
        .rcGroupId(roomId)
        .toConsultantId(consultantReassignment.getToConsultantId())
        .fromConsultantName(consultantReassignment.getFromConsultantName())
        .isConfirmed(consultantReassignment.getStatus() == ReassignStatus.CONFIRMED);

    var userControllerApi = clientFactory.userControllerApi();
    addDefaultHeaders(userControllerApi.getApiClient(), accessToken, tenantId);
    userControllerApi.sendReassignmentNotification(reassignmentNotification);
  }
}

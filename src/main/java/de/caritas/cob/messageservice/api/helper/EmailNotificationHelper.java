package de.caritas.cob.messageservice.api.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import de.caritas.cob.messageservice.api.model.NewMessageNotificationDTO;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.api.service.helper.ServiceHelper;

/*
 * Helper for sending an email notification via the MailService
 */
@Component
public class EmailNotificationHelper {

  private final RestTemplate restTemplate;
  private final ServiceHelper serviceHelper;

  @Autowired
  public EmailNotificationHelper(RestTemplate restTemplate, ServiceHelper serviceHelper) {
    this.restTemplate = restTemplate;
    this.serviceHelper = serviceHelper;
  }

  /**
   * Send an email via the UserService
   * 
   * @param rcGroupId
   * @param userServiceApiSendNewMessageNotificationUrl
   */
  public void sendEmailNotificationViaUserService(String rcGroupId,
      String userServiceApiSendNewMessageNotificationUrl) {

    try {
      HttpHeaders headers = serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders();
      NewMessageNotificationDTO notificationDTO = new NewMessageNotificationDTO(rcGroupId);
      HttpEntity<NewMessageNotificationDTO> request =
          new HttpEntity<>(notificationDTO, headers);

      restTemplate.exchange(userServiceApiSendNewMessageNotificationUrl, HttpMethod.POST, request,
          Void.class);

    } catch (RestClientException ex) {
      LogService.logUserServiceHelperError(ex);
    }

  }

}

package de.caritas.cob.messageservice.api.facade;

import de.caritas.cob.messageservice.api.helper.EmailNotificationHelper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
 * Facade to encapsulate the steps for sending an email notification
 */
@Component
public class EmailNotificationFacade {

  @Value("${user.service.api.new.message.notification}")
  private String userServiceApiSendNewMessageNotificationUrl;

  @Value("${user.service.api.new.feedback.message.notification}")
  private String userServiceApiSendNewFeedbackMessageNotificationUrl;

  private final EmailNotificationHelper emailNotificationHelper;

  @Autowired
  public EmailNotificationFacade(EmailNotificationHelper emailNotificationHelper) {
    this.emailNotificationHelper = emailNotificationHelper;
  }

  /**
   * Sends a new message notification via the UserService (user data needed for sending the mail
   * will be read by the UserService, which in turn calls the MessageService).
   *
   * @param rcGroupId - Rocket.Chat group id
   */
  public void sendEmailAboutNewChatMessage(String rcGroupId) {
    emailNotificationHelper.sendEmailNotificationViaUserService(rcGroupId,
        userServiceApiSendNewMessageNotificationUrl);
  }

  /**
   * Sends a new feedback message notification via the UserService (user data needed for sending the
   * mail will be read by the UserService, which in turn calls the MessageService).
   *
   * @param rcGroupId - Rocket.Chat group id
   */
  public void sendEmailAboutNewFeedbackMessage(String rcGroupId) {
    emailNotificationHelper.sendEmailNotificationViaUserService(rcGroupId,
        userServiceApiSendNewFeedbackMessageNotificationUrl);
  }

  @SuppressWarnings("unused")
  public void sendEmailAboutReassignRequest(String rcGroupId, String toConsultantId) {
    // will call user service
  }

  @SuppressWarnings("unused")
  public void sendEmailAboutReassignDecision(String roomId, UUID toConsultantId) {
    // will call user service
  }
}

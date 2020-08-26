package de.caritas.cob.messageservice.api.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import de.caritas.cob.messageservice.api.helper.EmailNotificationHelper;

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
   * @param rcGroupId
   */

  public void sendEmailNotification(String rcGroupId) {
    emailNotificationHelper.sendEmailNotificationViaUserService(rcGroupId,
        userServiceApiSendNewMessageNotificationUrl);
  }

  /**
   * Sends a new feedback message notification via the UserService (user data needed for sending the
   * mail will be read by the UserService, which in turn calls the MessageService).
   * 
   * @param rcGroupId
   */

  public void sendFeedbackEmailNotification(String rcGroupId) {
    emailNotificationHelper.sendEmailNotificationViaUserService(rcGroupId,
        userServiceApiSendNewFeedbackMessageNotificationUrl);
  }

}

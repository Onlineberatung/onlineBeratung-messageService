package de.caritas.cob.MessageService.api.service.helper;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import de.caritas.cob.MessageService.api.helper.EmailNotificationHelper;
import de.caritas.cob.MessageService.api.model.NewMessageNotificationDTO;
import de.caritas.cob.MessageService.api.service.LogService;

@RunWith(MockitoJUnitRunner.class)
public class EmailNotificationHelperTest {

  private final String RC_GROUP_ID = "fR2Rz7dmWmHdXE8uz";
  private final String USER_SERVICE_API_SEND_NEW_MESSAGE_NOTIFICATION_URL =
      "http://caritas.local/service/user/mails/new";
  private final String ERROR = "error";

  @Mock
  private RestTemplate restTemplate;
  @Mock
  private ServiceHelper serviceHelper;
  @Mock
  private LogService logService;
  @InjectMocks
  private EmailNotificationHelper emailNotificationHelper;

  @Test
  public void sendEmailNotificationViaUserService_Should_LogException_OnError()
      throws RestClientException {

    RestClientException exception = new RestClientException(ERROR);

    when(restTemplate.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(),
        ArgumentMatchers.<HttpEntity<?>>any(),
        ArgumentMatchers.<Class<NewMessageNotificationDTO>>any())).thenThrow(exception);

    emailNotificationHelper.sendEmailNotificationViaUserService(RC_GROUP_ID,
        USER_SERVICE_API_SEND_NEW_MESSAGE_NOTIFICATION_URL);

    verify(logService, times(1)).logUserServiceHelperError(Mockito.any());

  }

  @Test
  public void sendEmailNotificationViaUserService_Should_CallUserServiceWithGiveUrl() {

    emailNotificationHelper.sendEmailNotificationViaUserService(RC_GROUP_ID,
        USER_SERVICE_API_SEND_NEW_MESSAGE_NOTIFICATION_URL);

    verify(restTemplate, times(1)).exchange(
        Mockito.eq(USER_SERVICE_API_SEND_NEW_MESSAGE_NOTIFICATION_URL), Mockito.eq(HttpMethod.POST),
        Mockito.any(), Mockito.eq(Void.class));

  }

}

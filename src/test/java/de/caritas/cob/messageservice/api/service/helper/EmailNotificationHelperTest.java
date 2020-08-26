package de.caritas.cob.messageservice.api.service.helper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;

import de.caritas.cob.messageservice.api.helper.EmailNotificationHelper;
import de.caritas.cob.messageservice.api.model.NewMessageNotificationDTO;
import de.caritas.cob.messageservice.api.service.LogService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
  private Logger logger;
  @InjectMocks
  private EmailNotificationHelper emailNotificationHelper;

  @Before
  public void setup() {
    setInternalState(LogService.class, "LOGGER", logger);
  }

  @Test
  public void sendEmailNotificationViaUserService_Should_LogException_OnError()
      throws RestClientException {

    RestClientException exception = new RestClientException(ERROR);

    when(restTemplate.exchange(ArgumentMatchers.anyString(), any(),
        ArgumentMatchers.<HttpEntity<?>>any(),
        ArgumentMatchers.<Class<NewMessageNotificationDTO>>any())).thenThrow(exception);

    emailNotificationHelper.sendEmailNotificationViaUserService(RC_GROUP_ID,
        USER_SERVICE_API_SEND_NEW_MESSAGE_NOTIFICATION_URL);

    verify(logger, times(1)).error(anyString(), anyString());

  }

  @Test
  public void sendEmailNotificationViaUserService_Should_CallUserServiceWithGiveUrl() {

    emailNotificationHelper.sendEmailNotificationViaUserService(RC_GROUP_ID,
        USER_SERVICE_API_SEND_NEW_MESSAGE_NOTIFICATION_URL);

    verify(restTemplate, times(1)).exchange(
        Mockito.eq(USER_SERVICE_API_SEND_NEW_MESSAGE_NOTIFICATION_URL), Mockito.eq(HttpMethod.POST),
        any(), Mockito.eq(Void.class));

  }

}

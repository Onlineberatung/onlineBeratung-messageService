package de.caritas.cob.messageservice.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import de.caritas.cob.messageservice.api.service.helper.ServiceHelper;
import de.caritas.cob.messageservice.userservice.generated.web.LiveproxyControllerApi;
import java.lang.management.ManagementFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("testing")
@DirtiesContext
class LiveEventNotificationServiceIT {

  @Autowired
  private LiveEventNotificationService underTest;

  @MockBean
  @Qualifier("liveproxyControllerApi")
  @SuppressWarnings("unused")
  private LiveproxyControllerApi liveproxyControllerApi;

  @MockBean
  private ServiceHelper serviceHelper;

  @Test
  void sendLiveEventShouldRunInAnotherThread() {
    when(serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders()).thenReturn(new HttpHeaders());
    var threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

    underTest.sendLiveEvent(RandomStringUtils.randomAlphanumeric(16));

    assertEquals(threadCount + 1, ManagementFactory.getThreadMXBean().getThreadCount());
  }
}

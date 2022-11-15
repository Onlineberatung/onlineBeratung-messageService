package de.caritas.cob.messageservice.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import de.caritas.cob.messageservice.userservice.generated.web.LiveproxyControllerApi;
import java.lang.management.ManagementFactory;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("testing")
@TestPropertySource(properties = "multitenancy.enabled=true")
@DirtiesContext
class LiveEventNotificationServiceIT {

  private final EasyRandom easyRandom = new EasyRandom();

  @Autowired
  private LiveEventNotificationService underTest;

  @MockBean
  @SuppressWarnings("unused")
  private LiveproxyControllerApi liveproxyControllerApi;

  @MockBean
  private AuthenticatedUser authenticatedUser;

  @MockBean
  private ApiClient apiClient;

  @Test
  void sendLiveEventShouldRunInAnotherThread() {
    when(liveproxyControllerApi.getApiClient()).thenReturn(apiClient);
    var threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

    underTest.sendLiveEvent(
        RandomStringUtils.randomAlphanumeric(16),
        RandomStringUtils.randomAlphanumeric(16),
        Optional.of(easyRandom.nextLong())
    );

    assertEquals(threadCount + 1, ManagementFactory.getThreadMXBean().getThreadCount());
  }

  @Test
  void sendLiveEventShouldNeverCallAuthenticatedUserMethodsWhenAccessTokenGiven() {
    when(liveproxyControllerApi.getApiClient()).thenReturn(apiClient);

    underTest.sendLiveEvent(
        RandomStringUtils.randomAlphanumeric(16),
        RandomStringUtils.randomAlphanumeric(16),
        Optional.of(easyRandom.nextLong())
    );

    verify(authenticatedUser, timeout(1000).times(0))
        .getAccessToken();
  }

  @Test
  void sendLiveEventShouldCallAuthenticatedUserMethodsWhenAccessTokenMissing() {
    when(liveproxyControllerApi.getApiClient()).thenReturn(apiClient);

    underTest.sendLiveEvent(
        RandomStringUtils.randomAlphanumeric(16),
        null,
        Optional.of(easyRandom.nextLong())
    );

    verify(authenticatedUser, timeout(1000)).getAccessToken();
  }
}

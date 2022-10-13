package de.caritas.cob.messageservice.api.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
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
class EmailNotificationFacadeAsyncIT {

  private final EasyRandom easyRandom = new EasyRandom();

  @Autowired
  private EmailNotificationFacade underTest;

  @MockBean
  @SuppressWarnings("unused")
  private UserControllerApi userControllerApi;

  @MockBean
  private ApiClient apiClient;

  @Test
  void sendEmailAboutNewChatMessageShouldRunInAnotherThread() {
    when(userControllerApi.getApiClient()).thenReturn(apiClient);
    var threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

    underTest.sendEmailAboutNewChatMessage(
        RandomStringUtils.randomAlphanumeric(16),
        Optional.of(easyRandom.nextLong()),
        null
    );

    assertEquals(threadCount + 1, ManagementFactory.getThreadMXBean().getThreadCount());
  }

  @Test
  void sendEmailAboutNewFeedbackMessageShouldRunInAnotherThread() {
    when(userControllerApi.getApiClient()).thenReturn(apiClient);
    var threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

    underTest.sendEmailAboutNewFeedbackMessage(
        RandomStringUtils.randomAlphanumeric(16),
        Optional.of(easyRandom.nextLong()),
        null
    );

    assertEquals(threadCount + 1, ManagementFactory.getThreadMXBean().getThreadCount());
  }

  @Test
  void sendEmailAboutReassignRequestShouldRunInAnotherThread() {
    when(userControllerApi.getApiClient()).thenReturn(apiClient);
    var threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

    underTest.sendEmailAboutReassignRequest(
        RandomStringUtils.randomAlphanumeric(16),
        easyRandom.nextObject(AliasArgs.class),
        Optional.of(easyRandom.nextLong()),
        null
    );

    assertEquals(threadCount + 1, ManagementFactory.getThreadMXBean().getThreadCount());
  }

  @Test
  void sendEmailAboutReassignDecisionShouldRunInAnotherThread() {
    when(userControllerApi.getApiClient()).thenReturn(apiClient);
    var threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

    underTest.sendEmailAboutReassignDecision(
        RandomStringUtils.randomAlphanumeric(16),
        easyRandom.nextObject(ConsultantReassignment.class),
        Optional.of(easyRandom.nextLong()),
        null
    );

    assertEquals(threadCount + 1, ManagementFactory.getThreadMXBean().getThreadCount());
  }
}

package de.caritas.cob.messageservice.api.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
import java.lang.management.ManagementFactory;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Disabled;
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
class EmailNotificationFacadeIT {

  @Autowired
  private EmailNotificationFacade underTest;

  @MockBean
  @SuppressWarnings("unused")
  private UserControllerApi userControllerApi;

  @Test
  @Disabled("temporarily disabling it, as @Async does not work, as AuthenticatedUser request scope does not exist ")
  void sendEmailAboutNewChatMessageShouldRunInAnotherThread() {
    var threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

    underTest.sendEmailAboutNewChatMessage(RandomStringUtils.randomAlphanumeric(16), Optional.empty());

    assertEquals(threadCount + 1, ManagementFactory.getThreadMXBean().getThreadCount());
  }
}

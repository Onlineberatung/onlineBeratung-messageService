package de.caritas.cob.messageservice.api.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
import java.lang.management.ManagementFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("testing")
@DirtiesContext
class EmailNotificationFacadeIT {

  @Autowired
  private EmailNotificationFacade underTest;

  @MockBean
  @SuppressWarnings("unused")
  private UserControllerApi userControllerApi;

  @Test
  void sendEmailAboutNewChatMessageShouldRunInAnotherThread() {
    var threadCount = ManagementFactory.getThreadMXBean().getThreadCount();

    underTest.sendEmailAboutNewChatMessage(RandomStringUtils.randomAlphanumeric(16));

    assertEquals(threadCount + 1, ManagementFactory.getThreadMXBean().getThreadCount());
  }
}

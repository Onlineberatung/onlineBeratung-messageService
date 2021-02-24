package de.caritas.cob.messageservice.api.service;

import static com.anarsoft.vmlens.concurrent.junit.TestUtil.runMultithreaded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.caritas.cob.messageservice.MessageServiceApplication;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MessageServiceApplication.class)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = "spring.profiles.active=testing")
public class DraftMessageServiceIT {

  @Autowired
  private DraftMessageService draftMessageService;

  @MockBean
  private AuthenticatedUser authenticatedUser;

  @MockBean
  private EncryptionService encryptionService;

  @Before
  public void setup() throws CustomCryptoException {
    when(this.encryptionService.encrypt(any(), any())).thenReturn("encrypted");
    when(this.authenticatedUser.getUserId()).thenReturn("userId");
  }

  @Test
  public void saveAndDeleteDraftMessage_Should_produceNoError_When_executionIsInParallel()
      throws InterruptedException {
    AtomicInteger errorCount = new AtomicInteger(0);
    int threadCount = 10;
    String rcGroupId = "rcGroupId";

    runMultithreaded(() -> {
      try {
        draftMessageService.saveDraftMessage("message", rcGroupId);
        draftMessageService.deleteDraftMessageIfExist(rcGroupId);
      } catch (Exception e) {
        errorCount.incrementAndGet();
      }
    }, threadCount);

    assertThat(errorCount.get(), is(0));
  }

}

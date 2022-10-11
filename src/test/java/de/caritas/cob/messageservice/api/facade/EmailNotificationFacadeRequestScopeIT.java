package de.caritas.cob.messageservice.api.facade;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.model.AliasArgs;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import de.caritas.cob.messageservice.userservice.generated.web.UserControllerApi;
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
class EmailNotificationFacadeRequestScopeIT {

  private final EasyRandom easyRandom = new EasyRandom();

  @Autowired
  private EmailNotificationFacade underTest;

  @MockBean
  @SuppressWarnings("unused")
  private UserControllerApi userControllerApi;

  @MockBean
  private AuthenticatedUser authenticatedUser;

  @MockBean
  private ApiClient apiClient;

  @Test
  void sendEmailAboutNewChatMessageShouldCallAuthenticatedUserMethodsWhenAccessTokenMissing() {
    when(userControllerApi.getApiClient()).thenReturn(apiClient);

    underTest.sendEmailAboutNewChatMessage(
        RandomStringUtils.randomAlphanumeric(16),
        Optional.of(easyRandom.nextLong()),
        null
    );

    verify(authenticatedUser, timeout(1000)).getAccessToken();
  }

  @Test
  void sendEmailAboutNewFeedbackMessageShouldCallAuthenticatedUserMethodsWhenAccessTokenMissing() {
    when(userControllerApi.getApiClient()).thenReturn(apiClient);

    underTest.sendEmailAboutNewFeedbackMessage(
        RandomStringUtils.randomAlphanumeric(16),
        Optional.of(easyRandom.nextLong()),
        null
    );

    verify(authenticatedUser, timeout(1000)).getAccessToken();
  }

  @Test
  void sendEmailAboutReassignRequestShouldCallAuthenticatedUserMethodsWhenAccessTokenMissing() {
    when(userControllerApi.getApiClient()).thenReturn(apiClient);

    underTest.sendEmailAboutReassignRequest(
        RandomStringUtils.randomAlphanumeric(16),
        easyRandom.nextObject(AliasArgs.class),
        Optional.of(easyRandom.nextLong()),
        null
    );

    verify(authenticatedUser, timeout(1000)).getAccessToken();
  }

  @Test
  void sendEmailAboutReassignDecisionShouldCallAuthenticatedUserMethodsWhenAccessTokenMissing() {
    when(userControllerApi.getApiClient()).thenReturn(apiClient);

    underTest.sendEmailAboutReassignDecision(
        RandomStringUtils.randomAlphanumeric(16),
        easyRandom.nextObject(ConsultantReassignment.class),
        Optional.of(easyRandom.nextLong()),
        null
    );

    verify(authenticatedUser, timeout(1000)).getAccessToken();
  }
}

package de.caritas.cob.messageservice.api.service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import de.caritas.cob.messageservice.api.service.helper.ServiceHelper;
import de.caritas.cob.messageservice.config.apiclient.ApiControllerFactory;
import de.caritas.cob.messageservice.userservice.generated.ApiClient;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Service class to provide live event triggers to the live proxy endpoint in user service.
 */
@Service
@RequiredArgsConstructor
public class LiveEventNotificationService {

  private final @NonNull ApiControllerFactory clientFactory;
  private final @NonNull ServiceHelper serviceHelper;

  /**
   * Triggers a live event to proxy endpoint of user service.
   *
   * @param rcGroupId the rocket chat group id
   */
  @Async
  public void sendLiveEvent(String rcGroupId, String accessToken, Optional<Long> tenantId) {
    var liveProxyControllerApi = clientFactory.liveproxyControllerApi();
    if (isNotBlank(rcGroupId)) {
      addDefaultHeaders(liveProxyControllerApi.getApiClient(), accessToken, tenantId);
      try {
        liveProxyControllerApi.sendLiveEvent(rcGroupId);
      } catch (RestClientException e) {
        LogService.logInternalServerError(
            String.format("Unable to trigger live event for rc group id %s", rcGroupId), e);
      }
    }
  }

  private void addDefaultHeaders(ApiClient apiClient, String accessToken, Optional<Long> tenantId) {
    var headers = serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders(accessToken, tenantId);
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }

}

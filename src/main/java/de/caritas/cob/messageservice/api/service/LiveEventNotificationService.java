package de.caritas.cob.messageservice.api.service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import de.caritas.cob.messageservice.userservice.generated.web.LiveproxyControllerApi;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service class to provide live event triggers to the live proxy endpoint in user service.
 */
@Service
@RequiredArgsConstructor
public class LiveEventNotificationService {

  private final @NonNull LiveproxyControllerApi liveproxyControllerApi;

  /**
   * Triggers a live event to proxy endpoint of user service.
   *
   * @param rcGroupId the rocket chat group id
   */
  public void sendLiveEvent(String rcGroupId) {
    if (isNotBlank(rcGroupId)) {
      this.liveproxyControllerApi.sendLiveEvent(rcGroupId);
    }
  }

}

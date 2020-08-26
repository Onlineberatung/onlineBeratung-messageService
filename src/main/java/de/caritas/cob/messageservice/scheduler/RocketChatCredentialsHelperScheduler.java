package de.caritas.cob.messageservice.scheduler;

import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.api.service.helper.RocketChatCredentialsHelper;
import javax.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!testing")
public class RocketChatCredentialsHelperScheduler {

  private final @NonNull RocketChatCredentialsHelper rcCredentialsHelper;

  @PostConstruct
  public void postConstructInitializer() {
    LogService.logDebug("RocketChatCredentialsHelperScheduler - initialize tokens");
    rcCredentialsHelper.updateCredentials();
  }

  @Scheduled(cron = "${rocket.credentialscheduler.cron}")
  public void scheduledRotateToken() {
    LogService.logDebug("RocketChatCredentialsHelperScheduler - rotating tokens");
    rcCredentialsHelper.updateCredentials();
  }

}

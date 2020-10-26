package de.caritas.cob.messageservice.api.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import de.caritas.cob.messageservice.userservice.generated.web.LiveproxyControllerApi;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LiveEventNotificationServiceTest {

  @InjectMocks
  private LiveEventNotificationService liveEventNotificationService;

  @Mock
  private LiveproxyControllerApi liveproxyControllerApi;

  @Test
  public void sendLiveEvent_Should_notTriggerLiveEvent_When_rcGroupIdIsNull() {
    this.liveEventNotificationService.sendLiveEvent(null);

    verifyZeroInteractions(this.liveproxyControllerApi);
  }

  @Test
  public void sendLiveEvent_Should_notTriggerLiveEvent_When_rcGroupIdIsEmpty() {
    this.liveEventNotificationService.sendLiveEvent("");

    verifyZeroInteractions(this.liveproxyControllerApi);
  }

  @Test
  public void sendLiveEvent_Should_triggerLiveEvent_When_rcGroupIdIsValid() {
    this.liveEventNotificationService.sendLiveEvent("valid");

    verify(this.liveproxyControllerApi, times(1)).sendLiveEvent(eq("valid"));
  }

}

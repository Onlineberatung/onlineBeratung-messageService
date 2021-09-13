package de.caritas.cob.messageservice.api.service.statistics.event;

import de.caritas.cob.messageservice.api.helper.CustomLocalDateTime;
import de.caritas.cob.messageservice.api.helper.JSONHelper;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.EventType;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.CreateMessageStatisticsEventMessage;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Create message statistics event.
 */
@RequiredArgsConstructor
public class CreateMessageStatisticsEvent implements StatisticsEvent {

  private static final EventType EVENT_TYPE = EventType.CREATE_MESSAGE;
  private static final String TIMESTAMP = CustomLocalDateTime.nowAsFullQualifiedTimestamp();

  private @NonNull String consultantId;
  private @NonNull String rcGroupId;

  /** {@inheritDoc} */
  @Override
  public Optional<String> getPayload() {
    return JSONHelper.serialize(
        createCreateMessageStatisticsEventMessage(), LogService::logInternalServerError);
  }

  /** {@inheritDoc} */
  @Override
  public EventType getEventType() {
    return EVENT_TYPE;
  }

  private CreateMessageStatisticsEventMessage createCreateMessageStatisticsEventMessage() {
    return new CreateMessageStatisticsEventMessage()
        .eventType(EVENT_TYPE)
        .consultantId(consultantId)
        .rcGroupId(rcGroupId)
        .timestamp(TIMESTAMP);
  }
}

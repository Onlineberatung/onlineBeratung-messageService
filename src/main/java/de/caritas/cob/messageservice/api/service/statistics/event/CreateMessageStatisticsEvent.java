package de.caritas.cob.messageservice.api.service.statistics.event;

import de.caritas.cob.messageservice.api.helper.CustomOffsetDateTime;
import de.caritas.cob.messageservice.api.helper.JSONHelper;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.EventType;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.CreateMessageStatisticsEventMessage;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.UserRole;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Create message statistics event.
 */
@RequiredArgsConstructor
@AllArgsConstructor
public class CreateMessageStatisticsEvent implements StatisticsEvent {

  private static final EventType EVENT_TYPE = EventType.CREATE_MESSAGE;

  private @NonNull String userId;
  private @NonNull UserRole userRole;
  private @NonNull String rcGroupId;
  private @NonNull Boolean hasAttachment;

  private String receiverId;

  private Long  tenantId;

  /** {@inheritDoc} */
  @Override
  public Optional<String> getPayload() {
    return JSONHelper.serializeWithOffsetDateTimeAsString(createCreateMessageStatisticsEventMessage(),
        LogService::logStatisticsEventError);
  }

  /** {@inheritDoc} */
  @Override
  public EventType getEventType() {
    return EVENT_TYPE;
  }

  private CreateMessageStatisticsEventMessage createCreateMessageStatisticsEventMessage() {
    return new CreateMessageStatisticsEventMessage()
        .eventType(EVENT_TYPE)
        .userId(userId)
        .userRole(userRole)
        .rcGroupId(rcGroupId)
        .hasAttachment(hasAttachment)
        .receiverId(receiverId)
        .tenantId(tenantId)
        .timestamp(CustomOffsetDateTime.nowInUtc());
  }
}

package de.caritas.cob.messageservice.api.service.statistics.event;

import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.CONSULTANT_ID;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.caritas.cob.messageservice.statisticsservice.generated.web.model.EventType;
import java.util.Objects;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class CreateMessageStatisticsEventTest {

  private final String TIMESTAMP_FIELD_NAME = "TIMESTAMP";
  private CreateMessageStatisticsEvent createMessageStatisticsEvent;
  private String staticTimestamp;

  @Before
  public void setup() throws NoSuchFieldException, IllegalAccessException {
    createMessageStatisticsEvent = new CreateMessageStatisticsEvent(CONSULTANT_ID, RC_GROUP_ID);
    staticTimestamp = Objects.requireNonNull(ReflectionTestUtils
            .getField(createMessageStatisticsEvent,
                CreateMessageStatisticsEvent.class,
                TIMESTAMP_FIELD_NAME))
        .toString();
  }

  @Test
  public void getEventType_Should_ReturnEventTypeCreateMessage() {

    assertThat(this.createMessageStatisticsEvent.getEventType(), is(EventType.CREATE_MESSAGE));
  }

  @Test
  public void getPayload_Should_ReturnValidJsonPayload() {

    String expectedJson = "{"
        + "  \"rcGroupId\":\"" + RC_GROUP_ID + "\","
        + "  \"consultantId\":\"" + CONSULTANT_ID + "\","
        + "  \"timestamp\":\"" + staticTimestamp + "\","
        + "  \"eventType\":\"" + EventType.CREATE_MESSAGE + "\""
        + "}";

    Optional<String> result =  createMessageStatisticsEvent.getPayload();

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), jsonEquals(expectedJson));
  }

}

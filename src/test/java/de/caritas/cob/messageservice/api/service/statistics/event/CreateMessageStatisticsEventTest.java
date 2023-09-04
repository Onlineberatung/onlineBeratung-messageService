package de.caritas.cob.messageservice.api.service.statistics.event;

import static de.caritas.cob.messageservice.testhelper.TestConstants.ADVICESEEKER_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.CONSULTANT_ID;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import de.caritas.cob.messageservice.api.helper.CustomOffsetDateTime;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.EventType;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.UserRole;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateMessageStatisticsEventTest {

  private CreateMessageStatisticsEvent createMessageStatisticsEvent;

  @Before
  public void setup() throws NoSuchFieldException, IllegalAccessException {
    createMessageStatisticsEvent =
        new CreateMessageStatisticsEvent(CONSULTANT_ID, UserRole.CONSULTANT, RC_GROUP_ID, false, ADVICESEEKER_ID);
  }

  @Test
  public void getEventType_Should_ReturnEventTypeCreateMessage() {

    assertThat(this.createMessageStatisticsEvent.getEventType(), is(EventType.CREATE_MESSAGE));
  }

  @Test
  public void getPayload_Should_ReturnValidJsonPayload() {

    String expectedJson = "{"
        + "  \"rcGroupId\":\"" + RC_GROUP_ID + "\","
        + "  \"userId\":\"" + CONSULTANT_ID + "\","
        + "  \"userRole\":\"" + UserRole.CONSULTANT + "\","
        + "  \"hasAttachment\": false,"
        + "  \"timestamp\":\"" + CustomOffsetDateTime.nowInUtc() + "\","
        + "  \"eventType\":\"" + EventType.CREATE_MESSAGE + "\""
        + "}";

    Optional<String> result =  createMessageStatisticsEvent.getPayload();

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), jsonEquals(expectedJson).whenIgnoringPaths("timestamp"));
  }

}

package de.caritas.cob.messageservice.api.service.statistics;

import static de.caritas.cob.messageservice.testhelper.TestConstants.ADVICESEEKER_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.CONSULTANT_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.MessageServiceApplication;
import de.caritas.cob.messageservice.api.helper.CustomOffsetDateTime;
import de.caritas.cob.messageservice.api.service.statistics.event.CreateMessageStatisticsEvent;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.EventType;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.CreateMessageStatisticsEventMessage;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.UserRole;
import de.caritas.cob.messageservice.testConfig.RabbitMqTestConfig;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@SpringBootTest(classes = MessageServiceApplication.class)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class StatisticsServiceIT {

  private static final long MAX_TIMEOUT_MILLIS = 5000;

  @Autowired StatisticsService statisticsService;
  @Autowired AmqpTemplate amqpTemplate;

  @Test
  public void fireEvent_Should_Send_ExpectedCreateMessageStatisticsEventMessageToQueue()
      throws IOException {

    CreateMessageStatisticsEvent createMessageStatisticsEvent =
        new CreateMessageStatisticsEvent(CONSULTANT_ID, UserRole.CONSULTANT, RC_GROUP_ID, false, ADVICESEEKER_ID);

    statisticsService.fireEvent(createMessageStatisticsEvent);
    Message message =
        amqpTemplate.receive(RabbitMqTestConfig.QUEUE_NAME_ASSIGN_SESSION, MAX_TIMEOUT_MILLIS);
    assert message != null;

    String expectedJson =
        "{"
            + "  \"rcGroupId\":\""
            + RC_GROUP_ID
            + "\","
            + "  \"userId\":\""
            + CONSULTANT_ID
            + "\","
            + "  \"userRole\":\""
            + UserRole.CONSULTANT
            + "\","
            + "\"receiverId\":\""
            + ADVICESEEKER_ID
            + "\","
            + "  \"timestamp\":\""
            + CustomOffsetDateTime.nowInUtc()
            + "\","
            + "  \"eventType\":\""
            + EventType.CREATE_MESSAGE
            + "\","
            + "  \"hasAttachment\": false"
            + "}";

    assertThat(
        extractBodyFromAmpQMessage(message),
        jsonEquals(expectedJson).whenIgnoringPaths("timestamp"));
  }

  private String extractBodyFromAmpQMessage(Message message) throws IOException {
    return IOUtils.toString(message.getBody(), UTF_8);
  }
}

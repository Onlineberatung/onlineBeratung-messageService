package de.caritas.cob.messageservice.api.helper;

import static de.caritas.cob.messageservice.testhelper.TestConstants.CONSULTANT_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ForwardMessageDTO;
import de.caritas.cob.messageservice.api.model.MessagesDTO;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.CreateMessageStatisticsEventMessage;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.EventType;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.UserRole;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.Mockito;

public class JSONHelperTests {

  private static final EasyRandom easyRandom = new EasyRandom();

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void convertAliasMessageDTOToString_Should_returnConvertedString_When_aliasMessageDTOIsvalid() {
    AliasMessageDTO aliasMessageDTO = easyRandom.nextObject(AliasMessageDTO.class);

    Optional<String> result = JSONHelper.convertAliasMessageDTOToString(aliasMessageDTO);

    assertThat(result.isPresent(), is(true));
  }

  @Test
  public void convertStringToAliasMessageDTO_Should_returnOptionalEmpty_When_jsonStringCanNotBeConverted() {
    Optional<AliasMessageDTO> result = JSONHelper.convertStringToAliasMessageDTO("alias");

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void convertMuteStringToAliasMessageDTOShouldReturnFilledMutedAlias() throws JsonProcessingException {
    var messages = easyRandom.nextObject(MessagesDTO.class);
    messages.setT("user-muted");
    var json = objectMapper.writeValueAsString(messages);

    var aliasMessageDtoOptional = JSONHelper.convertMuteStringToAliasMessageDTO(json);

    assertTrue(aliasMessageDtoOptional.isPresent());
    assertEquals("USER_MUTED", aliasMessageDtoOptional.get().getMessageType().getValue());
  }

  @Test
  public void convertMuteStringToAliasMessageDTOShouldReturnEmptyMutedAliasOnNonMuted() throws JsonProcessingException {
    var messages = easyRandom.nextObject(MessagesDTO.class);
    messages.setT("not-user-muted");
    var json = objectMapper.writeValueAsString(messages);

    var aliasMessageDtoOptional = JSONHelper.convertMuteStringToAliasMessageDTO(json);

    assertTrue(aliasMessageDtoOptional.isEmpty());
  }

  @Test
  public void convertMuteStringToAliasMessageDTOShouldReturnEmptyMutedAliasOnDifferentType() throws JsonProcessingException {
    var forwardMessageDTO = easyRandom.nextObject(ForwardMessageDTO.class);
    var json = objectMapper.writeValueAsString(forwardMessageDTO);

    var aliasMessageDtoOptional = JSONHelper.convertMuteStringToAliasMessageDTO(json);

    assertTrue(aliasMessageDtoOptional.isEmpty());
  }

  @Test
  public void serialize_Should_returnOptionalWithSerializedObject() {

    OffsetDateTime offsetDateTime = CustomOffsetDateTime.nowInUtc();

    CreateMessageStatisticsEventMessage createMessageStatisticsEventMessage =
        new CreateMessageStatisticsEventMessage()
            .eventType(EventType.CREATE_MESSAGE)
            .rcGroupId(RC_GROUP_ID)
            .userId(CONSULTANT_ID)
            .userRole(UserRole.CONSULTANT)
            .timestamp(offsetDateTime)
            .hasAttachment(false);

    Optional<String> result =
        JSONHelper.serializeWithOffsetDateTimeAsString(createMessageStatisticsEventMessage,
            LogService::logInternalServerError);

    assertThat(result.isPresent(), is(true));

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
            + "  \"timestamp\":\""
            + offsetDateTime.format(DateTimeFormatter.ISO_DATE_TIME)
            + "\","
            + "  \"eventType\":\""
            + EventType.CREATE_MESSAGE
            + "\","
            + "  \"hasAttachment\": false"
            + "}";

    assertThat(result.get(), jsonEquals(expectedJson).whenIgnoringPaths("timestamp"));

  }

  @Test
  public void serialize_Should_returnOptionalEmpty_When_jsonStringCanNotBeConverted()
      throws JsonProcessingException {

    ObjectMapper om = Mockito.spy(new ObjectMapper());
    Mockito.when(om.writeValueAsString(Object.class)).thenThrow(new JsonProcessingException("") {});

    Optional<String> result =
        JSONHelper.serializeWithOffsetDateTimeAsString(new Object(),
            LogService::logInternalServerError);

    assertThat(result.isPresent(), is(false));
  }

}

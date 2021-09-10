package de.caritas.cob.messageservice.api.helper;

import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_USER_ID;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.CreateMessageStatisticsEventMessage;
import de.caritas.cob.messageservice.statisticsservice.generated.web.model.EventType;
import java.util.Optional;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.Mockito;

public class JSONHelperTests {

  @Test
  public void convertAliasMessageDTOToString_Should_returnConvertedString_When_aliasMessageDTOIsvalid() {
    AliasMessageDTO aliasMessageDTO = new EasyRandom().nextObject(AliasMessageDTO.class);

    Optional<String> result = JSONHelper.convertAliasMessageDTOToString(aliasMessageDTO);

    assertThat(result.isPresent(), is(true));
  }

  @Test
  public void convertStringToAliasMessageDTO_Should_returnOptionalEmpty_When_jsonStringCanNotBeConverted() {
    Optional<AliasMessageDTO> result = JSONHelper.convertStringToAliasMessageDTO("alias");

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void serialize_Should_returnOptionalWithSerializedObject() throws JsonProcessingException {

    CreateMessageStatisticsEventMessage createMessageStatisticsEventMessage =
        new CreateMessageStatisticsEventMessage()
            .eventType(EventType.CREATE_MESSAGE)
            .rcGroupId(RC_GROUP_ID)
            .rcUserId(RC_USER_ID)
            .timestamp(CustomLocalDateTime.nowAsFullQualifiedTimestamp());

    Optional<String> result =
        JSONHelper.serialize(createMessageStatisticsEventMessage, LogService::logInternalServerError);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), jsonEquals(new ObjectMapper().writeValueAsString(createMessageStatisticsEventMessage)));

  }

  @Test
  public void serialize_Should_returnOptionalEmpty_When_jsonStringCanNotBeConverted()
      throws JsonProcessingException {

    ObjectMapper om = Mockito.spy(new ObjectMapper());
    Mockito.when(om.writeValueAsString(Object.class)).thenThrow(new JsonProcessingException("") {});

    Optional<String> result =
        JSONHelper.serialize(new Object(), LogService::logInternalServerError);

    assertThat(result.isPresent(), is(false));
  }

}

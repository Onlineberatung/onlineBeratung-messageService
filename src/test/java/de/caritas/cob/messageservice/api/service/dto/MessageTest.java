package de.caritas.cob.messageservice.api.service.dto;

import static de.caritas.cob.messageservice.api.model.ReassignStatus.REQUESTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.model.ConsultantReassignment;
import de.caritas.cob.messageservice.api.model.MessageType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Matchers;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;

class MessageTest {

  private static final EasyRandom easyRandom = new EasyRandom();
  private static final String ALIAS_FURTHER_STEPS = "%7B%22forwardMessageDTO%22%3Anull%2C%22videoCallMessageDTO%22%3Anull%2C%22messageType%22%3A%22FURTHER_STEPS%22%7D";

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void messageShouldBeTransformable() throws JsonProcessingException {
    var message = easyRandom.nextObject(Message.class);
    message.setOtherProperties(
        Map.of(
            "x", "y",
            "z", Map.of("a", "b")
        )
    );
    message.setAlias(ALIAS_FURTHER_STEPS);

    var serializedMessage = mapper.writeValueAsString(message);
    assertTrue(serializedMessage.startsWith("{"));
    assertTrue(serializedMessage.contains("\"_id\":\"" + message.getId() + "\""));
    assertFalse(serializedMessage.contains("\"id\""));
    assertTrue(serializedMessage.contains("\"msg\":\"" + message.getMsg() + "\""));
    assertTrue(serializedMessage.contains("%22messageType%22%3A%22FURTHER_STEPS%22"));
    assertFalse(serializedMessage.contains("\"messageType\":\"FURTHER_STEPS\""));
    assertTrue(serializedMessage.contains("\"x\":\"y\""));
    assertTrue(serializedMessage.contains("\"z\":{\"a\":\"b\"}"));
    assertTrue(serializedMessage.endsWith("}"));

    var deserializedMessage = mapper.readValue(serializedMessage, Message.class);
    assertEquals(message.getId(), deserializedMessage.getId());
    assertEquals(message.getMsg(), deserializedMessage.getMsg());
    assertEquals(message.getAlias(), deserializedMessage.getAlias());
    assertEquals(message.getRid(), deserializedMessage.getRid());
    assertEquals(message.getOtherProperties(), deserializedMessage.getOtherProperties());
  }

  @Test
  void messageShouldBeDeserializable() throws JsonProcessingException {
    var serializedMessage = "{"
        + "        \"_id\": \"yj4L38Sf8KtkcCdgn\","
        + "        \"rid\": \"oenMBe5enNCnAXkhp\","
        + "        \"msg\": \"enc:aImAchhLcsiXfUPxHyLS9A==\","
        + "        \"org\": null,"
        + "        \"alias\": \"" + ALIAS_FURTHER_STEPS + "\","
        + "        \"t\": null,"
        + "        \"ts\": \"2022-06-28T10:45:41.907Z\","
        + "        \"u\": {"
        + "            \"_id\": \"9DBgss9ns9ptPQhmA\","
        + "            \"username\": \"System\""
        + "        },"
        + "        \"urls\": [],"
        + "        \"mentions\": [],"
        + "        \"channels\": [],"
        + "        \"_updatedAt\": \"2022-06-28T19:37:24.192Z\""
        + "    }";

    var deserializedMessage = mapper.readValue(serializedMessage, Message.class);
    assertEquals("yj4L38Sf8KtkcCdgn", deserializedMessage.getId());
    assertEquals(ALIAS_FURTHER_STEPS, deserializedMessage.getAlias());
    assertEquals(new ArrayList<>(), deserializedMessage.getOtherProperties().get("urls"));
    var u = deserializedMessage.getOtherProperties().get("u");
    assertEquals("System", ((HashMap<?, ?>) u).get("username"));
    assertEquals("oenMBe5enNCnAXkhp", deserializedMessage.getRid());
  }

  @Test
  void messageShouldTellMessageType() {
    var message = easyRandom.nextObject(Message.class);
    message.setOtherProperties(Map.of("x", "y"));
    message.setAlias(ALIAS_FURTHER_STEPS);

    assertTrue(message.isA(MessageType.FURTHER_STEPS));
    assertFalse(message.isA(MessageType.USER_MUTED));
    assertFalse(message.isA(MessageType.FORWARD));
    assertFalse(message.isA(MessageType.REASSIGN_CONSULTANT));
  }

  @Test
  void messageShouldTellFalseOnNullMessageType() {
    var message = easyRandom.nextObject(Message.class);
    message.setOtherProperties(Map.of("x", "y"));
    message.setAlias(null);

    assertFalse(message.isA(MessageType.FURTHER_STEPS));
    assertFalse(message.isA(MessageType.USER_MUTED));
    assertFalse(message.isA(MessageType.FORWARD));
    assertFalse(message.isA(MessageType.REASSIGN_CONSULTANT));
  }


  @Test
  void messageShouldTellFalseOnEmptyMessageType() {
    var message = easyRandom.nextObject(Message.class);
    message.setOtherProperties(Map.of("x", "y"));
    message.setAlias("");

    assertFalse(message.isA(MessageType.FURTHER_STEPS));
    assertFalse(message.isA(MessageType.USER_MUTED));
    assertFalse(message.isA(MessageType.FORWARD));
    assertFalse(message.isA(MessageType.REASSIGN_CONSULTANT));
  }

  @Test
  void messageShouldTellFalseOnArbitraryMessageType() {
    var message = easyRandom.nextObject(Message.class);
    message.setOtherProperties(Map.of("x", "y"));

    assertFalse(message.isA(MessageType.FURTHER_STEPS));
    assertFalse(message.isA(MessageType.USER_MUTED));
    assertFalse(message.isA(MessageType.FORWARD));
    assertFalse(message.isA(MessageType.REASSIGN_CONSULTANT));
  }

  @Test
  void objectMapperTest() throws JsonProcessingException {
    var stored = "{&quot;toConsultantId&quot;:&quot;8a81117b-d875-4ba4-8696-d62c3a2dae91&quot;,&quot;status&quot;:&quot;REQUESTED&quot;}";
    stored = stored.replace("&quot;", "\"");

    var result = new ObjectMapper().readValue(stored, ConsultantReassignment.class);

    assertThat(result.getStatus(), Matchers.is(REQUESTED));
  }

}

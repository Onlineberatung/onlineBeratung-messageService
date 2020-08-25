package de.caritas.cob.messageservice.api.model.jsonDeserializer;

import static de.caritas.cob.messageservice.testhelper.TestConstants.MESSAGE_FORWARD_ALIAS_JSON_WITH_DECODED_USERNAME;
import static de.caritas.cob.messageservice.testhelper.TestConstants.MESSAGE_FORWARD_ALIAS_JSON_WITH_ENCODED_USERNAME;
import static de.caritas.cob.messageservice.testhelper.TestConstants.MESSAGE_FORWARD_EMPTY_ALIAS_JSON;
import static de.caritas.cob.messageservice.testhelper.TestConstants.MESSAGE_FORWARD_NULL_ALIAS_JSON;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_USER_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.TIMESTAMP;
import static de.caritas.cob.messageservice.testhelper.TestConstants.USERNAME_DECODED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.helper.UserHelper;
import de.caritas.cob.messageservice.api.model.ForwardMessageDTO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ClassUtils;

@RunWith(MockitoJUnitRunner.class)
public class AliasJsonDeserializerTest {

  private ObjectMapper objectMapper;
  private AliasJsonDeserializer aliasJsonDeserializer;
  private UserHelper userHelper;

  @Before
  public void setup() {
    userHelper = new UserHelper();
    objectMapper = new ObjectMapper();
    aliasJsonDeserializer = new AliasJsonDeserializer(userHelper);
  }

  @Test
  public void aliasJsonDeserializer_Schould_haveNoArgsConstructor() {
    assertTrue(ClassUtils.hasConstructor(AliasJsonDeserializer.class));
  }

  @Test
  public void deserialize_Schould_convertAliasWithEncodedUsernameToForwardMessageDTO()
      throws JsonParseException, IOException {
    ForwardMessageDTO result = deserializeAlias(MESSAGE_FORWARD_ALIAS_JSON_WITH_ENCODED_USERNAME);
    assertEquals(RC_USER_ID, result.getRcUserId());
    assertEquals(TIMESTAMP, result.getTimestamp());
    assertEquals(USERNAME_DECODED, result.getUsername());
  }

  @Test
  public void deserialize_Schould_convertAliasWithDecodedUsernameToForwardMessageDTO()
      throws JsonParseException, IOException {
    ForwardMessageDTO result = deserializeAlias(MESSAGE_FORWARD_ALIAS_JSON_WITH_DECODED_USERNAME);
    assertEquals(RC_USER_ID, result.getRcUserId());
    assertEquals(TIMESTAMP, result.getTimestamp());
    assertEquals(USERNAME_DECODED, result.getUsername());
  }

  @Test
  public void deserialize_Schould_ReturnNull_IfAliasIsEmpty()
      throws JsonParseException, IOException {
    ForwardMessageDTO result = deserializeAlias(MESSAGE_FORWARD_EMPTY_ALIAS_JSON);
    assertNull(result);
  }

  @Test
  public void deserialize_Schould_ReturnNull_IfAliasIsNull()
      throws JsonParseException, IOException {
    ForwardMessageDTO result = deserializeAlias(MESSAGE_FORWARD_NULL_ALIAS_JSON);
    assertNull(result);
  }


  private ForwardMessageDTO deserializeAlias(String json) throws JsonParseException, IOException {
    InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    JsonParser jsonParser = objectMapper.getFactory().createParser(stream);
    jsonParser.nextToken();
    jsonParser.nextToken();
    jsonParser.nextToken();
    DeserializationContext deserializationContext = objectMapper.getDeserializationContext();
    return aliasJsonDeserializer.deserialize(jsonParser, deserializationContext);
  }

}

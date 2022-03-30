package de.caritas.cob.messageservice.api.model.jsondeserializer;

import static de.caritas.cob.messageservice.testhelper.TestConstants.DISPLAYNAME_DECODED;
import static de.caritas.cob.messageservice.testhelper.TestConstants.DISPLAYNAME_ENCODED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.helper.UserHelper;
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
public class DecodeUsernameJsonDeserializerTest {

  private ObjectMapper objectMapper;
  private DecodeUsernameJsonDeserializer decodeUsernameJsonDeserializer;
  private UserHelper userHelper;

  @Before
  public void setup() {
    userHelper = new UserHelper();
    objectMapper = new ObjectMapper();
    decodeUsernameJsonDeserializer = new DecodeUsernameJsonDeserializer(userHelper);
  }

  @Test
  public void decodeUsernameJsonDeserializer_Schould_haveNoArgsConstructor() {
    assertTrue(ClassUtils.hasConstructor(DecodeUsernameJsonDeserializer.class));
  }

  @Test
  public void deserialize_Schould_DecodeEncodedUsername() throws JsonParseException, IOException {
    String json = "{\"username:\":\"" + DISPLAYNAME_ENCODED + "\"}";
    String result = deserializeUsername(json);
    assertEquals(DISPLAYNAME_DECODED, result);
  }

  @Test
  public void deserialize_SchouldNot_DecodeNotEncodedUsername()
      throws JsonParseException, IOException {
    String json = "{\"username:\":\"" + DISPLAYNAME_DECODED + "\"}";
    String result = deserializeUsername(json);
    assertEquals(DISPLAYNAME_DECODED, result);
  }

  private String deserializeUsername(String json) throws JsonParseException, IOException {
    InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    JsonParser jsonParser = objectMapper.getFactory().createParser(stream);
    jsonParser.nextToken();
    jsonParser.nextToken();
    jsonParser.nextToken();
    DeserializationContext deserializationContext = objectMapper.getDeserializationContext();
    return decodeUsernameJsonDeserializer.deserialize(jsonParser, deserializationContext);
  }

}

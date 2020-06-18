package de.caritas.cob.MessageService.api.model.jsonDeserializer;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.caritas.cob.MessageService.api.helper.UserHelper;

/**
 * 
 * Json Deserializer for the base32 encoded usernames
 * 
 */
public class DecodeUsernameJsonDeserializer extends JsonDeserializer<String> {

  private UserHelper userHelper;

  public DecodeUsernameJsonDeserializer() {
    this.userHelper = new UserHelper();
  }

  public DecodeUsernameJsonDeserializer(UserHelper userHelper) {
    this.userHelper = userHelper;
  }

  @Override
  public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    String username = jsonParser.getValueAsString();
    return userHelper.decodeUsername(username);
  }

}

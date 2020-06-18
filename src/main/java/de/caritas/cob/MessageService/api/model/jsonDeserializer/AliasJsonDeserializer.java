package de.caritas.cob.MessageService.api.model.jsonDeserializer;

import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.caritas.cob.MessageService.api.helper.JSONHelper;
import de.caritas.cob.MessageService.api.helper.UserHelper;
import de.caritas.cob.MessageService.api.model.ForwardMessageDTO;

/**
 * 
 * Json Deserializer for the alias
 * 
 */
public class AliasJsonDeserializer extends JsonDeserializer<ForwardMessageDTO> {

  private UserHelper userHelper;

  public AliasJsonDeserializer() {
    this.userHelper = new UserHelper();
  }

  public AliasJsonDeserializer(UserHelper userHelper) {
    this.userHelper = userHelper;
  }

  @Override
  public ForwardMessageDTO deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {

    String aliasValue = p.getValueAsString();
    if (aliasValue == null || aliasValue.equals(StringUtils.EMPTY)) {
      return null;
    }

    ForwardMessageDTO alias = null;
    Optional<ForwardMessageDTO> forwardMessageDTO =
        JSONHelper.convertStringToForwardMessageDTO(aliasValue);
    alias = forwardMessageDTO.isPresent() ? forwardMessageDTO.get() : null;
    if (alias.getUsername() != null) {
      alias.setUsername(userHelper.decodeUsername(alias.getUsername()));
    }
    return alias;
  }

}


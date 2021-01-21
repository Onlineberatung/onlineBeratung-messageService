package de.caritas.cob.messageservice.api.model.jsondeserializer;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.caritas.cob.messageservice.api.helper.JSONHelper;
import de.caritas.cob.messageservice.api.helper.UserHelper;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * Json Deserializer for the alias.
 */
public class AliasJsonDeserializer extends JsonDeserializer<AliasMessageDTO> {

  private final UserHelper userHelper;

  public AliasJsonDeserializer() {
    this.userHelper = new UserHelper();
  }

  public AliasJsonDeserializer(UserHelper userHelper) {
    this.userHelper = userHelper;
  }

  @Override
  public AliasMessageDTO deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {

    String aliasValue = p.getValueAsString();
    if (StringUtils.isBlank(aliasValue)) {
      return null;
    }

    Optional<AliasMessageDTO> aliasMessageDTO =
        JSONHelper.convertStringToForwardMessageDTO(aliasValue);
    AliasMessageDTO alias = aliasMessageDTO.orElse(null);
    if (nonNull(alias) && nonNull(alias.getUsername())) {
      alias.setUsername(userHelper.decodeUsername(alias.getUsername()));
    }
    return alias;
  }

}


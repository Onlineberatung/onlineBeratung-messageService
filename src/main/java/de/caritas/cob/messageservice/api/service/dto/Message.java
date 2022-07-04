package de.caritas.cob.messageservice.api.service.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.caritas.cob.messageservice.api.model.MessageType;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class Message {

  @JsonProperty("_id")
  private String id;

  private String alias;

  private String msg;

  private String rid;

  @JsonIgnore
  Map<String, Object> otherProperties = new LinkedHashMap<>();

  @JsonAnySetter
  @SuppressWarnings("unused")
  void setOtherProperty(String key, Object value) {
    otherProperties.put(key, value);
  }

  @JsonAnyGetter
  @SuppressWarnings("unused")
  public Map<String, Object> getOtherProperties() {
    return otherProperties;
  }

  @JsonIgnore
  public boolean isA(MessageType messageType) {
    if (StringUtils.isEmpty(alias)) {
      return false;
    }

    return URLDecoder
        .decode(alias, StandardCharsets.UTF_8)
        .contains("\"messageType\":\"" + messageType + "\"");
  }
}

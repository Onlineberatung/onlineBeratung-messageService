package de.caritas.cob.messageservice.api.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.caritas.cob.messageservice.api.helper.json.OffsetDateTimeToStringSerializer;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ForwardMessageDTO;
import de.caritas.cob.messageservice.api.service.LogService;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Consumer;

/** Helper class for JSON specific tasks. */
public class JSONHelper {

  private JSONHelper() {}

  /**
   * Converts a {@link AliasMessageDTO} into an optional of a json string.
   *
   * @param aliasMessageDTO the message to be converted
   * @return Optional String as JSON
   */
  public static Optional<String> convertAliasMessageDTOToString(AliasMessageDTO aliasMessageDTO) {
    try {
      return Optional.ofNullable(
          UrlEncodingDecodingUtils.urlEncodeString(
              new ObjectMapper().writeValueAsString(aliasMessageDTO)));
    } catch (JsonProcessingException jsonEx) {
      LogService.logInternalServerError(
          "Could not convert AliasMessageDTO to alias String", jsonEx);
      return Optional.empty();
    }
  }

  /**
   * Maps a given String to a {@link ForwardMessageDTO}.
   *
   * @param alias String
   * @return Optional of {@link ForwardMessageDTO}
   */
  public static Optional<ForwardMessageDTO> convertStringToForwardMessageDTO(String alias) {
    try {
      return Optional.ofNullable(
          new ObjectMapper()
              .readValue(UrlEncodingDecodingUtils.urlDecodeString(alias), ForwardMessageDTO.class));
    } catch (IOException jsonParseEx) {
      // This is not an error any more due to restructuring of the alias object. This is not a
      // real error, but necessary due to legacy code
      return Optional.empty();
    }
  }

  /**
   * Maps a given String to a {@link AliasMessageDTO}.
   *
   * @param alias String
   * @return Optional of {@link AliasMessageDTO}
   */
  public static Optional<AliasMessageDTO> convertStringToAliasMessageDTO(String alias) {
    try {
      return Optional.ofNullable(
          new ObjectMapper()
              .readValue(UrlEncodingDecodingUtils.urlDecodeString(alias), AliasMessageDTO.class));

    } catch (IOException jsonParseEx) {
      LogService.logInternalServerError(
          "Could not convert alias String to AliasMessageDTO", jsonParseEx);
      return Optional.empty();
    }
  }

  /**
   * Serialize a object with specific json serializers.
   *
   * @param object an object to serialize
   * @param loggingMethod the method being used to log errors
   * @return {@link Optional} of serialized object as {@link String}
   */
  public static Optional<String> serializeWithOffsetDateTimeAsString(
      Object object, Consumer<Exception> loggingMethod) {
    try {
      return Optional.of(buildObjectMapper().writeValueAsString(object));
    } catch (JsonProcessingException jsonProcessingException) {
      loggingMethod.accept(jsonProcessingException);
    }
    return Optional.empty();
  }

  private static ObjectMapper buildObjectMapper() {

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerModule(buildSimpleModule());
    return objectMapper;
  }

  private static SimpleModule buildSimpleModule() {
    return new SimpleModule()
        .addSerializer(OffsetDateTime.class, new OffsetDateTimeToStringSerializer());
  }
}

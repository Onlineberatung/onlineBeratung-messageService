package de.caritas.cob.messageservice.api.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ForwardMessageDTO;
import de.caritas.cob.messageservice.api.service.LogService;
import java.io.IOException;
import java.util.Optional;

/**
 * Helper class for JSON specific tasks.
 */
public class JSONHelper {

  private JSONHelper() {}

  /**
   * Converts a {@link AliasMessageDTO} into an optional of an json string.
   *
   * @param aliasMessageDTO the message to be converted
   * @return Optional String as JSON
   */
  public static Optional<String> convertAliasMessageDTOToString(AliasMessageDTO aliasMessageDTO) {
    try {
      return Optional
          .ofNullable(
              Helper.urlEncodeString(new ObjectMapper().writeValueAsString(aliasMessageDTO)));
    } catch (JsonProcessingException jsonEx) {
      LogService.logInternalServerError("Could not convert AliasMessageDTO to alias String",
          jsonEx);
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
      return Optional
          .ofNullable(
              new ObjectMapper().readValue(Helper.urlDecodeString(alias), ForwardMessageDTO.class));
    } catch (IOException jsonParseEx) {
      // This is no error any more due to restructuring of alias object. This method is still
      // used for created alias objects before of the video adaption.
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
      return Optional
          .ofNullable(
              new ObjectMapper().readValue(Helper.urlDecodeString(alias), AliasMessageDTO.class));

    } catch (IOException jsonParseEx) {
      LogService.logInternalServerError("Could not convert alias String to AliasMessageDTO",
          jsonParseEx);
      return Optional.empty();
    }
  }

}

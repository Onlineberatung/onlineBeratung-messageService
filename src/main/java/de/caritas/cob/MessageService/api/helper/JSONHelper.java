package de.caritas.cob.MessageService.api.helper;

import java.io.IOException;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.caritas.cob.MessageService.api.model.ForwardMessageDTO;
import de.caritas.cob.MessageService.api.service.LogService;

/**
 * Helper class for JSON specific tasks
 *
 */
public class JSONHelper {

  private static LogService logService;
  @Autowired
  private LogService autowiredLogService;

  @PostConstruct
  public void init() {
    JSONHelper.logService = autowiredLogService;
  }

  /**
   * Converts a {@link ForwardMessageDTO} to a JSON formatted String
   * 
   * @param forwardMessageDTO
   * @return Optional String as JSON
   */
  /**
   * 
   * @param forwardMessageDTO
   * @return
   */
  public static Optional<String> convertForwardMessageDTOToString(
      ForwardMessageDTO forwardMessageDTO) {

    ObjectMapper mapper = new ObjectMapper();
    try {
      return Optional
          .ofNullable(Helper.urlEncodeString(mapper.writeValueAsString(forwardMessageDTO)));

    } catch (JsonProcessingException jsonEx) {
      logService.logInternalServerError("Could not convert ForwardMessageDTO to alias String",
          jsonEx);
      return Optional.empty();
    }
  }

  /**
   * Maps a given String to a {@link ForwardMessageDTO}
   * 
   * @param alias String
   * @return Optional of {@link ForwardMessageDTO}
   */
  public static Optional<ForwardMessageDTO> convertStringToForwardMessageDTO(String alias) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return Optional
          .ofNullable(mapper.readValue(Helper.urlDecodeString(alias), ForwardMessageDTO.class));

    } catch (JsonParseException jsonParseEx) {
      logService.logInternalServerError("Could not convert alias String to ForwardMessageDTO",
          jsonParseEx);
      return Optional.empty();

    } catch (JsonMappingException jsonMappingEx) {
      logService.logInternalServerError("Could not convert alias String to ForwardMessageDTO",
          jsonMappingEx);
      return Optional.empty();

    } catch (IOException ioEx) {
      logService.logInternalServerError("Could not convert alias String to ForwardMessageDTO",
          ioEx);
      return Optional.empty();
    }
  }

}

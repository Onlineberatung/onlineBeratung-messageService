package de.caritas.cob.MessageService.api.controller;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import de.caritas.cob.MessageService.api.facade.PostGroupMessageFacade;
import de.caritas.cob.MessageService.api.helper.JSONHelper;
import de.caritas.cob.MessageService.api.model.ForwardMessageDTO;
import de.caritas.cob.MessageService.api.model.MasterKeyDTO;
import de.caritas.cob.MessageService.api.model.MessageDTO;
import de.caritas.cob.MessageService.api.model.MessageStreamDTO;
import de.caritas.cob.MessageService.api.service.EncryptionService;
import de.caritas.cob.MessageService.api.service.LogService;
import de.caritas.cob.MessageService.api.service.RocketChatService;
import de.caritas.cob.MessageService.generated.api.controller.MessagesApi;
import io.swagger.annotations.Api;

/**
 * Controller for message requests
 */
@RestController
@Api(tags = "message-controller")
public class MessageController implements MessagesApi {

  @Autowired
  RocketChatService rocketChatService;

  @Autowired
  EncryptionService encryptionService;

  @Autowired
  LogService logService;

  @Autowired
  PostGroupMessageFacade postGroupMessageFacade;

  /**
   * Returns a list of {@link MessageStreamDTO}s from the specified Rocket.Chat group
   */
  @Override
  public ResponseEntity<MessageStreamDTO> getMessageStream(@RequestHeader String rcToken,
      @RequestHeader String rcUserId, @NotBlank @Valid @RequestParam String rcGroupId,
      @NotNull @Valid @RequestParam Integer offset, @NotNull @Valid @RequestParam Integer count) {

    MessageStreamDTO message =
        rocketChatService.getGroupMessages(rcToken, rcUserId, rcGroupId, offset, count);

    return (message != null) ? new ResponseEntity<MessageStreamDTO>(message, HttpStatus.OK)
        : new ResponseEntity<MessageStreamDTO>(HttpStatus.NO_CONTENT);
  }

  /**
   * Upates the Master-Key Fragment for the en-/decryption of messages
   * 
   * @param masterKey
   * @return
   */
  @Override
  public ResponseEntity<Void> updateKey(@Valid @RequestBody MasterKeyDTO masterKey) {

    if (!encryptionService.getMasterKey().equals(masterKey.getMasterKey())) {
      encryptionService.updateMasterKey(masterKey.getMasterKey());
      logService.logInfo("MasterKey updated");
      return new ResponseEntity<Void>(HttpStatus.OK);
    }

    return new ResponseEntity<Void>(HttpStatus.CONFLICT);
  }

  /**
   * Posts a message in the specified Rocket.Chat group
   */
  @Override
  public ResponseEntity<Void> createMessage(@Valid @RequestBody MessageDTO message,
      @NotBlank @RequestHeader String rcToken, @NotBlank @NotNull @RequestHeader String rcUserId,
      @NotBlank @NotNull @RequestHeader String rcGroupId) {

    HttpStatus status =
        postGroupMessageFacade.postGroupMessage(rcToken, rcUserId, rcGroupId, message);

    return (status != null) ? new ResponseEntity<Void>(status)
        : new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Forwards/posts a message in the specified Rocket.Chat group and sets the values from the body
   * object in the alias object of the Rocket.Chat message.
   */
  @Override
  public ResponseEntity<Void> forwardMessage(
      @Valid @RequestBody ForwardMessageDTO forwardMessageDTO,
      @NotBlank @RequestHeader String rcToken, @NotBlank @NotNull @RequestHeader String rcUserId,
      @NotBlank @NotNull @RequestHeader String rcGroupId) {

    Optional<String> alias = JSONHelper.convertForwardMessageDTOToString(forwardMessageDTO);

    if (!alias.isPresent()) {
      return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
    }

    HttpStatus status = postGroupMessageFacade.postFeedbackGroupMessage(rcToken, rcUserId,
        rcGroupId, forwardMessageDTO.getMessage(), alias.get());

    return (status != null) ? new ResponseEntity<Void>(status)
        : new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Posts a message in the specified Feedback Rocket.Chat group
   */
  @Override
  public ResponseEntity<Void> createFeedbackMessage(@Valid @RequestBody MessageDTO message,
      @NotBlank @RequestHeader String rcToken, @NotBlank @NotNull @RequestHeader String rcUserId,
      @NotBlank @NotNull @RequestHeader String rcFeedbackGroupId) {

    HttpStatus status = postGroupMessageFacade.postFeedbackGroupMessage(rcToken, rcUserId,
        rcFeedbackGroupId, message.getMessage(), null);

    return (status != null) ? new ResponseEntity<Void>(status)
        : new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);


  }
}

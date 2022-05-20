package de.caritas.cob.messageservice.api.controller;

import de.caritas.cob.messageservice.api.facade.PostGroupMessageFacade;
import de.caritas.cob.messageservice.api.helper.JSONHelper;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.AliasOnlyMessageDTO;
import de.caritas.cob.messageservice.api.model.ChatMessage;
import de.caritas.cob.messageservice.api.model.DraftMessageDTO;
import de.caritas.cob.messageservice.api.model.ForwardMessageDTO;
import de.caritas.cob.messageservice.api.model.MasterKeyDTO;
import de.caritas.cob.messageservice.api.model.MessageDTO;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType;
import de.caritas.cob.messageservice.api.service.DraftMessageService;
import de.caritas.cob.messageservice.api.service.EncryptionService;
import de.caritas.cob.messageservice.api.service.LogService;
import de.caritas.cob.messageservice.api.service.RocketChatService;
import de.caritas.cob.messageservice.generated.api.controller.MessagesApi;
import io.swagger.annotations.Api;
import java.util.Optional;
import javax.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for message requests.
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "message-controller")
public class MessageController implements MessagesApi {

  private final @NonNull RocketChatService rocketChatService;
  private final @NonNull EncryptionService encryptionService;
  private final @NonNull PostGroupMessageFacade postGroupMessageFacade;
  private final @NonNull DraftMessageService draftMessageService;

  /**
   * Returns a list of {@link MessageStreamDTO}s from the specified Rocket.Chat group.
   *
   * @param rcToken   (required) Rocket.Chat token of the user
   * @param rcUserId  (required) Rocket.Chat user ID
   * @param rcGroupId (required) Rocket.Chat group ID
   * @return {@link ResponseEntity} containing {@link MessageStreamDTO}
   */
  @Override
  public ResponseEntity<MessageStreamDTO> getMessageStream(@RequestHeader String rcToken,
      @RequestHeader String rcUserId, @RequestParam String rcGroupId) {

    MessageStreamDTO message =
        rocketChatService.getGroupMessages(rcToken, rcUserId, rcGroupId);

    return (message != null) ? new ResponseEntity<>(message, HttpStatus.OK)
        : new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * Updates the Master-Key Fragment for the en-/decryption of messages.
   *
   * @param masterKey the master key
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<Void> updateKey(@Valid @RequestBody MasterKeyDTO masterKey) {

    if (!encryptionService.getMasterKey().equals(masterKey.getMasterKey())) {
      encryptionService.updateMasterKey(masterKey.getMasterKey());
      LogService.logInfo("MasterKey updated");
      return new ResponseEntity<>(HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.CONFLICT);
  }

  /**
   * Posts a message in the specified Rocket.Chat group.
   *
   * @param rcToken   (required) Rocket.Chat token of the user
   * @param rcUserId  (required) Rocket.Chat user ID
   * @param rcGroupId (required) Rocket.Chat group ID
   * @param message   (required) the message
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<Void> createMessage(@RequestHeader String rcToken,
      @RequestHeader String rcUserId, @RequestHeader String rcGroupId,
      @Valid @RequestBody MessageDTO message) {

    var groupMessage = ChatMessage.builder().rcToken(rcToken).rcUserId(rcUserId).rcGroupId(rcGroupId)
        .text(message.getMessage()).sendNotification(Boolean.TRUE.equals(message.getSendNotification()))
        .t(message.getT()).build();

    postGroupMessageFacade.postGroupMessage(groupMessage);

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  /**
   * Forwards/posts a message in the specified Rocket.Chat group and sets the values from the body
   * object in the alias object of the Rocket.Chat message.
   *
   * @param rcToken           (required) Rocket.Chat token of the user
   * @param rcUserId          (required) Rocket.Chat user ID
   * @param rcGroupId         (required) Rocket.Chat group ID
   * @param forwardMessageDTO (required) {@link ForwardMessageDTO}
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<Void> forwardMessage(@RequestHeader String rcToken,
      @RequestHeader String rcUserId, @RequestHeader String rcGroupId,
      @Valid @RequestBody ForwardMessageDTO forwardMessageDTO) {

    Optional<String> alias =
        JSONHelper.convertAliasMessageDTOToString(
            new AliasMessageDTO().forwardMessageDTO(forwardMessageDTO));

    if (alias.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    var forwardMessage = ChatMessage.builder().rcToken(rcToken).rcUserId(rcUserId)
        .rcGroupId(rcGroupId).text(forwardMessageDTO.getMessage()).t(forwardMessageDTO.getT())
        .alias(alias.get()).build();

    postGroupMessageFacade.postFeedbackGroupMessage(forwardMessage);

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  /**
   * Posts a message in the specified Feedback Rocket.Chat group.
   *
   * @param rcToken           (required) Rocket.Chat token of the user
   * @param rcUserId          (required) Rocket.Chat user ID
   * @param rcFeedbackGroupId (required) Rocket.Chat group ID
   * @param message           (required) the message
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<Void> createFeedbackMessage(@RequestHeader String rcToken,
      @RequestHeader String rcUserId, @RequestHeader String rcFeedbackGroupId,
      @Valid @RequestBody MessageDTO message) {

    var feedbackMessage = ChatMessage.builder().rcToken(rcToken).rcUserId(rcUserId)
        .rcGroupId(rcFeedbackGroupId).t(message.getT()).text(message.getMessage()).build();

    postGroupMessageFacade.postFeedbackGroupMessage(feedbackMessage);

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  /**
   * Creates a video event hint message.
   *
   * @param rcGroupId           the Rocket.Chat group to post the hint message
   * @param videoCallMessageDTO the {@link VideoCallMessageDTO} containing the information to be
   *                            written in the alias object
   */
  @Override
  public ResponseEntity<Void> createVideoHintMessage(@RequestHeader String rcGroupId,
      @Valid @RequestBody VideoCallMessageDTO videoCallMessageDTO) {

    this.postGroupMessageFacade.createVideoHintMessage(rcGroupId, videoCallMessageDTO);

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  /**
   * Saves a draft message identified by current authenticated user and Rocket.Chat group.
   *
   * @param rcGroupId (required) Rocket.Chat group ID
   * @param message   the message
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<Void> saveDraftMessage(@RequestHeader String rcGroupId,
      @Valid @RequestBody DraftMessageDTO message) {

    SavedDraftType savedDraftType = this.draftMessageService.saveDraftMessage(message.getMessage(),
        rcGroupId, message.getT());

    return new ResponseEntity<>(savedDraftType.getHttpStatus());
  }

  /**
   * Returns a saved draft message if present.
   *
   * @param rcGroupId (required) Rocket.Chat group ID
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<DraftMessageDTO> findDraftMessage(@RequestHeader String rcGroupId) {
    Optional<DraftMessageDTO> draftMessage = this.draftMessageService.findAndDecryptDraftMessage(
        rcGroupId);
    return draftMessage.map(ResponseEntity::ok)
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  /**
   * Posts an empty message which only contains an alias with the provided {@link MessageType} in
   * the specified Rocket.Chat group.
   *
   * @param rcGroupId           (required) Rocket.Chat group ID
   * @param aliasOnlyMessageDTO {@link AliasOnlyMessageDTO}
   * @return {@link ResponseEntity} with the {@link HttpStatus}
   */
  @Override
  public ResponseEntity<Void> saveAliasOnlyMessage(@RequestHeader String rcGroupId,
      @Valid AliasOnlyMessageDTO aliasOnlyMessageDTO) {
    postGroupMessageFacade.postAliasOnlyMessage(rcGroupId, aliasOnlyMessageDTO.getMessageType());

    return new ResponseEntity<>(HttpStatus.CREATED);
  }
}

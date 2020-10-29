package de.caritas.cob.messageservice.api.service;

import static de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType.NEW_MESSAGE;
import static de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType.OVERWRITTEN_MESSAGE;

import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType;
import de.caritas.cob.messageservice.api.model.draftmessage.entity.DraftMessage;
import de.caritas.cob.messageservice.api.repository.DraftMessageRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service class to provide creation, updating and deletion of draft messages.
 */
@Service
@RequiredArgsConstructor
public class DraftMessageService {

  private final @NonNull DraftMessageRepository draftMessageRepository;
  private final @NonNull EncryptionService encryptionService;
  private final @NonNull AuthenticatedUser authenticatedUser;

  /**
   * Encrypts and saves a draft message. The message will be overwritten if a message for the given
   * user and rocket chat group id already exists.
   *
   * @param message the message to encrypt and persist
   * @param rcGroupId the rocket chat group id
   * @return a {@link SavedDraftType} for the created type
   */
  public SavedDraftType saveDraftMessage(String message, String rcGroupId) {

    Optional<DraftMessage> optionalDraftMessage = this.draftMessageRepository
        .findByUserIdAndRcGroupId(this.authenticatedUser.getUserId(), rcGroupId);

    DraftMessage draftMessage = optionalDraftMessage.orElse(buildNewDraftMessage(rcGroupId));
    updateMessage(message, rcGroupId, draftMessage);

    this.draftMessageRepository.save(draftMessage);
    return extractSavedDraftType(optionalDraftMessage);
  }

  private SavedDraftType extractSavedDraftType(Optional<DraftMessage> optionalDraftMessage) {
    return optionalDraftMessage.isPresent() ? OVERWRITTEN_MESSAGE : NEW_MESSAGE;
  }

  private DraftMessage buildNewDraftMessage(String rcGroupId) {
    return DraftMessage.builder()
        .createDate(LocalDateTime.now())
        .userId(this.authenticatedUser.getUserId())
        .rcGroupId(rcGroupId)
        .build();
  }

  private void updateMessage(String message, String rcGroupId, DraftMessage draftMessage) {
    try {
      String encryptedMessage = this.encryptionService.encrypt(message, rcGroupId);
      draftMessage.setMessage(encryptedMessage);
    } catch (CustomCryptoException e) {
      throw new InternalServerErrorException(e, LogService::logInternalServerError);
    }
  }

  /**
   * Deletes a draft message if exists.
   *
   * @param rcGroupId the rocket chat group id
   */
  public void deleteDraftMessageIfExist(String rcGroupId) {
    this.draftMessageRepository.findByUserIdAndRcGroupId(this.authenticatedUser.getUserId(),
        rcGroupId).ifPresent(this::deleteDraftMessage);
  }

  private void deleteDraftMessage(DraftMessage draftMessage) {
    this.draftMessageRepository.delete(draftMessage);
  }

}

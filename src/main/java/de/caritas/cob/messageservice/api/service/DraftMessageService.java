package de.caritas.cob.messageservice.api.service;

import static de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType.NEW_MESSAGE;
import static de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType.OVERWRITTEN_MESSAGE;
import static de.caritas.cob.messageservice.api.service.RocketChatService.E2E_ENCRYPTION_TYPE;

import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.model.DraftMessageDTO;
import de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType;
import de.caritas.cob.messageservice.api.model.draftmessage.entity.DraftMessage;
import de.caritas.cob.messageservice.api.repository.DraftMessageRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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
   * @param message         the message to encrypt and persist
   * @param rcGroupId       the rocket chat group id
   * @param t               type of the message
   * @return a {@link SavedDraftType} for the created type
   */
  public synchronized SavedDraftType saveDraftMessage(String message, String rcGroupId, String t) {

    var optionalDraftMessage = findDraftMessage(rcGroupId);
    var draftMessage = createOrUpdateDraftMessage(rcGroupId, t, optionalDraftMessage);
    updateMessage(message, rcGroupId, draftMessage);
    this.draftMessageRepository.save(draftMessage);

    return extractSavedDraftType(optionalDraftMessage);
  }

  private DraftMessage createOrUpdateDraftMessage(String rcGroupId, String t,
      Optional<DraftMessage> optionalDraftMessage) {
    var draftMessageRef = new AtomicReference<DraftMessage>();
    optionalDraftMessage.ifPresentOrElse(
        dm -> {
          dm.setT(t);
          draftMessageRef.set(dm);
        },
        () -> {
          var dm = buildNewDraftMessage(rcGroupId, t);
          draftMessageRef.set(dm);
        }
    );

    return draftMessageRef.get();
  }

  private Optional<DraftMessage> findDraftMessage(String rcGroupId) {
    return this.draftMessageRepository
        .findByUserIdAndRcGroupId(this.authenticatedUser.getUserId(), rcGroupId);
  }

  private SavedDraftType extractSavedDraftType(Optional<DraftMessage> optionalDraftMessage) {
    return optionalDraftMessage.isPresent() ? OVERWRITTEN_MESSAGE : NEW_MESSAGE;
  }

  private DraftMessage buildNewDraftMessage(String rcGroupId, String t) {
    return DraftMessage.builder()
        .createDate(LocalDateTime.now())
        .userId(this.authenticatedUser.getUserId())
        .rcGroupId(rcGroupId)
        .t(t)
        .build();
  }

  private void updateMessage(String message, String rcGroupId,
      DraftMessage draftMessage) {
    try {
      if (isMessageE2eEncrypted(draftMessage)) {
        draftMessage.setMessage(message);
      } else {
        var encryptedText = this.encryptionService.encrypt(message, rcGroupId);
        draftMessage.setMessage(encryptedText);
      }
    } catch (CustomCryptoException e) {
      throw new InternalServerErrorException(e, LogService::logInternalServerError);
    }
  }

  private boolean isMessageE2eEncrypted(DraftMessage draftMessage) {
    return E2E_ENCRYPTION_TYPE.equals(draftMessage.getT());
  }

  /**
   * Deletes a draft message if exists.
   *
   * @param rcGroupId the rocket chat group id
   */
  public synchronized void deleteDraftMessageIfExist(String rcGroupId) {
    this.findDraftMessage(rcGroupId).ifPresent(this::deleteDraftMessage);
  }

  private void deleteDraftMessage(DraftMessage draftMessage) {
    this.draftMessageRepository.delete(draftMessage);
  }

  /**
   * Searches for a draft message by the authenticated user and given rocket chat group id.
   *
   * @param rcGroupId the rocket chat group id
   * @return an {@link Optional} of the database query result
   */
  public Optional<DraftMessageDTO> findAndDecryptDraftMessage(String rcGroupId) {
    return findDraftMessage(rcGroupId)
        .map(toDecryptedMessage(rcGroupId))
        .map(toDraftMessageDTO());
  }

  private Function<DraftMessage, DraftMessageDTO> toDraftMessageDTO() {
    return dm -> {
      var dto = new DraftMessageDTO();
      dto.setMessage(dm.getMessage());
      dto.setT(dm.getT());
      return dto;
    };
  }

  private Function<DraftMessage, DraftMessage> toDecryptedMessage(String rcGroupId) {
    return dm -> {
      dm.setMessage(decryptMessage(dm.getMessage(), rcGroupId));
      return dm;
    };
  }

  private String decryptMessage(String encryptedMessage, String rcGroupId) {
    try {
      return this.encryptionService.decrypt(encryptedMessage, rcGroupId);
    } catch (CustomCryptoException e) {
      throw new InternalServerErrorException(e, LogService::logInternalServerError);
    }
  }

}

package de.caritas.cob.messageservice.api.service;

import static de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType.NEW_MESSAGE;
import static de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType.OVERWRITTEN_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.model.draftmessage.SavedDraftType;
import de.caritas.cob.messageservice.api.model.draftmessage.entity.DraftMessage;
import de.caritas.cob.messageservice.api.repository.DraftMessageRepository;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DraftMessageServiceTest {

  @InjectMocks
  private DraftMessageService draftMessageService;

  @Mock
  private DraftMessageRepository draftMessageRepository;

  @Mock
  @SuppressWarnings("unused")
  private AuthenticatedUser authenticatedUser;

  @Mock
  private EncryptionService encryptionService;

  @Captor
  ArgumentCaptor<DraftMessage> captor;

  @Test
  public void saveDraftMessage_Should_returnNewMessageType_When_noMessageForUserAndRcGroupExists()
      throws CustomCryptoException {

    SavedDraftType savedDraftType = this.draftMessageService.saveDraftMessage("message",
        "rcGroupId", null);

    assertThat(savedDraftType, is(NEW_MESSAGE));
    verify(this.draftMessageRepository, times(1)).save(any());
    verify(this.encryptionService).encrypt(any(), any());
  }

  @Test
  public void saveDraftMessage_Should_returnOverwrittenMessageType_When_messageForUserAndRcGroupExists()
      throws CustomCryptoException {
    when(this.draftMessageRepository.findByUserIdAndRcGroupId(any(), any()))
        .thenReturn(Optional.of(new DraftMessage()));

    SavedDraftType savedDraftType = this.draftMessageService.saveDraftMessage("message",
        "rcGroupId", "p");

    assertThat(savedDraftType, is(OVERWRITTEN_MESSAGE));
    verify(this.encryptionService).encrypt(any(), any());

    verify(this.draftMessageRepository).save(captor.capture());
    assertThat("p", is(captor.getValue().getT()));
  }

  @Test
  public void saveDraftMessage_should_not_encrypt_message_if_already_e2e_encrypted()
      throws CustomCryptoException {
    draftMessageService.saveDraftMessage("message", "rcGroupId", "e2e");

    verifyNoMoreInteractions(this.encryptionService);
    verify(this.draftMessageRepository).save(captor.capture());
    assertThat("e2e", is(captor.getValue().getT()));
  }

  @Test
  public void deleteDraftMessageIfExist_Should_notCallDelete_When_noMessageForUserAndGroupExists() {
    this.draftMessageService.deleteDraftMessageIfExist("rcGroupId");

    verify(this.draftMessageRepository, times(0)).delete(any());
  }

  @Test
  public void deleteDraftMessageIfExist_Should_callDelete_When_messageForUserAndGroupExists() {
    when(this.draftMessageRepository.findByUserIdAndRcGroupId(any(), any()))
        .thenReturn(Optional.of(new DraftMessage()));

    this.draftMessageService.deleteDraftMessageIfExist("rcGroupId");

    verify(this.draftMessageRepository, times(1)).delete(any());
  }

  @Test(expected = InternalServerErrorException.class)
  public void saveDraftMessage_Should_throwInternalServerError_When_encryptionServiceThrowsCustomCryptoException()
      throws CustomCryptoException {
    when(this.encryptionService.encrypt(any(), any()))
        .thenThrow(new CustomCryptoException(new Exception()));

    this.draftMessageService.saveDraftMessage("message", "rcGroupId", "text");
  }

  @Test
  public void findAndDecryptDraftMessage_Should_returnNull_When_noDraftMessageIsPresent() {
    var draftMessage = this.draftMessageService.findAndDecryptDraftMessage("rcGroupId");

    assertThat(draftMessage.isEmpty(), is(true));
    verifyNoInteractions(this.encryptionService);
  }

  @Test
  public void findAndDecryptDraftMessage_Should_returnNull_When_rcGroupIdIsNull() {
    var draftMessage = this.draftMessageService.findAndDecryptDraftMessage(null);

    assertThat(draftMessage.isEmpty(), is(true));
    verifyNoInteractions(this.encryptionService);
  }

  @Test
  public void findAndDecryptDraftMessage_Should_returnDecryptedMessage_When_draftMessageIsPresent()
      throws CustomCryptoException {
    var draftMessage = DraftMessage.builder().message("encrypted").build();
    when(this.draftMessageRepository.findByUserIdAndRcGroupId(any(), any()))
        .thenReturn(Optional.of(draftMessage));
    when(this.encryptionService.decrypt(any(), any())).thenReturn("decrypted");

    var message = this.draftMessageService.findAndDecryptDraftMessage("rcGroupId");

    assertThat(message.isPresent(), is(true));
    assertThat(message.get().getMessage(), is("decrypted"));
    verify(this.encryptionService, times(1)).decrypt("encrypted", "rcGroupId");
  }

  @Test(expected = InternalServerErrorException.class)
  public void findAndDecryptDraftMessage_Should_throwInternalServerError_When_encryptionServiceThrowsCustomCryptoException()
      throws CustomCryptoException {
    when(this.draftMessageRepository.findByUserIdAndRcGroupId(any(), any()))
        .thenReturn(Optional.of(new DraftMessage()));
    when(this.encryptionService.decrypt(any(), any())).thenThrow(
        new CustomCryptoException(new Exception()));

    this.draftMessageService.findAndDecryptDraftMessage("rcGroupId");
  }

}

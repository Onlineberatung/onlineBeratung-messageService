package de.caritas.cob.messageservice.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import de.caritas.cob.messageservice.api.exception.CustomCryptoException;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionServiceTest {

  private final String KEY_MASTER = "MasterKeyTestKey";
  private final String KEY_APPLICATION = "ApplicationTestKey";
  private final String KEY_SESSION = "SessionTestKey";
  private final String KEY_SESSION_WRONG = "WrongSessionTestKey";

  private final String MESSAGE_PLAIN = "Das hier ist jetzt mal eine Test-Message";
  private final String MESSAGE_ENCRYPTED =
      "enc:uWHNUkWrQJikGnVpknvB3SkzT1RWHJuY0igDT9p7fGFHWECLBpV2+0eIZF6Qi7J0";

  @InjectMocks
  private EncryptionService encryptionService;

  @Mock
  private LogService logService;

  @Before
  public void setup() throws NoSuchFieldException {
    FieldSetter.setField(encryptionService,
        encryptionService.getClass().getDeclaredField("fragment_applicationKey"), KEY_APPLICATION);
    encryptionService.updateMasterKey(KEY_MASTER);
  }

  @Test
  public void check_setup() {
    assertEquals("MasterKey was not properly set", KEY_MASTER, encryptionService.getMasterKey());
    assertEquals("ApplicationKey was not properly set", KEY_APPLICATION,
        encryptionService.getApplicationKey());
  }

  @Test
  public void updateMasterKey_Should_UpdateMasterKeyFragment() {
    encryptionService.updateMasterKey(KEY_MASTER);
    assertEquals("Cannot properly set MasterKey", KEY_MASTER, encryptionService.getMasterKey());
  }

  @Test
  public void encrypt_Should_ReturnEncryptedText_WhenProvidedWithValidParameters()
      throws  CustomCryptoException {
    String encryptMessage = encryptionService.encrypt(MESSAGE_PLAIN, KEY_SESSION);
    assertEquals("Did not get the expected encryption result.", MESSAGE_ENCRYPTED, encryptMessage);
  }

  @Test
  public void encrypt_Should_ReturnWrongEncryptedText_WhenProvidedWithInvalidParameters()
      throws  CustomCryptoException {
    String encryptMessage = encryptionService.encrypt(MESSAGE_PLAIN, KEY_SESSION_WRONG);
    Assert.assertNotEquals("Did not get the expected encryption result.", MESSAGE_ENCRYPTED,
        encryptMessage);
  }

  @Test
  public void decrypt_Should_ReturnDecryptedText_WhenProvidedWithValidParameters()
      throws  CustomCryptoException {
    String decryptedMessage = encryptionService.decrypt(MESSAGE_ENCRYPTED, KEY_SESSION);
    assertEquals("Did not get the expected decrypted result.", MESSAGE_PLAIN, decryptedMessage);
  }

  @Test
  public void decrypt_Should_ReturnWrongDecryptedText_WhenProvidedWithInvalidParameters() {
    try {
      encryptionService.decrypt(MESSAGE_ENCRYPTED, KEY_SESSION_WRONG);
      fail("The expected BadPaddingException due to wrong password was not thrown.");
    } catch (CustomCryptoException ex) {
      assertTrue("Expected BadPaddingException thrown", true);
    }
  }

  @Test
  public void decrypt_Should_ReturnNull_WhenMessageIsNull() throws CustomCryptoException {
    assertNull(encryptionService.decrypt(null, KEY_MASTER));
  }

}

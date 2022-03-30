package de.caritas.cob.messageservice.api.helper;

import static de.caritas.cob.messageservice.testhelper.TestConstants.DISPLAYNAME_DECODED;
import static de.caritas.cob.messageservice.testhelper.TestConstants.DISPLAYNAME_ENCODED;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserHelperTest {

  private UserHelper userHelper;

  @Before
  public void setup() {
    this.userHelper = new UserHelper();
  }

  @Test
  public void encodeUsername_Should_ReturnEncodedUsernameWithReplacedPaddingAndAddedPrefix_WhenDecodedUsernameIsGiven() {
    assertEquals(DISPLAYNAME_ENCODED, userHelper.encodeUsername(DISPLAYNAME_DECODED));
  }

  @Test
  public void encodeUsername_Should_ReturnEncodedUsername_WhenEncodedUsernameIsGiven() {
    assertEquals(DISPLAYNAME_ENCODED, userHelper.encodeUsername(DISPLAYNAME_ENCODED));
  }

  @Test
  public void decodeUsername_Should_ReturnDecodedUsername_WhenEncodedUsernameIsGiven() {
    assertEquals(DISPLAYNAME_DECODED, userHelper.decodeUsername(DISPLAYNAME_ENCODED));
  }

  @Test
  public void decodeUsername_Should_ReturnDecodedUsername_WhenDecodedUsernameIsGiven() {
    assertEquals(DISPLAYNAME_DECODED, userHelper.decodeUsername(DISPLAYNAME_DECODED));
  }

}

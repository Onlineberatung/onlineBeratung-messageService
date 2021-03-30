package de.caritas.cob.messageservice.api.helper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UrlEncodingDecodingUtilsTest {

  private static final String DECODED_STRING = "töst#$";
  private static final String ENCODED_STRING = "t%C3%B6st%23%24";

  @Test
  public void urlEncodeString_Should_ReturnEncodedString() {
    assertEquals(ENCODED_STRING, UrlEncodingDecodingUtils.urlEncodeString(DECODED_STRING));
  }

  @Test
  public void urlDecodeString_Should_ReturnEncodedString() {
    assertEquals(DECODED_STRING, UrlEncodingDecodingUtils.urlDecodeString(ENCODED_STRING));
  }

}

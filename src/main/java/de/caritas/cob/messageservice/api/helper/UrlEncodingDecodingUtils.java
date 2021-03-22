package de.caritas.cob.messageservice.api.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Util class to provide url encoding and decoding.
 */
public class UrlEncodingDecodingUtils {

  private UrlEncodingDecodingUtils() {}

  /**
   * URL encoding for a given string.
   *
   * @return the encoded string
   */
  public static String urlEncodeString(String stringToEncode) {
    try {
      return URLEncoder.encode(stringToEncode, StandardCharsets.UTF_8.name());

    } catch (UnsupportedEncodingException ex) {
      return null;
    }
  }

  /**
   * Url decoding for a given string.
   *
   * @return the decoded string
   */
  public static String urlDecodeString(String stringToDecode) {
    try {
      return URLDecoder.decode(stringToDecode, StandardCharsets.UTF_8.name());

    } catch (UnsupportedEncodingException ex) {
      return null;
    }
  }
}

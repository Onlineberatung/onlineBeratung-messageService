package de.caritas.cob.messageservice.api.helper;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.springframework.web.util.HtmlUtils;

/**
 * Provides escaping of html characters for XSS protection.
 */
public class XssProtection {

  private XssProtection() {
  }

  /**
   * Escape HTML code from a text (XSS-Protection)
   *
   * @return the given text without html
   */
  public static String escapeHtml(String text) {
    if (isNotBlank(text)) {
      return HtmlUtils.htmlEscape(text);
    }
    return EMPTY;
  }

}

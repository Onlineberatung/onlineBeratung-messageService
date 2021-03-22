package de.caritas.cob.messageservice.api.helper;

import static de.caritas.cob.messageservice.api.helper.XssProtection.escapeHtml;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class XssProtectionTest {

  @Test
  public void escapeHtml_Should_escapeStandardHtml() {
    String normalized = escapeHtml("<strong>Lorem Ipsum</strong>");

    assertThat(normalized, is("&lt;strong&gt;Lorem Ipsum&lt;/strong&gt;"));
  }

  @Test
  public void escapeHtml_Should_escapeJavascriptCode() {
    String normalized = escapeHtml("Lorem Ipsum<script>alert('1');</script>");

    assertThat(normalized, is("Lorem Ipsum&lt;script&gt;alert(&#39;1&#39;);&lt;/script&gt;"));
  }

  @Test
  public void escapeHtml_ShouldNot_removeNewlinesFromText() {
    String normalized = escapeHtml("Lorem Ipsum\nLorem Ipsum");

    assertThat(normalized, is("Lorem Ipsum\nLorem Ipsum"));
  }

  @Test
  public void escapeHtml_Should_escapeHtmlFromText_And_ShouldNot_removeNewlines() {
    String normalized = escapeHtml("<b>Lorem Ipsum</b>\nLorem Ipsum<script>alert('1');"
        + "</script>");

    assertThat(normalized, is("&lt;b&gt;Lorem Ipsum&lt;/b&gt;\nLorem Ipsum&lt;script&gt;alert(&#39;1&#39;);&lt;/script&gt;"));
  }

  @Test
  public void removeHTMLFromText_Should_returnEmptyString_When_inputIsNull() {
    String normalized = escapeHtml(null);

    assertThat(normalized, is(""));
  }

  @Test
  public void removeHTMLFromText_Should_returnEmptyString_When_inputIsEmptyString() {
    String normalized = escapeHtml("");

    assertThat(normalized, is(""));
  }

}

package de.caritas.cob.messageservice.api.service.helper;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;

@Component
public class ServiceHelper {

  @Value("${csrf.header.property}")
  private String csrfHeaderProperty;

  @Value("${csrf.cookie.property}")
  private String csrfCookieProperty;

  @Autowired
  private AuthenticatedUser authenticatedUser;

  /**
   * Adds the Rocket.Chat user id, token and group id to the given {@link HttpHeaders} object
   * 
   * @return
   */
  public HttpHeaders getKeycloakAndCsrfHttpHeaders() {
    HttpHeaders header = new HttpHeaders();
    header = this.addCsrfValues(header);

    header.add("Authorization", "Bearer " + authenticatedUser.getAccessToken());

    return header;
  }

  /**
   * Adds CSRF cookie and header value to the given {@link HttpHeaders} object
   * 
   * @param httpHeaders
   * @param csrfToken
   */
  private HttpHeaders addCsrfValues(HttpHeaders httpHeaders) {
    String csrfToken = UUID.randomUUID().toString();

    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.add("Cookie", csrfCookieProperty + "=" + csrfToken);
    httpHeaders.add(csrfHeaderProperty, csrfToken);

    return httpHeaders;
  }

}

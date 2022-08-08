package de.caritas.cob.messageservice.api.service.helper;

import de.caritas.cob.messageservice.api.service.TenantHeaderSupplier;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;

@Component
@Slf4j
public class ServiceHelper {

  @Value("${csrf.header.property}")
  private String csrfHeaderProperty;

  @Value("${csrf.cookie.property}")
  private String csrfCookieProperty;

  @Autowired
  private AuthenticatedUser authenticatedUser;

  @Autowired
  private TenantHeaderSupplier tenantHeaderSupplier;

  /**
   * Adds the Rocket.Chat user id, token and group id to the given {@link HttpHeaders} object
   * 
   * @return
   */
  public HttpHeaders getKeycloakAndCsrfAndOriginHttpHeaders() {
    HttpHeaders headers = new HttpHeaders();
    addCsrfHeaders(headers);
    tenantHeaderSupplier.addTenantHeader(headers);
    addAuthorizationHeader(headers);
    return headers;
  }

  private void addAuthorizationHeader(HttpHeaders headers) {
    headers.add("Authorization", "Bearer " + authenticatedUser.getAccessToken());
  }

  /**
   * Adds CSRF cookie and header value to the given {@link HttpHeaders} object
   * 
   * @param httpHeaders
   * @param csrfToken
   */
  private HttpHeaders addCsrfHeaders(HttpHeaders httpHeaders) {
    String csrfToken = UUID.randomUUID().toString();

    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.add("Cookie", csrfCookieProperty + "=" + csrfToken);
    httpHeaders.add(csrfHeaderProperty, csrfToken);

    return httpHeaders;
  }

}

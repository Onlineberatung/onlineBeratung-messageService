package de.caritas.cob.messageservice.api.service.helper;

import static java.util.Objects.isNull;

import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.service.TenantHeaderSupplier;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

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
  public HttpHeaders getKeycloakAndCsrfAndOriginHttpHeaders(String accessToken,
      Optional<Long> tenantId) {
    HttpHeaders headers = new HttpHeaders();
    addCsrfHeaders(headers);
    tenantHeaderSupplier.addTenantHeader(headers, tenantId);
    addAuthorizationHeader(headers, accessToken);

    return headers;
  }

  private void addAuthorizationHeader(HttpHeaders headers, String accessToken) {
    var token = isNull(accessToken) ? authenticatedUser.getAccessToken() : accessToken;
    headers.add("Authorization", "Bearer " + token);
  }

  /**
   * Adds CSRF cookie and header value to the given {@link HttpHeaders} object
   * 
   * @param httpHeaders headers
   */
  private HttpHeaders addCsrfHeaders(HttpHeaders httpHeaders) {
    String csrfToken = UUID.randomUUID().toString();

    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.add("Cookie", csrfCookieProperty + "=" + csrfToken);
    httpHeaders.add(csrfHeaderProperty, csrfToken);

    return httpHeaders;
  }

}

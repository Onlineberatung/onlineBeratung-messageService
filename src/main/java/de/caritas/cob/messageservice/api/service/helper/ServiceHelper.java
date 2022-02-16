package de.caritas.cob.messageservice.api.service.helper;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
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
  public HttpHeaders getKeycloakAndCsrfAndOriginHttpHeaders() {
    HttpHeaders headers = new HttpHeaders();
    addCsrfHeaders(headers);
    addOriginHeader(headers);
    addAuthorizationHeader(headers);
    return headers;
  }

  private void addAuthorizationHeader(HttpHeaders headers) {
    headers.add("Authorization", "Bearer " + authenticatedUser.getAccessToken());
  }

  private void addOriginHeader(HttpHeaders headers) {
    String originHeaderValue = getOriginHeaderValue();
    log.info("Resolved origin header to {}", originHeaderValue);
    if (originHeaderValue != null) {
      headers.add("origin", originHeaderValue);
    }
  }

  private String getOriginHeaderValue() {
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();

    return Collections.list(request.getHeaderNames())
        .stream()
        .collect(Collectors.toMap(h -> h, request::getHeader)).get("origin");
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

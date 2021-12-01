package de.caritas.cob.messageservice;

import static java.util.Objects.nonNull;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import de.caritas.cob.messageservice.api.exception.KeycloakException;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MessageServiceApplication {

  private static final String CLAIM_NAME_USER_ID = "userId";
  private static final String CLAIM_NAME_USERNAME = "username";

  public static void main(String[] args) {
    SpringApplication.run(MessageServiceApplication.class, args);
  }

  /**
   * Returns the @KeycloakAuthenticationToken which represents the token for a Keycloak
   * authentication.
   *
   * @return KeycloakAuthenticationToken
   */
  @Bean
  @Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
  public KeycloakAuthenticationToken getAccessToken() {
    return (KeycloakAuthenticationToken) getRequest().getUserPrincipal();
  }

  /**
   * Returns the @KeycloakSecurityContext
   *
   * @return KeycloakSecurityContext
   */
  @Bean
  @Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
  public KeycloakSecurityContext getKeycloakSecurityContext() {
    return ((KeycloakAuthenticationToken) getRequest().getUserPrincipal())
        .getAccount()
        .getKeycloakSecurityContext();
  }

  /**
   * Returns the Keycloak user id of the authenticated user
   *
   * @return {@link AuthenticatedUser}
   */
  @Bean
  @Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
  public AuthenticatedUser getAuthenticatedUser() {

    // Get current KeycloakSecurityContext
    KeycloakSecurityContext keycloakSecContext =
        ((KeycloakAuthenticationToken) getRequest().getUserPrincipal())
            .getAccount()
            .getKeycloakSecurityContext();

    Map<String, Object> claimMap = keycloakSecContext.getToken().getOtherClaims();

    AuthenticatedUser authenticatedUser = new AuthenticatedUser();

    if (claimMap.containsKey(CLAIM_NAME_USER_ID)) {
      authenticatedUser.setUserId(claimMap.get(CLAIM_NAME_USER_ID).toString());
    } else {
      throw new KeycloakException(
          "Keycloak user attribute '" + CLAIM_NAME_USER_ID + "' not found.");
    }

    if (claimMap.containsKey(CLAIM_NAME_USERNAME)) {
      authenticatedUser.setUsername(claimMap.get(CLAIM_NAME_USERNAME).toString());
    }

    // Set user roles
    AccessToken.Access realmAccess =
        ((KeycloakAuthenticationToken) getRequest().getUserPrincipal())
            .getAccount()
            .getKeycloakSecurityContext()
            .getToken()
            .getRealmAccess();
    Set<String> roles = realmAccess.getRoles();
    if (CollectionUtils.isNotEmpty(roles)) {
      authenticatedUser.setRoles(roles);
    } else {
      throw new KeycloakException(
          String.format(
              "Keycloak roles null or not set for user: %s", authenticatedUser.getUserId()));
    }

    // Set granted authorities
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    authenticatedUser.setGrantedAuthorities(
        authentication.getAuthorities().stream().map(Object::toString).collect(Collectors.toSet()));

    // Set Keycloak token to authenticated user object
    if (nonNull(keycloakSecContext.getTokenString())) {
      authenticatedUser.setAccessToken(keycloakSecContext.getTokenString());
    } else {
      throw new KeycloakException("No valid Keycloak access token string found.");
    }

    return authenticatedUser;
  }

  private HttpServletRequest getRequest() {
    return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
        .getRequest();
  }
}

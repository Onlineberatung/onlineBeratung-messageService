package de.caritas.cob.MessageService;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import de.caritas.cob.MessageService.api.exception.KeycloakException;
import de.caritas.cob.MessageService.api.helper.AuthenticatedUser;

@SpringBootApplication
@EnableScheduling
public class MessageServiceApplication {

  private final String claimNameUserId = "userId";
  private final String claimNameUsername = "username";

  public static void main(String[] args) {
    SpringApplication.run(MessageServiceApplication.class, args);
  }

  /**
   * Returns the @KeycloakAuthenticationToken which represents the token for a Keycloak
   * authentication.
   * 
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
    return (KeycloakSecurityContext) ((KeycloakAuthenticationToken) getRequest().getUserPrincipal())
        .getAccount().getKeycloakSecurityContext();
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
        ((KeycloakAuthenticationToken) getRequest().getUserPrincipal()).getAccount()
            .getKeycloakSecurityContext();

    Map<String, Object> claimMap = keycloakSecContext.getToken().getOtherClaims();

    AuthenticatedUser authenticatedUser = new AuthenticatedUser();

    if (claimMap.containsKey(claimNameUserId)) {
      authenticatedUser.setUserId(claimMap.get(claimNameUserId).toString());
    } else {
      throw new KeycloakException("Keycloak user attribute '" + claimNameUserId + "' not found.");
    }

    if (claimMap.containsKey(claimNameUsername)) {
      authenticatedUser.setUsername(claimMap.get(claimNameUsername).toString());
    }

    // Set user roles
    AccessToken.Access realmAccess = ((KeycloakAuthenticationToken) getRequest().getUserPrincipal())
        .getAccount().getKeycloakSecurityContext().getToken().getRealmAccess();
    Set<String> roles = realmAccess.getRoles();
    if (roles != null && roles.size() > 0) {
      authenticatedUser.setRoles(roles);
    } else {
      throw new KeycloakException(
          "Keycloak roles null or not set for user: " + authenticatedUser.getUserId() != null
              ? authenticatedUser.getUserId()
              : "unknown");
    }

    // Set granted authorities
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    authenticatedUser.setGrantedAuthorities(authentication.getAuthorities().stream()
        .map(authority -> authority.toString()).collect(Collectors.toSet()));

    // Set Keycloak token to authenticated user object
    if (keycloakSecContext.getTokenString() != null) {
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

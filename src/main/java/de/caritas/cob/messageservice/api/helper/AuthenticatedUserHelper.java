package de.caritas.cob.messageservice.api.helper;

import static de.caritas.cob.messageservice.api.authorization.Role.CONSULTANT;

import java.util.Arrays;
import java.util.Set;

/** Helper methodes for {@link AuthenticatedUser}. */
public class AuthenticatedUserHelper {

  private AuthenticatedUserHelper() {}

  /**
   * Check, if {@link AuthenticatedUser} is consultant.
   *
   * @param authenticatedUser the {@link AuthenticatedUser} instance
   * @return true, if {@link AuthenticatedUser} is consultant.
   */
  public static boolean isConsultant(AuthenticatedUser authenticatedUser) {
    Set<String> roles = authenticatedUser.getRoles();
    return userRolesContainAnyRoleOf(roles, CONSULTANT.getRoleName());
  }

  private static boolean userRolesContainAnyRoleOf(Set<String> userRoles, String... roles) {
    return Arrays.stream(roles).anyMatch(userRoles::contains);
  }
}

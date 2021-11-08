package de.caritas.cob.messageservice.api.authorization;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * All used Keycloak roles.
 */
@Getter
@AllArgsConstructor
public enum Role {

  TECHNICAL("technical"),
  USER("user"),
  CONSULTANT("consultant"),
  ANONYMOUS("anonymous");

  private final String roleName;

  public static Optional<Role> getRoleByName(String roleName) {
    return Arrays.stream(values())
        .filter(userRole -> userRole.roleName.equals(roleName))
        .findFirst();
  }

}

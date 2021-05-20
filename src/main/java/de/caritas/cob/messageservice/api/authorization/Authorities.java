package de.caritas.cob.messageservice.api.authorization;

import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.ANONYMOUS_DEFAULT;
import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.ASSIGN_CONSULTANT_TO_ENQUIRY;
import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.ASSIGN_CONSULTANT_TO_SESSION;
import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.CONSULTANT_DEFAULT;
import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.TECHNICAL_DEFAULT;
import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.USER_DEFAULT;
import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.USE_FEEDBACK;
import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.VIEW_AGENCY_CONSULTANTS;
import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.VIEW_ALL_FEEDBACK_SESSIONS;
import static de.caritas.cob.messageservice.api.authorization.Authorities.Authority.VIEW_ALL_PEER_SESSIONS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Definition of all authorities and of the role-authority-mapping.
 */
@Getter
@AllArgsConstructor
public enum Authorities {

  ANONYMOUS(Role.ANONYMOUS, singletonList(ANONYMOUS_DEFAULT)),
  USER(Role.USER, singletonList(USER_DEFAULT)),
  CONSULTANT(Role.CONSULTANT, singletonList(CONSULTANT_DEFAULT)),
  U25_CONSULTANT(Role.U25_CONSULTANT, singletonList(USE_FEEDBACK)),
  U25_MAIN_CONSULTANT(Role.U25_MAIN_CONSULTANT,
      asList(VIEW_ALL_FEEDBACK_SESSIONS, VIEW_ALL_PEER_SESSIONS, ASSIGN_CONSULTANT_TO_SESSION,
          ASSIGN_CONSULTANT_TO_ENQUIRY, VIEW_AGENCY_CONSULTANTS)),
  TECHNICAL(Role.TECHNICAL, singletonList(TECHNICAL_DEFAULT));

  private final Role userRole;
  private final List<String> grantedAuthorities;

  /**
   * Get all authorities for a specific role.
   *
   * @param userRole the user role
   * @return the related authorities
   */
  public static List<String> getAuthoritiesByUserRole(Role userRole) {
    Optional<Authorities> authorityByUserRole = Stream.of(values())
        .filter(authority -> authority.userRole.equals(userRole))
        .findFirst();

    return authorityByUserRole.isPresent() ? authorityByUserRole.get().getGrantedAuthorities()
        : emptyList();
  }

  public static class Authority {

    private Authority() {
    }

    public static final String PREFIX = "AUTHORIZATION_";

    public static final String CONSULTANT_DEFAULT = PREFIX + "CONSULTANT_DEFAULT";
    public static final String USER_DEFAULT = PREFIX + "USER_DEFAULT";
    public static final String USE_FEEDBACK = PREFIX + "USE_FEEDBACK";
    public static final String VIEW_ALL_FEEDBACK_SESSIONS = PREFIX + "VIEW_ALL_FEEDBACK_SESSIONS";
    public static final String VIEW_ALL_PEER_SESSIONS = PREFIX + "VIEW_ALL_PEER_SESSIONS";
    public static final String ASSIGN_CONSULTANT_TO_SESSION =
        PREFIX + "ASSIGN_CONSULTANT_TO_SESSION";
    public static final String ASSIGN_CONSULTANT_TO_ENQUIRY =
        PREFIX + "ASSIGN_CONSULTANT_TO_ENQUIRY";
    public static final String VIEW_AGENCY_CONSULTANTS = PREFIX + "VIEW_AGENCY_CONSULTANTS";
    public static final String TECHNICAL_DEFAULT = PREFIX + "TECHNICAL_DEFAULT";
    public static final String ANONYMOUS_DEFAULT = PREFIX + "ANONYMOUS_DEFAULT";

  }

}

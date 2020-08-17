package de.caritas.cob.messageservice.api.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * Definition of all authorities and of the role-authority-mapping
 *
 */
public class Authority {

  public static final String PREFIX = "AUTHORIZATION_";

  public static final String CONSULTANT_DEFAULT = PREFIX + "CONSULTANT_DEFAULT";
  public static final String USER_DEFAULT = PREFIX + "USER_DEFAULT";
  public static final String USE_FEEDBACK = PREFIX + "USE_FEEDBACK";
  public static final String VIEW_ALL_FEEDBACK_SESSIONS = PREFIX + "VIEW_ALL_FEEDBACK_SESSIONS";
  public static final String VIEW_ALL_PEER_SESSIONS = PREFIX + "VIEW_ALL_PEER_SESSIONS";
  public static final String ASSIGN_CONSULTANT_TO_SESSION = PREFIX + "ASSIGN_CONSULTANT_TO_SESSION";
  public static final String ASSIGN_CONSULTANT_TO_ENQUIRY = PREFIX + "ASSIGN_CONSULTANT_TO_ENQUIRY";
  public static final String VIEW_AGENCY_CONSULTANTS = PREFIX + "VIEW_AGENCY_CONSULTANTS";
  public static final String TECHNICAL_DEFAULT = PREFIX + "TECHNICAL_DEFAULT";

  /**
   * Get all authorities for a specific role
   * 
   * @param roleName
   * @return
   */
  public static List<String> getAuthoritiesByRoleName(String roleName) {
    if (authorizationRoleMapping.containsKey(roleName)) {
      return authorizationRoleMapping.get(roleName);
    } else {
      return new ArrayList<String>();
    }

  }

  private static final Map<String, List<String>> authorizationRoleMapping =
      new HashMap<String, List<String>>() {

        private static final long serialVersionUID = -4293306706967206011L;

        {
          put(Role.USER, Arrays.asList(USER_DEFAULT));
          put(Role.CONSULTANT, Arrays.asList(CONSULTANT_DEFAULT));
          put(Role.U25_CONSULTANT, Arrays.asList(USE_FEEDBACK));
          put(Role.U25_MAIN_CONSULTANT,
              Arrays.asList(VIEW_ALL_FEEDBACK_SESSIONS, VIEW_ALL_PEER_SESSIONS,
                  ASSIGN_CONSULTANT_TO_SESSION, ASSIGN_CONSULTANT_TO_ENQUIRY,
                  VIEW_AGENCY_CONSULTANTS));
          put(Role.TECHNICAL, Arrays.asList(TECHNICAL_DEFAULT));
        }
      };

}

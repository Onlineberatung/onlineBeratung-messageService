package de.caritas.cob.messageservice.api.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthorityTest {

  @Test
  public void getAuthoritiesByRoleName_Should_ReturnCorrectRoles_ForKeycloakRoleConsultant() {

    List<String> result = Authority.getAuthoritiesByUserRole(Role.CONSULTANT);

    assertNotNull(result);
    assertTrue(result.contains(AuthorityValue.CONSULTANT_DEFAULT));
    assertEquals(1, result.size());

  }

  @Test
  public void getAuthoritiesByRoleName_Should_ReturnCorrectRoles_ForKeycloakRoleUser() {

    List<String> result = Authority.getAuthoritiesByUserRole(Role.USER);

    assertNotNull(result);
    assertTrue(result.contains(AuthorityValue.USER_DEFAULT));
    assertEquals(1, result.size());

  }

  @Test
  public void getAuthoritiesByRoleName_Should_ReturnCorrectRoles_ForKeycloakRoleTechnical() {

    List<String> result = Authority.getAuthoritiesByUserRole(Role.TECHNICAL);

    assertNotNull(result);
    assertTrue(result.contains(AuthorityValue.TECHNICAL_DEFAULT));
    assertEquals(1, result.size());

  }

}

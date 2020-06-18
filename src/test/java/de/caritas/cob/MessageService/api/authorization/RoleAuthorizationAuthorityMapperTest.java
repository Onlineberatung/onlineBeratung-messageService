package de.caritas.cob.MessageService.api.authorization;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import de.caritas.cob.MessageService.api.authorization.Authority;
import de.caritas.cob.MessageService.api.authorization.Role;
import de.caritas.cob.MessageService.api.authorization.RoleAuthorizationAuthorityMapper;

@RunWith(MockitoJUnitRunner.class)
public class RoleAuthorizationAuthorityMapperTest {

  private KeycloakAuthenticationProvider provider = new KeycloakAuthenticationProvider();
  private Set<String> roles =
      Sets.newSet(Role.CONSULTANT, Role.U25_CONSULTANT, Role.U25_MAIN_CONSULTANT);

  @Test
  public void roleAuthorizationAuthorityMapper_Should_GrantCorrectAuthorities() throws Exception {

    Principal principal = mock(Principal.class);
    RefreshableKeycloakSecurityContext securityContext =
        mock(RefreshableKeycloakSecurityContext.class);
    KeycloakAccount account = new SimpleKeycloakAccount(principal, roles, securityContext);

    KeycloakAuthenticationToken token = new KeycloakAuthenticationToken(account, false);

    RoleAuthorizationAuthorityMapper roleAuthorizationAuthorityMapper =
        new RoleAuthorizationAuthorityMapper();
    provider.setGrantedAuthoritiesMapper(roleAuthorizationAuthorityMapper);

    Authentication result = provider.authenticate(token);

    List<SimpleGrantedAuthority> expectedGrantendAuthorities =
        new ArrayList<SimpleGrantedAuthority>();
    roles.forEach(roleName -> {
      expectedGrantendAuthorities.addAll(Authority.getAuthoritiesByRoleName(roleName).stream()
          .map(authority -> new SimpleGrantedAuthority(authority)).collect(Collectors.toList()));
    });

    assertThat(expectedGrantendAuthorities, containsInAnyOrder(result.getAuthorities().toArray()));

  }

}

package de.caritas.cob.MessageService.api.service.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import de.caritas.cob.MessageService.api.helper.AuthenticatedUser;

@RunWith(MockitoJUnitRunner.class)
public class ServiceHelperTest {

  private final String FIELD_NAME_CSRF_TOKEN_HEADER_PROPERTY = "csrfHeaderProperty";
  private final String FIELD_NAME_CSRF_TOKEN_COOKIE_PROPERTY = "csrfCookieProperty";
  private final String CSRF_TOKEN_HEADER_VALUE = "X-CSRF-TOKEN";
  private final String CSRF_TOKEN_COOKIE_VALUE = "CSRF-TOKEN";
  private final String AUTHORIZATION = "Authorization";

  @Mock
  private AuthenticatedUser authenticatedUser;
  @InjectMocks
  private ServiceHelper serviceHelper;

  @Before
  public void setup() throws NoSuchFieldException, SecurityException {
    FieldSetter.setField(serviceHelper,
        serviceHelper.getClass().getDeclaredField(FIELD_NAME_CSRF_TOKEN_HEADER_PROPERTY),
        CSRF_TOKEN_HEADER_VALUE);
    FieldSetter.setField(serviceHelper,
        serviceHelper.getClass().getDeclaredField(FIELD_NAME_CSRF_TOKEN_COOKIE_PROPERTY),
        CSRF_TOKEN_COOKIE_VALUE);
  }

  /**
   * 
   * Tests for method: getKeycloakAndCsrfHttpHeaders
   *
   */

  @Test
  public void getKeycloakAndCsrfHttpHeaders_Should_Return_HeaderWithCorrectContentType() {

    HttpHeaders result = serviceHelper.getKeycloakAndCsrfHttpHeaders();
    assertEquals(MediaType.APPLICATION_JSON_UTF8, result.getContentType());

  }

  @Test
  public void getKeycloakAndCsrfHttpHeaders_Should_Return_HeaderWithCookiePropertyNameFromProperties() {

    HttpHeaders result = serviceHelper.getKeycloakAndCsrfHttpHeaders();
    assertTrue(result.get("Cookie").toString().startsWith("[" + CSRF_TOKEN_COOKIE_VALUE + "="));

  }

  @Test
  public void getKeycloakAndCsrfHttpHeaders_Should_Return_HeaderWithPropertyNameFromProperties() {

    HttpHeaders result = serviceHelper.getKeycloakAndCsrfHttpHeaders();
    assertNotNull(result.get(CSRF_TOKEN_HEADER_VALUE));

  }

  @Test
  public void getKeycloakAndCsrfHttpHeaders_Should_Return_HeaderWithBearerAuthorization() {

    HttpHeaders result = serviceHelper.getKeycloakAndCsrfHttpHeaders();
    assertNotNull(result.get(AUTHORIZATION));

  }

}

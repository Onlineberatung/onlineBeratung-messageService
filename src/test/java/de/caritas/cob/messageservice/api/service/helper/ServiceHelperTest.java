package de.caritas.cob.messageservice.api.service.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RunWith(MockitoJUnitRunner.class)
public class ServiceHelperTest {

  private final String FIELD_NAME_CSRF_TOKEN_HEADER_PROPERTY = "csrfHeaderProperty";
  private final String FIELD_NAME_CSRF_TOKEN_COOKIE_PROPERTY = "csrfCookieProperty";
  private final String CSRF_TOKEN_HEADER_VALUE = "X-CSRF-TOKEN";
  private final String CSRF_TOKEN_COOKIE_VALUE = "CSRF-TOKEN";
  private final String AUTHORIZATION = "Authorization";

  @Mock
  private AuthenticatedUser authenticatedUser;

  private HttpServletRequest httpServletRequest  = new MockHttpServletRequest();

  private ServletRequestAttributes requestAttributes = new ServletRequestAttributes(httpServletRequest);

  @InjectMocks
  private ServiceHelper serviceHelper;

  @Mock
  private Enumeration<String> headers;

  @Before
  public void setup() throws NoSuchFieldException, SecurityException {
    givenRequestContextIsSet();
    FieldSetter.setField(serviceHelper,
        serviceHelper.getClass().getDeclaredField(FIELD_NAME_CSRF_TOKEN_HEADER_PROPERTY),
        CSRF_TOKEN_HEADER_VALUE);
    FieldSetter.setField(serviceHelper,
        serviceHelper.getClass().getDeclaredField(FIELD_NAME_CSRF_TOKEN_COOKIE_PROPERTY),
        CSRF_TOKEN_COOKIE_VALUE);
  }

  private void givenRequestContextIsSet() {
    RequestContextHolder.setRequestAttributes(requestAttributes);
  }

  /**
   * Tests for method: getKeycloakAndCsrfHttpHeaders
   */

  @Test
  public void getKeycloakAndCsrfHttpHeaders_Should_Return_HeaderWithCorrectContentType() {

    HttpHeaders result = serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders();
    assertEquals(MediaType.APPLICATION_JSON_UTF8, result.getContentType());

  }

  @Test
  public void getKeycloakAndCsrfHttpHeaders_Should_Return_HeaderWithCookiePropertyNameFromProperties() {

    HttpHeaders result = serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders();
    assertTrue(result.get("Cookie").toString().startsWith("[" + CSRF_TOKEN_COOKIE_VALUE + "="));

  }

  @Test
  public void getKeycloakAndCsrfHttpHeaders_Should_Return_HeaderWithPropertyNameFromProperties() {

    HttpHeaders result = serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders();
    assertNotNull(result.get(CSRF_TOKEN_HEADER_VALUE));

  }

  @Test
  public void getKeycloakAndCsrfHttpHeaders_Should_Return_HeaderWithBearerAuthorization() {

    HttpHeaders result = serviceHelper.getKeycloakAndCsrfAndOriginHttpHeaders();
    assertNotNull(result.get(AUTHORIZATION));

  }

}

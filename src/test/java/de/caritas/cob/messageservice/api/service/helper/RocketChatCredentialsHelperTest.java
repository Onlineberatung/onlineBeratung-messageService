package de.caritas.cob.messageservice.api.service.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import de.caritas.cob.messageservice.api.exception.RocketChatUserNotInitializedException;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.login.DataDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.login.LoginResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.logout.LogoutResponseDTO;

@RunWith(MockitoJUnitRunner.class)
public class RocketChatCredentialsHelperTest {

  /**
   * FIELD Names
   */

  private final static String FIELD_NAME_SYSTEM_USER_A = "systemUserA";
  private final static String FIELD_NAME_SYSTEM_USER_B = "systemUserB";

  private final static String FIELD_NAME_SYSTEM_USERNAME = "systemUsername";
  private final static String FIELD_NAME_SYSTEM_PASSWORD = "systemPassword";

  private final static String FIELD_NAME_ROCKET_CHAT_HEADER_AUTH_TOKEN =
      "rocketChatHeaderAuthToken";
  private final static String FIELD_VALUE_ROCKET_CHAT_HEADER_AUTH_TOKEN = "X-Auth-Token";
  private final static String FIELD_NAME_ROCKET_CHAT_HEADER_USER_ID = "rocketChatHeaderUserId";
  private final static String FIELD_VALUE_ROCKET_CHAT_HEADER_USER_ID = "X-User-Id";

  private final String FIELD_NAME_ROCKET_CHAT_API_POST_USER_LOGIN = "rocketChatApiUserLogin";
  private final String FIELD_NAME_ROCKET_CHAT_API_POST_USER_LOGOUT = "rocketChatApiUserLogout";

  /**
   * DATA
   */

  private final String RC_URL_CHAT_USER_LOGIN = "http://localhost/api/v1/login";
  private final String RC_URL_CHAT_USER_LOGOUT = "http://localhost/api/v1/logout";

  MultiValueMap<String, String> MULTI_VALUE_MAP_WITH_SYSTEM_USER_CREDENTIALS =
      new LinkedMultiValueMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
          add("username", SYSTEM_USER_USERNAME);
          add("password", SYSTEM_USER_PW);
        }
      };

  LoginResponseDTO LOGIN_RESPONSE_DTO_SYSTEM_USER_A =
      new LoginResponseDTO("status", new DataDTO(SYSTEM_USER_A_ID, SYSTEM_USER_A_TOKEN, null));

  LoginResponseDTO LOGIN_RESPONSE_DTO_SYSTEM_USER_B =
      new LoginResponseDTO("status", new DataDTO(SYSTEM_USER_B_ID, SYSTEM_USER_B_TOKEN, null));


  LoginResponseDTO LOGIN_RESPONSE_DTO_TECHNICAL_USER_A = new LoginResponseDTO("status",
      new DataDTO(TECHNICAL_USER_A_ID, TECHNICAL_USER_A_TOKEN, null));

  LoginResponseDTO LOGIN_RESPONSE_DTO_TECHNICAL_USER_B = new LoginResponseDTO("status",
      new DataDTO(TECHNICAL_USER_B_ID, TECHNICAL_USER_B_TOKEN, null));

  LogoutResponseDTO LOGOUT_RESPONSE_DTO_SYSTEM_USER_A = new LogoutResponseDTO("status",
      SYSTEM_USER_USERNAME, new de.caritas.cob.messageservice.api.model.rocket.chat.logout.DataDTO(
          SYSTEM_USER_A_USERNAME));

  private final static String TECHNICAL_USER_USERNAME = "techUserName";
  private final static String TECHNICAL_USER_PW = "techUserPW";

  private final static String SYSTEM_USER_USERNAME = "sysUserName";
  private final static String SYSTEM_USER_PW = "sysUserPW";

  private final static String TECHNICAL_USER_A_USERNAME = "techUserAName";
  private final static String TECHNICAL_USER_A_TOKEN = "techUserAToken";
  private final static String TECHNICAL_USER_A_ID = "techUserAID";

  private final static String TECHNICAL_USER_B_USERNAME = "techUserBName";
  private final static String TECHNICAL_USER_B_TOKEN = "techUserBToken";
  private final static String TECHNICAL_USER_B_ID = "techUserBID";

  private final static String SYSTEM_USER_A_USERNAME = "sysUserAName";
  private final static String SYSTEM_USER_A_TOKEN = "sysUserAToken";
  private final static String SYSTEM_USER_A_ID = "sysUserAID";

  private final static String SYSTEM_USER_B_USERNAME = "sysUserBName";
  private final static String SYSTEM_USER_B_TOKEN = "sysUserBToken";
  private final static String SYSTEM_USER_B_ID = "sysUserBID";

  @InjectMocks
  private RocketChatCredentialsHelper rcCredentialHelper;

  @Mock
  private RestTemplate restTemplate;

  @Before
  public void setup() throws NoSuchFieldException {
    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USERNAME, SYSTEM_USER_USERNAME);
    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_PASSWORD, SYSTEM_USER_PW);
    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_ROCKET_CHAT_API_POST_USER_LOGIN, RC_URL_CHAT_USER_LOGIN);
    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_ROCKET_CHAT_API_POST_USER_LOGOUT, RC_URL_CHAT_USER_LOGOUT);
    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_ROCKET_CHAT_HEADER_AUTH_TOKEN, FIELD_VALUE_ROCKET_CHAT_HEADER_AUTH_TOKEN);
    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_ROCKET_CHAT_HEADER_USER_ID, FIELD_VALUE_ROCKET_CHAT_HEADER_USER_ID);
  }

  /**
   *
   * Method: updateCredentials
   *
   **/

  @Test
  public void updateCredentials_Should_LoginAUsers_WhenNoUsersAreLoggedIn()
      throws RocketChatUserNotInitializedException {
    // Prepare Header for Requests
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // Prepare intercept login system user
    HttpEntity<MultiValueMap<String, String>> requestSys =
        new HttpEntity<MultiValueMap<String, String>>(MULTI_VALUE_MAP_WITH_SYSTEM_USER_CREDENTIALS,
            headers);

    when(restTemplate.postForEntity(ArgumentMatchers.eq(RC_URL_CHAT_USER_LOGIN),
        ArgumentMatchers.eq(requestSys), ArgumentMatchers.<Class<LoginResponseDTO>>any()))
            .thenReturn(new ResponseEntity<LoginResponseDTO>(LOGIN_RESPONSE_DTO_SYSTEM_USER_A,
                HttpStatus.OK));

    // Execute test
    rcCredentialHelper.updateCredentials();

    // Get and check system user
    RocketChatCredentials systemUser = rcCredentialHelper.getSystemUser();
    assertNotNull(systemUser);
    assertEquals(SYSTEM_USER_A_ID, systemUser.getRocketChatUserId());
    assertEquals(SYSTEM_USER_A_TOKEN, systemUser.getRocketChatToken());

  }

  @Test
  public void updateCredentials_Should_LoginBUsers_WhenAUsersAreLoggedIn()
      throws NoSuchFieldException, RocketChatUserNotInitializedException {
    // Prepare Header for Requests
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // Prepare intercept login system user
    HttpEntity<MultiValueMap<String, String>> requestSys =
        new HttpEntity<MultiValueMap<String, String>>(MULTI_VALUE_MAP_WITH_SYSTEM_USER_CREDENTIALS,
            headers);

    when(restTemplate.postForEntity(ArgumentMatchers.eq(RC_URL_CHAT_USER_LOGIN),
        ArgumentMatchers.eq(requestSys), ArgumentMatchers.<Class<LoginResponseDTO>>any()))
            .thenReturn(new ResponseEntity<LoginResponseDTO>(LOGIN_RESPONSE_DTO_SYSTEM_USER_B,
                HttpStatus.OK));

    RocketChatCredentials systemA = new RocketChatCredentials(SYSTEM_USER_A_TOKEN, SYSTEM_USER_A_ID,
        SYSTEM_USER_A_USERNAME, LocalDateTime.now().minusMinutes(5));
    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USER_A, systemA);

    // Get and check system user - pre test needs to be User A
    RocketChatCredentials systemUser = rcCredentialHelper.getSystemUser();
    assertNotNull(systemUser);
    assertEquals(SYSTEM_USER_A_ID, systemUser.getRocketChatUserId());
    assertEquals(SYSTEM_USER_A_TOKEN, systemUser.getRocketChatToken());

    // Execute test
    rcCredentialHelper.updateCredentials();

    // Get and check system user - post test needs to be User B since he is newer
    systemUser = rcCredentialHelper.getSystemUser();
    assertNotNull(systemUser);
    assertEquals(SYSTEM_USER_B_ID, systemUser.getRocketChatUserId());
    assertEquals(SYSTEM_USER_B_TOKEN, systemUser.getRocketChatToken());

  }

  @Test
  public void updateCredentials_Should_LogoutAndReLoginBUsers_WhenAllUsersArePresent()
      throws NoSuchFieldException, RocketChatUserNotInitializedException {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // create and set system A user
    RocketChatCredentials systemA = new RocketChatCredentials(SYSTEM_USER_A_TOKEN, SYSTEM_USER_A_ID,
        SYSTEM_USER_A_USERNAME, LocalDateTime.now().minusMinutes(5));
    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USER_A, systemA);


    // create and set system B user
    RocketChatCredentials systemB = new RocketChatCredentials(SYSTEM_USER_B_TOKEN, SYSTEM_USER_B_ID,
        SYSTEM_USER_B_USERNAME, LocalDateTime.now().minusMinutes(1));
    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USER_B, systemB);

    // prepare logout intercept for system user
    HttpHeaders headersLogoutSys = new HttpHeaders();
    headersLogoutSys.setContentType(MediaType.APPLICATION_JSON_UTF8);
    headersLogoutSys.add(FIELD_VALUE_ROCKET_CHAT_HEADER_AUTH_TOKEN, SYSTEM_USER_A_TOKEN);
    headersLogoutSys.add(FIELD_VALUE_ROCKET_CHAT_HEADER_USER_ID, SYSTEM_USER_A_ID);
    HttpEntity<Void> requestSysLogout = new HttpEntity<Void>(headersLogoutSys);
    when(restTemplate.postForEntity(ArgumentMatchers.eq(RC_URL_CHAT_USER_LOGOUT),
        ArgumentMatchers.eq(requestSysLogout), ArgumentMatchers.<Class<LogoutResponseDTO>>any()))
            .thenReturn(new ResponseEntity<LogoutResponseDTO>(LOGOUT_RESPONSE_DTO_SYSTEM_USER_A,
                HttpStatus.OK));

    // prepare login intercept for system user
    HttpEntity<MultiValueMap<String, String>> requestSysLogin =
        new HttpEntity<MultiValueMap<String, String>>(MULTI_VALUE_MAP_WITH_SYSTEM_USER_CREDENTIALS,
            headers);
    when(restTemplate.postForEntity(ArgumentMatchers.eq(RC_URL_CHAT_USER_LOGIN),
        ArgumentMatchers.eq(requestSysLogin), ArgumentMatchers.<Class<LoginResponseDTO>>any()))
            .thenReturn(new ResponseEntity<LoginResponseDTO>(LOGIN_RESPONSE_DTO_SYSTEM_USER_B,
                HttpStatus.OK));

    // Execute test
    rcCredentialHelper.updateCredentials();

    // get system user and ensure it is a new one
    RocketChatCredentials systemUser = rcCredentialHelper.getSystemUser();
    assertNotEquals(systemA.getTimeStampCreated(), systemUser.getTimeStampCreated());
    assertNotEquals(systemB.getTimeStampCreated(), systemUser.getTimeStampCreated());

    // ensure logout interception was called
    verify(restTemplate, times(1)).postForEntity(RC_URL_CHAT_USER_LOGOUT, requestSysLogout,
        LogoutResponseDTO.class);
    // ensure login interception was called
    verify(restTemplate, times(1)).postForEntity(RC_URL_CHAT_USER_LOGIN, requestSysLogin,
        LoginResponseDTO.class);

  }

  /**
   *
   * Method: getSystemUser
   *
   **/

  @Test
  public void getSystemUser_Should_ThrowRocketChatUserNotInitializedException_WhenNoUserIsInitialized() {
    try {
      rcCredentialHelper.getSystemUser();
      fail("Expected exception: RocketChatUserNotInitializedException");
    } catch (RocketChatUserNotInitializedException ex) {
      assertTrue("Excepted RocketChatUserNotInitializedException thrown", true);
    }
  }

  @Test
  public void getSystemUser_Should_ReturnUserA_WhenOnlyUserAIsInitialized()
      throws NoSuchFieldException, RocketChatUserNotInitializedException {

    RocketChatCredentials sysUserA = new RocketChatCredentials(SYSTEM_USER_A_ID,
        SYSTEM_USER_A_TOKEN, SYSTEM_USER_A_USERNAME, LocalDateTime.now());

    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USER_A, sysUserA);

    RocketChatCredentials systemUser = rcCredentialHelper.getSystemUser();

    assertEquals(sysUserA, systemUser);
  }

  @Test
  public void getSystemUser_Should_ReturnUserB_WhenOnlyUserBIsInitialized()
      throws NoSuchFieldException, RocketChatUserNotInitializedException {

    RocketChatCredentials sysUserB = new RocketChatCredentials(SYSTEM_USER_B_ID,
        SYSTEM_USER_B_TOKEN, SYSTEM_USER_B_USERNAME, LocalDateTime.now());

    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USER_B, sysUserB);

    RocketChatCredentials systemUser = rcCredentialHelper.getSystemUser();

    assertEquals(sysUserB, systemUser);
  }

  @Test
  public void getSystemUser_Should_ReturnUserA_WhenUserAIsNewer()
      throws NoSuchFieldException, RocketChatUserNotInitializedException {

    // Prepare User A
    RocketChatCredentials sysUserA = new RocketChatCredentials(SYSTEM_USER_A_ID,
        SYSTEM_USER_A_TOKEN, SYSTEM_USER_A_USERNAME, LocalDateTime.now());

    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USER_A, sysUserA);

    // Prepare User B - 5 minutes older than User A
    RocketChatCredentials sysUserB = new RocketChatCredentials(SYSTEM_USER_B_ID,
        SYSTEM_USER_B_TOKEN, SYSTEM_USER_B_USERNAME, LocalDateTime.now().minusMinutes(5));

    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USER_B, sysUserB);

    // Get User from Class (actual test)
    RocketChatCredentials systemUser = rcCredentialHelper.getSystemUser();

    // Verify it is User A since he is newer
    assertEquals(sysUserA, systemUser);
  }

  @Test
  public void getSystemUser_Should_ReturnUserB_WhenUserBIsNewer()
      throws NoSuchFieldException, RocketChatUserNotInitializedException {

    // Prepare User A - 5 minutes older than User B
    RocketChatCredentials sysUserA = new RocketChatCredentials(SYSTEM_USER_A_ID,
        SYSTEM_USER_A_TOKEN, SYSTEM_USER_A_USERNAME, LocalDateTime.now().minusMinutes(5));

    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USER_A, sysUserA);

    // Prepare User B
    RocketChatCredentials sysUserB = new RocketChatCredentials(SYSTEM_USER_B_ID,
        SYSTEM_USER_B_TOKEN, SYSTEM_USER_B_USERNAME, LocalDateTime.now());

    Whitebox.setInternalState(rcCredentialHelper, FIELD_NAME_SYSTEM_USER_B, sysUserB);

    // Get User from Class (actual test)
    RocketChatCredentials systemUser = rcCredentialHelper.getSystemUser();

    // Verify it is User A since he is newer
    assertEquals(sysUserB, systemUser);
  }

}

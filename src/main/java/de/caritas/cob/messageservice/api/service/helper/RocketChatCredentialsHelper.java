package de.caritas.cob.messageservice.api.service.helper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatUserNotInitializedException;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.login.LoginResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.logout.LogoutResponseDTO;
import de.caritas.cob.messageservice.api.service.LogService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class RocketChatCredentialsHelper {

  @Value("${rocket.systemuser.username}")
  private String systemUsername;

  @Value("${rocket.systemuser.password}")
  private String systemPassword;

  @Value("${rocket.chat.api.user.login}")
  private String rocketChatApiUserLogin;

  @Value("${rocket.chat.api.user.logout}")
  private String rocketChatApiUserLogout;

  @Value("${rocket.chat.header.auth.token}")
  private String rocketChatHeaderAuthToken;

  @Value("${rocket.chat.header.user.id}")
  private String rocketChatHeaderUserId;

  @Autowired
  private RestTemplate restTemplate;

  // Tokens
  private RocketChatCredentials systemUserA;
  private RocketChatCredentials systemUserB;

  /**
   * Get a valid system user.
   * 
   * @return the credentials for system user
   */
  public RocketChatCredentials getSystemUser() throws RocketChatUserNotInitializedException {
    // If both are uninitialized throw Exception
    if (areBothSystemUsersNull()) {
      throw new RocketChatUserNotInitializedException("No system user was initialized");
    }

    if (isOneOfSystemUsersNull()) {
      return getNonNullSystemUser();
    }

    return getNewestCreatedSystemUser();
  }

  private boolean areBothSystemUsersNull() {
    return isNull(systemUserA) && isNull(systemUserB);
  }

  private boolean isOneOfSystemUsersNull() {
    return isNull(systemUserA) || isNull(systemUserB);
  }

  private RocketChatCredentials getNonNullSystemUser() {
    return nonNull(systemUserA) ? systemUserA : systemUserB;
  }

  private RocketChatCredentials getNewestCreatedSystemUser() {
    if (systemUserA.getTimeStampCreated().isAfter(systemUserB.getTimeStampCreated())) {
      return systemUserA;
    } else {
      return systemUserB;
    }
  }

  /**
   * Update the Credentials.
   *
   */
  public void updateCredentials() {

    if (!isOneOfSystemUsersNull()) {
      if (systemUserA.getTimeStampCreated().isBefore(systemUserB.getTimeStampCreated())) {
        logoutUser(systemUserA);
        systemUserA = null;
      } else {
        logoutUser(systemUserB);
        systemUserB = null;
      }
    }

    if (areBothSystemUsersNull()) {
      systemUserA = loginUserServiceUser(systemUsername, systemPassword);
    } else {
      if (systemUserA == null) {
        systemUserA = loginUserServiceUser(systemUsername, systemPassword);
      }

      if (systemUserB == null) {
        systemUserB = loginUserServiceUser(systemUsername, systemPassword);
      }
    }

  }

  private RocketChatCredentials loginUserServiceUser(String username, String password) {

    RocketChatCredentials rcc = new RocketChatCredentials();
    rcc.setTimeStampCreated(LocalDateTime.now());
    rcc.setRocketChatUsername(username);

    try {

      ResponseEntity<LoginResponseDTO> response = loginUser(username, password);

      rcc.setRocketChatToken(response.getBody().getData().getAuthToken());
      rcc.setRocketChatUserId(response.getBody().getData().getUserId());

    } catch (Exception ex) {
      throw new InternalServerErrorException("Could not login " + username + " user in Rocket"
          + ".Chat", LogService::logRocketChatServiceError);
    }

    if (rcc.getRocketChatToken() == null || rcc.getRocketChatUserId() == null) {
      String error = "Could not login " + username
          + " user in Rocket.Chat correctly, no authToken or UserId received.";
      throw new InternalServerErrorException(error, LogService::logRocketChatServiceError);
    }

    return rcc;
  }

  private ResponseEntity<LoginResponseDTO> loginUser(String username, String password) {

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add("username", username);
      map.add("password", password);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

      return restTemplate.postForEntity(rocketChatApiUserLogin, request, LoginResponseDTO.class);
    } catch (Exception ex) {
      throw new InternalServerErrorException(String.format("Could not login user (%s) in Rocket"
          + ".Chat", username), LogService::logRocketChatServiceError);
    }
  }

  private void logoutUser(String rcUserId, String rcAuthToken) {

    try {
      HttpHeaders headers = getStandardHttpHeaders(rcAuthToken, rcUserId);

      HttpEntity<Void> request = new HttpEntity<>(headers);
      restTemplate.postForEntity(rocketChatApiUserLogout, request, LogoutResponseDTO.class);

    } catch (Exception ex) {
      LogService.logRocketChatServiceError(
          String.format("Could not log out user id (%s) from Rocket.Chat", rcUserId), ex);
    }
  }

  private void logoutUser(RocketChatCredentials user) {
    this.logoutUser(user.getRocketChatUserId(), user.getRocketChatToken());
  }

  private HttpHeaders getStandardHttpHeaders(String rcToken, String rcUserId) {

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.add(rocketChatHeaderAuthToken, rcToken);
    httpHeaders.add(rocketChatHeaderUserId, rcUserId);
    return httpHeaders;
  }

}

package de.caritas.cob.MessageService.api.service;

import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import de.caritas.cob.MessageService.api.exception.RocketChatBadRequestException;
import de.caritas.cob.MessageService.api.exception.RocketChatGetGroupMessagesException;
import de.caritas.cob.MessageService.api.exception.RocketChatPostMarkGroupAsReadException;
import de.caritas.cob.MessageService.api.exception.RocketChatPostMessageException;
import de.caritas.cob.MessageService.api.model.MessageStreamDTO;
import de.caritas.cob.MessageService.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.MessageService.api.model.rocket.chat.StandardResponseDTO;
import de.caritas.cob.MessageService.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.MessageService.api.model.rocket.chat.message.PostMessageResponseDTO;
import de.caritas.cob.MessageService.api.service.helper.RocketChatCredentialsHelper;

@RunWith(MockitoJUnitRunner.class)
public class RocketChatServiceTest {

  private final String FIELD_NAME_SYSUSER_AUTH_TOKEN = "systemUserAuthToken";
  private final String FIELD_VALUE_SYSUSER_AUTH_TOKEN = "d45zerthzdgh";
  private final String FIELD_NAME_SYSUSER_ID = "systemUserId";
  private final String FIELD_VALUE_SYSUSER_ID = "d45zer34rthzdgh";
  private final String FIELD_NAME_RC_POST_GROUP_MESSAGES_READ = "RC_POST_GROUP_MESSAGES_READ";
  private final String FIELD_VALUE_RC_POST_GROUP_MESSAGES_READ = "/api/v1/subscriptions.read";
  private final String FIELD_NAME_RC_POST_USER_LOGIN_URL = "RC_POST_USER_LOGIN_URL";
  private final String FIELD_VALUE_RC_POST_USER_LOGIN_URL = "/api/v1/login";
  private final String RC_TOKEN = "r94qMDk8gtgVNzqCq9zD2hELK-eXGB5VHlUVBgE8a8f";
  private final String RC_USER_ID = "pptLwARyTMzbTTRdg";
  private final String RC_GROUP_ID = "fR2Rz7dmWmHdXE8uz";
  private final int RC_OFFSET = 0;
  private final int RC_COUNT = 0;
  private final String RC_MESSAGE = "Lorem ipsum";
  private final String ERROR_MSG = "error";
  private StandardResponseDTO STANDARD_SUCCESS_RESPONSE_DTO = new StandardResponseDTO(true, null);
  private final String RC_SYSTEM_USERNAME = "system";
  private final String RC_SYSTEM_USER_ID = "systemId";
  private final String RC_SYSTEM_USER_AUTH_TOKEN = "systemToken";
  private RocketChatCredentials RCC_SYSTEM_USER = new RocketChatCredentials(
      RC_SYSTEM_USER_AUTH_TOKEN, RC_SYSTEM_USER_ID, RC_SYSTEM_USERNAME, null);
  private RocketChatCredentials INVALID_RCC_SYSTEM_USER =
      new RocketChatCredentials(null, null, null, null);

  @InjectMocks
  private RocketChatService rocketChatService;
  @Mock
  private RestTemplate restTemplate;

  @Mock
  private LogService logService;

  @Mock
  private EncryptionService encryptionService;

  @Mock
  RocketChatCredentialsHelper rcCredentialsHelper;

  @Before
  public void setup() throws NoSuchFieldException, SecurityException {
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_HEADER_AUTH_TOKEN"),
        String.valueOf(RC_TOKEN));
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_HEADER_USER_ID"),
        String.valueOf(RC_USER_ID));
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_QUERY_PARAM_ROOM_ID"), RC_GROUP_ID);
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_QUERY_PARAM_ROOM_ID"), RC_GROUP_ID);
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_QUERY_PARAM_OFFSET"),
        String.valueOf(RC_OFFSET));
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_QUERY_PARAM_COUNT"),
        String.valueOf(RC_COUNT));
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_QUERY_PARAM_SORT"), "sort");
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_QUERY_PARAM_SORT_VALUE"), "{\"ts\":1}");
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_GET_GROUP_MESSAGE_URL"),
        "http://localhost/api/v1/groups.messages");
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("RC_POST_MESSAGE_URL"),
        "http://localhost/api/v1/chat.postMessage");
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField(FIELD_NAME_RC_POST_GROUP_MESSAGES_READ),
        FIELD_VALUE_RC_POST_GROUP_MESSAGES_READ);
    FieldSetter.setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField(FIELD_NAME_RC_POST_USER_LOGIN_URL),
        FIELD_VALUE_RC_POST_USER_LOGIN_URL);
  }

  /**
   * Exception tests
   *
   */

  @Test
  public void getGroupMessages_Should_ReturnRocketChatGetGroupMessagesException_WhenHTTPRequestFails()
      throws Exception {

    RocketChatGetGroupMessagesException ex = new RocketChatGetGroupMessagesException("reason");

    when(restTemplate.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
        ArgumentMatchers.any(), ArgumentMatchers.<Class<String>>any())).thenThrow(ex);

    try {
      rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, RC_OFFSET, RC_COUNT);
      fail("Expected exception: RocketChatGetGroupMessagesException");
    } catch (RocketChatGetGroupMessagesException rocketChatGetGroupMessagesException) {
      assertTrue("Expected RocketChatGetGroupMessagesException thrown", true);
    }

    verify(logService, times(1)).logRocketChatServiceError(ex);
  }

  @Test
  public void getGroupMessages_Should_ReturnRocketChatBadRequestException_WhenProvidedWithInvalidParameters()
      throws Exception {

    HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

    when(restTemplate.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
        ArgumentMatchers.any(), ArgumentMatchers.<Class<String>>any())).thenThrow(ex);

    try {
      rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, RC_OFFSET, RC_COUNT);
      fail("Expected exception: RocketChatBadRequestException");
    } catch (RocketChatBadRequestException rocketChatBadRequestException) {
      assertTrue("Expected RocketChatBadRequestException thrown", true);
    }

    verify(logService, times(1)).logRocketChatBadRequestError(ex);
  }

  @Test
  public void postGroupMessage_Should_ReturnRocketChatPostMessageException_WhenHTTPRequestFails()
      throws Exception {

    RocketChatPostMessageException ex = new RocketChatPostMessageException(new Exception("reason"));

    when(restTemplate.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(),
        ArgumentMatchers.<Class<PostMessageResponseDTO>>any())).thenThrow(ex);

    when(encryptionService.encrypt(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
        .thenReturn(RC_MESSAGE);

    try {
      rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, RC_MESSAGE, null);
      fail("Expected exception: RocketChatPostMessageException");
    } catch (RocketChatPostMessageException rocketChatPostMessageException) {
      assertTrue("Expected RocketChatPostMessageException thrown", true);
    }

    verify(logService, times(1)).logRocketChatServiceError(ex);
  }

  /**
   * Return correct object class tests
   *
   */

  @Test
  public void getGroupMessages_Should_ReturnMessageStreamDTO_WhenProvidedWithValidParameters()
      throws Exception {

    List<MessagesDTO> messages = new ArrayList<MessagesDTO>();
    ResponseEntity<MessageStreamDTO> entity = new ResponseEntity<MessageStreamDTO>(
        new MessageStreamDTO(messages, Integer.toString(RC_COUNT), Integer.toString(RC_OFFSET), "5",
            "true", "0"),
        HttpStatus.OK);

    when(restTemplate.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
        ArgumentMatchers.any(), ArgumentMatchers.<Class<MessageStreamDTO>>any()))
            .thenReturn(entity);

    assertThat(
        rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, RC_OFFSET, RC_COUNT),
        instanceOf(MessageStreamDTO.class));
  }

  @Test
  public void postGroupMessage_Should_ReturnPostMessageResponseDTO_WhenProvidedWithValidParameters()
      throws Exception {

    PostMessageResponseDTO response =
        new PostMessageResponseDTO(new Date(), RC_GROUP_ID, true, "", "");

    when(restTemplate.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(),
        ArgumentMatchers.<Class<PostMessageResponseDTO>>any())).thenReturn(response);

    when(encryptionService.encrypt(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
        .thenReturn(RC_MESSAGE);

    assertThat(
        rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, RC_MESSAGE, null),
        instanceOf(PostMessageResponseDTO.class));
  }

  /**
   * Method: markGroupAsReadForSystemUser
   * 
   */

  @Test
  public void markGroupAsReadForSystemUser_Should_LogError_WhenMarkGroupAsReadFails()
      throws NoSuchFieldException, SecurityException {

    when(rcCredentialsHelper.getSystemUser()).thenReturn(RCC_SYSTEM_USER);

    RestClientException ex = new RestClientException(ERROR_MSG);
    when(restTemplate.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(),
        ArgumentMatchers.<Class<StandardResponseDTO>>any())).thenThrow(ex);

    try {
      rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
      fail("Expected exception: RocketChatPostMarkGroupAsReadException");
    } catch (RocketChatPostMarkGroupAsReadException rocketChatPostMarkGroupAsReadException) {
      assertTrue("Expected RocketChatPostMarkGroupAsReadException thrown", true);
    }

    verify(logService, times(1)).logRocketChatServiceError(ex);
  }

  @Test
  public void markGroupAsReadForSystemUser_Should_MarkGroupAsRead_WhenProvidedWithValidGroupId()
      throws NoSuchFieldException, SecurityException {

    when(rcCredentialsHelper.getSystemUser()).thenReturn(RCC_SYSTEM_USER);

    when(restTemplate.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.any(),
        ArgumentMatchers.<Class<StandardResponseDTO>>any()))
            .thenReturn(STANDARD_SUCCESS_RESPONSE_DTO);

    boolean response = rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
    assertTrue(response);
  }

  @Test
  public void markGroupAsReadForSystemUser_Should_LogError_WhenProvidedWithInvalidRocketChatSystemUserCredentials()
      throws NoSuchFieldException, SecurityException {

    when(rcCredentialsHelper.getSystemUser()).thenReturn(INVALID_RCC_SYSTEM_USER);

    boolean response = rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
    assertFalse(response);
    verify(logService, times(1)).logRocketChatServiceError(Mockito.anyString());
  }

}

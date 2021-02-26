package de.caritas.cob.messageservice.api.service;

import static de.caritas.cob.messageservice.testhelper.RocketChatFieldConstants.FIELD_NAME_RC_GET_GROUP_INFO_URL;
import static de.caritas.cob.messageservice.testhelper.RocketChatFieldConstants.FIELD_NAME_RC_POST_GROUP_MESSAGES_READ;
import static de.caritas.cob.messageservice.testhelper.RocketChatFieldConstants.FIELD_VALUE_RC_GET_GROUP_INFO_URL;
import static de.caritas.cob.messageservice.testhelper.RocketChatFieldConstants.FIELD_VALUE_RC_POST_GROUP_MESSAGES_READ;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_TOKEN;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_USER_ID;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.FieldSetter.setField;
import static org.powermock.reflect.Whitebox.setInternalState;

import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatBadRequestException;
import de.caritas.cob.messageservice.api.exception.RocketChatPostMessageException;
import de.caritas.cob.messageservice.api.exception.RocketChatUserNotInitializedException;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.StandardResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.PostMessageResponseDTO;
import de.caritas.cob.messageservice.api.service.helper.RocketChatCredentialsHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class RocketChatServiceTest {

  private static final int RC_OFFSET = 0;
  private static final int RC_COUNT = 0;
  private static final String RC_MESSAGE = "Lorem ipsum";
  private static final String ERROR_MSG = "error";
  private static final StandardResponseDTO STANDARD_SUCCESS_RESPONSE_DTO =
      new StandardResponseDTO(true, null);
  private static final String RC_SYSTEM_USERNAME = "system";
  private static final String RC_SYSTEM_USER_ID = "systemId";
  private static final String RC_SYSTEM_USER_AUTH_TOKEN = "systemToken";
  private static final RocketChatCredentials RCC_SYSTEM_USER = new RocketChatCredentials(
      RC_SYSTEM_USER_AUTH_TOKEN, RC_SYSTEM_USER_ID, RC_SYSTEM_USERNAME, null);
  private static final RocketChatCredentials INVALID_RCC_SYSTEM_USER = new RocketChatCredentials(
      null, null, null, null);

  @InjectMocks
  private RocketChatService rocketChatService;

  @Mock
  private RestTemplate restTemplate;

  @Mock
  private EncryptionService encryptionService;

  @Mock
  RocketChatCredentialsHelper rcCredentialsHelper;

  @Mock
  private Logger logger;

  @Before
  public void setup() throws NoSuchFieldException, SecurityException {
    setField(rocketChatService, rocketChatService.getClass().getDeclaredField("rcHeaderAuthToken"),
        RC_TOKEN);
    setField(rocketChatService, rocketChatService.getClass().getDeclaredField("rcHeaderUserId"),
        RC_USER_ID);
    setField(rocketChatService, rocketChatService.getClass().getDeclaredField("rcQueryParamRoomId"),
        RC_GROUP_ID);
    setField(rocketChatService, rocketChatService.getClass().getDeclaredField("rcQueryParamOffset"),
        String.valueOf(RC_OFFSET));
    setField(rocketChatService, rocketChatService.getClass().getDeclaredField("rcQueryParamCount"),
        String.valueOf(RC_COUNT));
    setField(rocketChatService, rocketChatService.getClass().getDeclaredField("rcQueryParamSort"),
        "sort");
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("rcQueryParamSortValue"), "{\"ts\":1}");
    setField(rocketChatService, rocketChatService.getClass().getDeclaredField("rcPostMessageUrl"),
        "http://localhost/api/v1/chat.postMessage");
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField(FIELD_NAME_RC_POST_GROUP_MESSAGES_READ),
        FIELD_VALUE_RC_POST_GROUP_MESSAGES_READ);
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField(FIELD_NAME_RC_GET_GROUP_INFO_URL),
        FIELD_VALUE_RC_GET_GROUP_INFO_URL);
    setInternalState(LogService.class, "LOGGER", logger);
  }

  @Test(expected = InternalServerErrorException.class)
  public void getGroupMessages_Should_ThrowInternalServerErrorException_When_BuildMessageStreamUriFails()
      throws NoSuchFieldException {
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("rcGetGroupMessageUrl"), null);

    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void getGroupMessages_Should_ThrowInternalServerErrorException_When_RocketChatRequestFails()
      throws NoSuchFieldException {
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("rcGetGroupMessageUrl"),
        "http://localhost/api/v1/groups.messages");
    HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any())).thenThrow(ex);

    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID);
  }

  @Test(expected = InternalServerErrorException.class)
  public void getGroupMessages_Should_ThrowInternalServerErrorException_When_DecryptionOfMessageFails()
      throws NoSuchFieldException, CustomCryptoException {
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("rcGetGroupMessageUrl"),
        "http://localhost/api/v1/groups.messages");
    EasyRandom easyRandom = new EasyRandom();
    MessageStreamDTO messageStreamDTO = easyRandom.nextObject(MessageStreamDTO.class);
    messageStreamDTO.setMessages(easyRandom.objects(MessagesDTO.class, 5)
        .collect(Collectors.toList()));
    ResponseEntity<MessageStreamDTO> response = new ResponseEntity<>(messageStreamDTO,
        HttpStatus.OK);
    CustomCryptoException exception = new CustomCryptoException(new Exception());

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any())).thenReturn(response);
    when(encryptionService.decrypt(anyString(), anyString())).thenThrow(exception);

    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID);
  }

  @Test
  public void getGroupMessages_Should_ReturnMessageStreamDTO_When_ProvidedWithValidParameters()
      throws NoSuchFieldException {
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("rcGetGroupMessageUrl"),
        "http://localhost/api/v1/groups.messages");
    List<MessagesDTO> messages = new ArrayList<>();
    ResponseEntity<MessageStreamDTO> entity = new ResponseEntity<>(
        new MessageStreamDTO().messages(messages), HttpStatus.OK);
    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any())).thenReturn(entity);

    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID);

    assertThat(rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID),
        instanceOf(MessageStreamDTO.class));
  }

  @Test
  public void getGroupMessages_Should_DecryptAllMessages()
      throws NoSuchFieldException, CustomCryptoException {
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("rcGetGroupMessageUrl"),
        "http://localhost/api/v1/groups.messages");
    EasyRandom easyRandom = new EasyRandom();
    MessageStreamDTO messageStreamDTO = easyRandom.nextObject(MessageStreamDTO.class);
    messageStreamDTO.setMessages(easyRandom.objects(MessagesDTO.class, 5)
        .collect(Collectors.toList()));
    ResponseEntity<MessageStreamDTO> response = new ResponseEntity<>(messageStreamDTO,
        HttpStatus.OK);
    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any())).thenReturn(response);

    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID);

    verify(encryptionService, times(5)).decrypt(anyString(), anyString());
  }

  @Test
  public void getGroupMessages_Should_SetForwardAsMessageType_ForForwardedMessages()
      throws NoSuchFieldException {
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("rcGetGroupMessageUrl"),
        "http://localhost/api/v1/groups.messages");
    EasyRandom easyRandom = new EasyRandom();
    MessageStreamDTO messageStreamDTO = easyRandom.nextObject(MessageStreamDTO.class);
    messageStreamDTO.setMessages(easyRandom.objects(MessagesDTO.class, 5)
        .collect(Collectors.toList()));
    messageStreamDTO.getMessages().get(0).getAlias().setMessageType(null);
    messageStreamDTO.getMessages().get(0).getAlias().setVideoCallMessageDTO(null);

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any()))
        .thenReturn(new ResponseEntity<>(messageStreamDTO,
            HttpStatus.OK));

    MessageStreamDTO result = rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID);

    assertThat(result.getMessages().get(0).getAlias().getMessageType(), is(MessageType.FORWARD));
  }

  @Test
  public void getGroupMessages_Should_SetVideocallAsMessageType_ForVideocallMessages()
      throws NoSuchFieldException {
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("rcGetGroupMessageUrl"),
        "http://localhost/api/v1/groups.messages");
    EasyRandom easyRandom = new EasyRandom();
    MessageStreamDTO messageStreamDTO = easyRandom.nextObject(MessageStreamDTO.class);
    messageStreamDTO.setMessages(easyRandom.objects(MessagesDTO.class, 5)
        .collect(Collectors.toList()));
    messageStreamDTO.getMessages().get(0).getAlias().setMessageType(null);
    messageStreamDTO.getMessages().get(0).getAlias().setForwardMessageDTO(null);

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any()))
        .thenReturn(new ResponseEntity<>(messageStreamDTO,
            HttpStatus.OK));

    MessageStreamDTO result = rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID);

    assertThat(result.getMessages().get(0).getAlias().getMessageType(), is(MessageType.VIDEOCALL));
  }

  @Test
  public void getGroupMessages_Should_SetFurtherStepsAsMessageType_ForFurtherStepsMessages()
      throws NoSuchFieldException {
    setField(rocketChatService,
        rocketChatService.getClass().getDeclaredField("rcGetGroupMessageUrl"),
        "http://localhost/api/v1/groups.messages");
    EasyRandom easyRandom = new EasyRandom();
    MessageStreamDTO messageStreamDTO = easyRandom.nextObject(MessageStreamDTO.class);
    messageStreamDTO.setMessages(easyRandom.objects(MessagesDTO.class, 5)
        .collect(Collectors.toList()));
    messageStreamDTO.getMessages().get(0).getAlias().setMessageType(MessageType.FURTHER_STEPS);
    messageStreamDTO.getMessages().get(0).getAlias().setForwardMessageDTO(null);
    messageStreamDTO.getMessages().get(0).getAlias().setVideoCallMessageDTO(null);

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any()))
        .thenReturn(new ResponseEntity<>(messageStreamDTO,
            HttpStatus.OK));

    MessageStreamDTO result = rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID);

    assertThat(result.getMessages().get(0).getAlias().getMessageType(),
        is(MessageType.FURTHER_STEPS));
  }

  @Test
  public void postGroupMessage_Should_ReturnRocketChatPostMessageException_When_HttpRequestFails()
      throws CustomCryptoException {

    RocketChatPostMessageException ex = new RocketChatPostMessageException(new Exception("reason"));

    when(restTemplate.postForObject(
        ArgumentMatchers.anyString(),
        any(),
        ArgumentMatchers.<Class<PostMessageResponseDTO>>any()))
        .thenThrow(ex);

    when(encryptionService.encrypt(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
        .thenReturn(RC_MESSAGE);

    try {
      rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, RC_MESSAGE, null);
      fail("Expected exception: InternalServerErrorException");
    } catch (InternalServerErrorException internalServerErrorException) {
      assertTrue("Expected InternalServerErrorException thrown", true);
    }
  }

  @Test(expected = RocketChatBadRequestException.class)
  public void getGroupInfo_Should_ReturnRocketChatBadRequestException_When_HttpRequestFails() {

    HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

    when(restTemplate.exchange(
        any(), any(HttpMethod.class), any(), ArgumentMatchers.<Class<String>>any()))
        .thenThrow(ex);

    rocketChatService.getGroupInfo(RC_TOKEN, RC_USER_ID, RC_GROUP_ID);
  }

  @Test
  public void
      postGroupMessage_Should_ReturnPostMessageResponseDTO_When_ProvidedWithValidParameters()
          throws CustomCryptoException {

    PostMessageResponseDTO response =
        new PostMessageResponseDTO(new Date(), RC_GROUP_ID, true, "", "");

    when(restTemplate.postForObject(
            ArgumentMatchers.anyString(),
            any(),
            ArgumentMatchers.<Class<PostMessageResponseDTO>>any()))
        .thenReturn(response);

    when(encryptionService.encrypt(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
        .thenReturn(RC_MESSAGE);

    assertThat(
        rocketChatService.postGroupMessage(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, RC_MESSAGE, null),
        instanceOf(PostMessageResponseDTO.class));
  }

  /** Method: markGroupAsReadForSystemUser */
  @Test
  public void markGroupAsReadForSystemUser_Should_LogError_When_MarkGroupAsReadFails()
      throws SecurityException, RocketChatUserNotInitializedException {

    when(rcCredentialsHelper.getSystemUser()).thenReturn(RCC_SYSTEM_USER);

    RestClientException ex = new RestClientException(ERROR_MSG);
    when(restTemplate.postForObject(
            ArgumentMatchers.anyString(),
            any(),
            ArgumentMatchers.<Class<StandardResponseDTO>>any()))
        .thenThrow(ex);

    try {
      rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
      fail("Expected exception: InternalServerErrorException");
    } catch (InternalServerErrorException internalServerErrorException) {
      assertTrue("Expected InternalServerErrorException thrown", true);
    }
  }

  @Test
  public void markGroupAsReadForSystemUser_Should_MarkGroupAsRead_When_ProvidedWithValidGroupId()
      throws SecurityException, RocketChatUserNotInitializedException {

    when(rcCredentialsHelper.getSystemUser()).thenReturn(RCC_SYSTEM_USER);

    when(restTemplate.postForObject(
            ArgumentMatchers.anyString(),
            any(),
            ArgumentMatchers.<Class<StandardResponseDTO>>any()))
        .thenReturn(STANDARD_SUCCESS_RESPONSE_DTO);

    rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
    verify(restTemplate, atLeastOnce())
        .postForObject(anyString(), any(HttpEntity.class), eq(StandardResponseDTO.class));
  }

  @Test
  public void
      markGroupAsReadForSystemUser_Should_LogError_When_ProvidedWithInvalidRocketChatSystemUserCredentials()
          throws SecurityException, RocketChatUserNotInitializedException {

    when(rcCredentialsHelper.getSystemUser()).thenReturn(INVALID_RCC_SYSTEM_USER);

    rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
    verify(logger, times(1)).error(anyString(), anyString());
  }

  @Test(expected = InternalServerErrorException.class)
  public void
      markGroupAsReadForSystemUser_Should_ThrowInternalServerError_When_ProvidedWithOutChatSystemUserCredentials()
          throws SecurityException, RocketChatUserNotInitializedException {

    when(rcCredentialsHelper.getSystemUser())
        .thenThrow(new RocketChatUserNotInitializedException(""));

    rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
  }

  @Test
  public void postGroupVideoHintMessageBySystemUser_Should_postGroupMessage()
      throws Exception {
    RocketChatCredentials rocketChatCredentials =
        new EasyRandom().nextObject(RocketChatCredentials.class);
    when(this.rcCredentialsHelper.getSystemUser()).thenReturn(rocketChatCredentials);

    this.rocketChatService.postGroupVideoHintMessageBySystemUser("rcGroupId", "alias");

    verify(this.restTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), any());
  }
}

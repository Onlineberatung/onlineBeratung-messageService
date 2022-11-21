package de.caritas.cob.messageservice.api.service;

import static de.caritas.cob.messageservice.api.service.RocketChatService.E2E_ENCRYPTION_TYPE;
import static de.caritas.cob.messageservice.testhelper.RocketChatFieldConstants.FIELD_NAME_RC_GET_GROUP_INFO_URL;
import static de.caritas.cob.messageservice.testhelper.RocketChatFieldConstants.FIELD_VALUE_RC_GET_GROUP_INFO_URL;
import static de.caritas.cob.messageservice.testhelper.RocketChatFieldConstants.FIELD_VALUE_RC_POST_GROUP_MESSAGES_READ;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_TOKEN;
import static de.caritas.cob.messageservice.testhelper.TestConstants.RC_USER_ID;
import static de.caritas.cob.messageservice.testhelper.TestConstants.createGroupMessage;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;

import de.caritas.cob.messageservice.api.exception.CustomCryptoException;
import de.caritas.cob.messageservice.api.exception.InternalServerErrorException;
import de.caritas.cob.messageservice.api.exception.RocketChatBadRequestException;
import de.caritas.cob.messageservice.api.exception.RocketChatSendMessageException;
import de.caritas.cob.messageservice.api.exception.RocketChatUserNotInitializedException;
import de.caritas.cob.messageservice.api.model.AliasMessageDTO;
import de.caritas.cob.messageservice.api.model.ChatMessage;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.VideoCallMessageDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.RocketChatCredentials;
import de.caritas.cob.messageservice.api.model.rocket.chat.StandardResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResponseDTO;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.SendMessageResultDTO;
import de.caritas.cob.messageservice.api.service.helper.RocketChatCredentialsHelper;
import java.time.Instant;
import java.util.ArrayList;
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
import org.powermock.reflect.Whitebox;
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
  private MessageMapper messageMapper;

  @Mock
  private Logger logger;

  @Before
  public void setup() throws NoSuchFieldException, SecurityException {
    Whitebox.setInternalState(rocketChatService, "rcHeaderAuthToken", RC_TOKEN);
    Whitebox.setInternalState(rocketChatService, "rcHeaderUserId", RC_USER_ID);
    Whitebox.setInternalState(rocketChatService, "rcQueryParamRoomId", RC_GROUP_ID);
    Whitebox.setInternalState(rocketChatService, "rcQueryParamOffset", String.valueOf(RC_OFFSET));
    Whitebox.setInternalState(rocketChatService, "rcQueryParamCount", String.valueOf(RC_COUNT));
    Whitebox.setInternalState(rocketChatService, "rcQueryParamSort", "sort");
    Whitebox.setInternalState(rocketChatService, "rcQueryParamSortValue", "{\"ts\":1}");
    Whitebox.setInternalState(rocketChatService, "rcSendMessageUrl", "http://localhost/api/v1/chat.sendMessage");
    Whitebox.setInternalState(rocketChatService, "rcPostGroupMessagesRead", FIELD_VALUE_RC_POST_GROUP_MESSAGES_READ);
    Whitebox.setInternalState(rocketChatService, FIELD_NAME_RC_GET_GROUP_INFO_URL, FIELD_VALUE_RC_GET_GROUP_INFO_URL);
    setInternalState(LogService.class, "LOGGER", logger);
  }

  @Test(expected = InternalServerErrorException.class)
  public void getGroupMessages_Should_ThrowInternalServerErrorException_When_BuildMessageStreamUriFails()
      throws NoSuchFieldException {

    Whitebox.setInternalState(rocketChatService, "rcGetGroupMessageUrl", (Object[]) null);
    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, 0, 0, Instant.now());
  }

  @Test(expected = InternalServerErrorException.class)
  public void getGroupMessages_Should_ThrowInternalServerErrorException_When_RocketChatRequestFails()
      throws NoSuchFieldException {
    Whitebox.setInternalState(rocketChatService, "rcGetGroupMessageUrl",  "http://localhost/api/v1/groups.messages");

    HttpClientErrorException ex = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any())).thenThrow(ex);

    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, 0, 0, Instant.now());
  }

  @Test(expected = InternalServerErrorException.class)
  public void getGroupMessages_Should_ThrowInternalServerErrorException_When_DecryptionOfMessageFails()
      throws NoSuchFieldException, CustomCryptoException {
    Whitebox.setInternalState(rocketChatService, "rcGetGroupMessageUrl",  "http://localhost/api/v1/groups.messages");

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

    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, 0, 0, Instant.now());
  }

  @Test
  public void getGroupMessages_Should_ReturnMessageStreamDTO_When_ProvidedWithValidParameters()
      throws NoSuchFieldException {
    Whitebox.setInternalState(rocketChatService, "rcGetGroupMessageUrl", "http://localhost/api/v1/groups.messages");

    List<MessagesDTO> messages = new ArrayList<>();
    ResponseEntity<MessageStreamDTO> entity = new ResponseEntity<>(
        new MessageStreamDTO().messages(messages), HttpStatus.OK);
    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any())).thenReturn(entity);

    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, 0, 0, Instant.now());

    assertThat(rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, 0, 0, Instant.now()),
        instanceOf(MessageStreamDTO.class));
  }

  @Test
  public void getGroupMessages_Should_DecryptAllMessages() throws CustomCryptoException {
    Whitebox.setInternalState(rocketChatService, "rcGetGroupMessageUrl", "http://localhost/api/v1/groups.messages");

    EasyRandom easyRandom = new EasyRandom();
    MessageStreamDTO messageStreamDTO = easyRandom.nextObject(MessageStreamDTO.class);
    messageStreamDTO.setMessages(easyRandom.objects(MessagesDTO.class, 5)
        .collect(Collectors.toList()));
    ResponseEntity<MessageStreamDTO> response = new ResponseEntity<>(messageStreamDTO,
        HttpStatus.OK);
    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any())).thenReturn(response);

    rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID, 0, 0, Instant.now());

    verify(encryptionService, times(5)).decrypt(anyString(), anyString());
  }

  @Test
  public void getGroupMessages_Should_SetForwardAsMessageType_ForForwardedMessages()
      throws NoSuchFieldException {
    Whitebox.setInternalState(rocketChatService, "rcGetGroupMessageUrl", "http://localhost/api/v1/groups.messages");
    EasyRandom easyRandom = new EasyRandom();
    MessageStreamDTO messageStreamDTO = easyRandom.nextObject(MessageStreamDTO.class);
    messageStreamDTO.setMessages(easyRandom.objects(MessagesDTO.class, 5)
        .collect(Collectors.toList()));
    messageStreamDTO.getMessages().get(0).getAlias().setMessageType(null);
    messageStreamDTO.getMessages().get(0).getAlias().setVideoCallMessageDTO(null);

    var mapper = new MessageMapper(null, null);
    messageStreamDTO.getMessages().forEach(messagesDTO -> {
      when(messageMapper.typedMessageOf(eq(messagesDTO)))
            .thenReturn(messagesDTO);
      when(messageMapper.messageTypeOf(eq(messagesDTO.getAlias())))
            .thenReturn(mapper.messageTypeOf(messagesDTO.getAlias()));
    });

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any()))
        .thenReturn(new ResponseEntity<>(messageStreamDTO,
            HttpStatus.OK));

    MessageStreamDTO result = rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        0, 0, Instant.now());

    assertThat(result.getMessages().get(0).getAlias().getMessageType(), is(MessageType.FORWARD));
  }

  @Test
  public void getGroupMessages_Should_SetVideocallAsMessageType_ForVideocallMessages()
      throws NoSuchFieldException {

    Whitebox.setInternalState(rocketChatService, "rcGetGroupMessageUrl", "http://localhost/api/v1/groups.messages");
    EasyRandom easyRandom = new EasyRandom();
    MessageStreamDTO messageStreamDTO = easyRandom.nextObject(MessageStreamDTO.class);
    messageStreamDTO.setMessages(easyRandom.objects(MessagesDTO.class, 5)
        .collect(Collectors.toList()));
    messageStreamDTO.getMessages().get(0).getAlias().setMessageType(null);
    messageStreamDTO.getMessages().get(0).getAlias().setForwardMessageDTO(null);

    var mapper = new MessageMapper(null, null);
    messageStreamDTO.getMessages().forEach(messagesDTO -> {
      when(messageMapper.typedMessageOf(eq(messagesDTO)))
          .thenReturn(messagesDTO);
      when(messageMapper.messageTypeOf(eq(messagesDTO.getAlias())))
          .thenReturn(mapper.messageTypeOf(messagesDTO.getAlias()));
    });

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any()))
        .thenReturn(new ResponseEntity<>(messageStreamDTO,
            HttpStatus.OK));

    MessageStreamDTO result = rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        0, 0, Instant.now());

    assertThat(result.getMessages().get(0).getAlias().getMessageType(), is(MessageType.VIDEOCALL));
  }

  @Test
  public void getGroupMessages_Should_SetFurtherStepsAsMessageType_ForFurtherStepsMessages()
      throws NoSuchFieldException {
    Whitebox.setInternalState(rocketChatService, "rcGetGroupMessageUrl", "http://localhost/api/v1/groups.messages");
    EasyRandom easyRandom = new EasyRandom();
    MessageStreamDTO messageStreamDTO = easyRandom.nextObject(MessageStreamDTO.class);
    messageStreamDTO.setMessages(easyRandom.objects(MessagesDTO.class, 5)
        .collect(Collectors.toList()));
    messageStreamDTO.getMessages().get(0).getAlias().setMessageType(MessageType.FURTHER_STEPS);
    messageStreamDTO.getMessages().get(0).getAlias().setForwardMessageDTO(null);
    messageStreamDTO.getMessages().get(0).getAlias().setVideoCallMessageDTO(null);

    var mapper = new MessageMapper(null, null);
    messageStreamDTO.getMessages().forEach(messagesDTO -> {
      when(messageMapper.typedMessageOf(eq(messagesDTO)))
          .thenReturn(messagesDTO);
      when(messageMapper.messageTypeOf(eq(messagesDTO.getAlias())))
          .thenReturn(mapper.messageTypeOf(messagesDTO.getAlias()));
    });

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(),
        ArgumentMatchers.<Class<MessageStreamDTO>>any()))
        .thenReturn(new ResponseEntity<>(messageStreamDTO,
            HttpStatus.OK));

    MessageStreamDTO result = rocketChatService.getGroupMessages(RC_TOKEN, RC_USER_ID, RC_GROUP_ID,
        0, 0, Instant.now());

    assertThat(result.getMessages().get(0).getAlias().getMessageType(),
        is(MessageType.FURTHER_STEPS));
  }

  @Test
  public void postGroupMessage_Should_ReturnRocketChatPostMessageException_When_HttpRequestFails()
      throws CustomCryptoException {

    RocketChatSendMessageException ex = new RocketChatSendMessageException(new Exception("reason"));

    when(restTemplate.postForObject(
        ArgumentMatchers.anyString(),
        any(),
        ArgumentMatchers.<Class<SendMessageResponseDTO>>any()))
        .thenThrow(ex);

    when(encryptionService.encrypt(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
        .thenReturn(RC_MESSAGE);

    try {
      rocketChatService.postGroupMessage(createGroupMessage());
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
  public void postGroupMessage_Should_ReturnPostMessageResponseDTO_When_ProvidedWithValidParameters()
      throws CustomCryptoException {

    SendMessageResponseDTO response = new SendMessageResponseDTO(new SendMessageResultDTO(), true,
        null, null);

    when(restTemplate.postForObject(ArgumentMatchers.anyString(), any(),
        ArgumentMatchers.<Class<SendMessageResponseDTO>>any())).thenReturn(response);

    when(encryptionService.encrypt(ArgumentMatchers.anyString(),
        ArgumentMatchers.anyString())).thenReturn(RC_MESSAGE);

    assertThat(
        rocketChatService.postGroupMessage(createGroupMessage()),
        instanceOf(SendMessageResponseDTO.class));
  }

  @Test
  public void postGroupMessage_should_not_encrypt_text_of_e2e_encrypted_messages()
      throws CustomCryptoException {
    var e2eEncryptedMessage = ChatMessage.builder().rcToken(RC_TOKEN).rcUserId(RC_USER_ID)
        .rcGroupId(RC_GROUP_ID).text("e2eEncryptedMessage")
        .type(E2E_ENCRYPTION_TYPE).build();

    rocketChatService.postGroupMessage(e2eEncryptedMessage);

    verifyNoInteractions(encryptionService);
  }

  /**
   * Method: markGroupAsReadForSystemUser
   */
  @Test
  public void markGroupAsReadForSystemUser_Should_LogError_When_MarkGroupAsReadFails()
      throws SecurityException, RocketChatUserNotInitializedException {

    when(rcCredentialsHelper.getSystemUser()).thenReturn(RCC_SYSTEM_USER);

    RestClientException ex = new RestClientException(ERROR_MSG);
    when(restTemplate.postForObject(ArgumentMatchers.anyString(), any(),
        ArgumentMatchers.<Class<StandardResponseDTO>>any())).thenThrow(ex);

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

    when(restTemplate.postForObject(ArgumentMatchers.anyString(), any(),
        ArgumentMatchers.<Class<StandardResponseDTO>>any())).thenReturn(
        STANDARD_SUCCESS_RESPONSE_DTO);

    rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
    verify(restTemplate, atLeastOnce())
        .postForObject(anyString(), any(HttpEntity.class), eq(StandardResponseDTO.class));
  }

  @Test
  public void markGroupAsReadForSystemUser_Should_LogError_When_ProvidedWithInvalidRocketChatSystemUserCredentials()
      throws SecurityException, RocketChatUserNotInitializedException {

    when(rcCredentialsHelper.getSystemUser()).thenReturn(INVALID_RCC_SYSTEM_USER);

    rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
    verify(logger, times(1)).error(anyString(), anyString());
  }

  @Test(expected = InternalServerErrorException.class)
  public void markGroupAsReadForSystemUser_Should_ThrowInternalServerError_When_ProvidedWithOutChatSystemUserCredentials()
      throws SecurityException, RocketChatUserNotInitializedException {

    when(rcCredentialsHelper.getSystemUser())
        .thenThrow(new RocketChatUserNotInitializedException(""));

    rocketChatService.markGroupAsReadForSystemUser(RC_GROUP_ID);
  }

  @Test
  public void postAliasOnlyMessageAsSystemUser_Should_postGroupMessage() throws Exception {
    RocketChatCredentials rocketChatCredentials =
        new EasyRandom().nextObject(RocketChatCredentials.class);
    when(this.rcCredentialsHelper.getSystemUser()).thenReturn(rocketChatCredentials);
    AliasMessageDTO aliasMessageDTO =
        new AliasMessageDTO().videoCallMessageDTO(new VideoCallMessageDTO());

    this.rocketChatService.postAliasOnlyMessageAsSystemUser("rcGroupId", aliasMessageDTO);

    verify(this.restTemplate, times(1)).postForObject(anyString(), any(HttpEntity.class), any());
  }

  @Test(expected = InternalServerErrorException.class)
  public void postAliasOnlyMessageAsSystemUser_Should_throwInternalServerErrorException_When_CustomCryptoExceptionIsThrown()
      throws CustomCryptoException, RocketChatUserNotInitializedException {
    EasyRandom easyRandom = new EasyRandom();
    RocketChatCredentials rocketChatCredentials = easyRandom
        .nextObject(RocketChatCredentials.class);
    AliasMessageDTO aliasMessageDTO = easyRandom.nextObject(AliasMessageDTO.class);
    when(this.rcCredentialsHelper.getSystemUser()).thenReturn(rocketChatCredentials);
    when(encryptionService.encrypt(anyString(), anyString()))
        .thenThrow(new CustomCryptoException(new Exception()));

    this.rocketChatService.postAliasOnlyMessageAsSystemUser(RC_GROUP_ID, aliasMessageDTO);
  }
}

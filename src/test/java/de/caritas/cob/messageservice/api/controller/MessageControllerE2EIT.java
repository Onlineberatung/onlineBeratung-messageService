package de.caritas.cob.messageservice.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue;
import de.caritas.cob.messageservice.api.helper.AuthenticatedUser;
import de.caritas.cob.messageservice.api.model.MessageStreamDTO;
import de.caritas.cob.messageservice.api.model.MessageType;
import de.caritas.cob.messageservice.api.model.rocket.chat.message.MessagesDTO;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import org.apache.commons.lang3.RandomStringUtils;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.profiles.active=testing")
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class MessageControllerE2EIT {

  private static final EasyRandom easyRandom = new EasyRandom();
  private static final String CSRF_HEADER = "X-CSRF-TOKEN";
  private static final String CSRF_VALUE = "test";
  private static final Cookie CSRF_COOKIE = new Cookie("CSRF-TOKEN", CSRF_VALUE);

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RestTemplate restTemplate;

  @MockBean
  @SuppressWarnings("unused")
  private AuthenticatedUser authenticatedUser;

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void getMessagesShouldRespondWithMutedUnmutedAlias() throws Exception {
    givenSomeMessagesWithMutedUnmutedType();

    mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("messages", hasSize(5)))
        .andExpect(jsonPath("messages[0].alias.messageType", is(not("USER_MUTED"))))
        .andExpect(jsonPath("messages[0].alias.messageType", is(not("USER_UNMUTED"))))
        .andExpect(jsonPath("messages[1].alias.messageType", is("USER_MUTED")))
        .andExpect(jsonPath("messages[2].alias.messageType", is(not("USER_MUTED"))))
        .andExpect(jsonPath("messages[2].alias.messageType", is(not("USER_UNMUTED"))))
        .andExpect(jsonPath("messages[3].alias.messageType", is("USER_UNMUTED")))
        .andExpect(jsonPath("messages[4].alias.messageType", is(not("USER_MUTED"))))
        .andExpect(jsonPath("messages[4].alias.messageType", is(not("USER_UNMUTED"))));
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.USER_DEFAULT})
  public void getMessagesShouldRespondWithEmptyAlias() throws Exception {
    givenMessagesWithoutClearAlias();

    mockMvc.perform(
            get("/messages")
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .header("rcToken", RandomStringUtils.randomAlphabetic(16))
                .header("rcUserId", RandomStringUtils.randomAlphabetic(16))
                .param("rcGroupId", RandomStringUtils.randomAlphabetic(16))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("messages", hasSize(5)))
        .andExpect(jsonPath("messages[0].alias").isEmpty())
        .andExpect(jsonPath("messages[1].alias").isEmpty())
        .andExpect(jsonPath("messages[2].alias").isEmpty())
        .andExpect(jsonPath("messages[3].alias").isEmpty())
        .andExpect(jsonPath("messages[4].alias").isEmpty());
  }

  private void givenSomeMessagesWithMutedUnmutedType() {
    var messages = easyRandom.objects(MessagesDTO.class, 5).collect(Collectors.toList());
    messages.get(1).setT("user-muted");
    messages.get(3).setT("user-unmuted");
    messages.forEach(message -> {
      var userMutedUnmuted = Set.of(MessageType.USER_MUTED, MessageType.USER_UNMUTED);
      if (userMutedUnmuted.contains(message.getAlias().getMessageType())) {
        var type = easyRandom.nextBoolean() ? MessageType.VIDEOCALL : MessageType.FURTHER_STEPS;
        message.getAlias().setMessageType(type);
      }
    });

    var messageStreamDTO = new MessageStreamDTO();
    messageStreamDTO.setMessages(messages);

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(), eq(MessageStreamDTO.class)))
        .thenReturn(new ResponseEntity<>(messageStreamDTO, HttpStatus.OK));
  }

  private void givenMessagesWithoutClearAlias() {
    var messages = easyRandom.objects(MessagesDTO.class, 5).collect(Collectors.toList());
    messages.forEach(message -> {
      message.setAlias(null);
    });

    var messageStreamDTO = new MessageStreamDTO();
    messageStreamDTO.setMessages(messages);

    when(restTemplate.exchange(any(), any(HttpMethod.class), any(), eq(MessageStreamDTO.class)))
        .thenReturn(new ResponseEntity<>(messageStreamDTO, HttpStatus.OK));
  }
}

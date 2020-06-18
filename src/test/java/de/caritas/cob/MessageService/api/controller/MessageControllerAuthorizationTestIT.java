package de.caritas.cob.MessageService.api.controller;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import javax.servlet.http.Cookie;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import de.caritas.cob.MessageService.api.authorization.Authority;
import de.caritas.cob.MessageService.api.facade.PostGroupMessageFacade;
import de.caritas.cob.MessageService.api.service.EncryptionService;
import de.caritas.cob.MessageService.api.service.RocketChatService;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@SpringBootTest
@AutoConfigureMockMvc
public class MessageControllerAuthorizationTestIT {

  private final String PATH_GET_MESSAGE_STREAM = "/messages";
  private final String PATH_POST_CREATE_MESSAGE = "/messages/new";
  private final String PATH_POST_CREATE_FEEDBACK_MESSAGE = "/messages/feedback/new";
  private final String PATH_POST_UPDATE_KEY = "/messages/key";
  private final String PATH_POST_FORWARD_MESSAGE = "/messages/forward";
  private final String CSRF_COOKIE = "CSRF-TOKEN";
  private final String CSRF_HEADER = "X-CSRF-TOKEN";
  private final String CSRF_VALUE = "test";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private RocketChatService rocketChatService;

  @MockBean
  private EncryptionService encryptionService;

  @MockBean
  private PostGroupMessageFacade postGroupMessageFacade;

  private Cookie csrfCookie;

  @Before
  public void setUp() {
    csrfCookie = new Cookie(CSRF_COOKIE, CSRF_VALUE);
  }

  /**
   * GET on /messages (role: consultant, user)
   *
   */

  @Test
  public void getMessageStream_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(rocketChatService);
  }

  @Test
  @WithMockUser(authorities = {Authority.TECHNICAL_DEFAULT})
  public void getMessageStream_Should_ReturnForbiddenAndCallNoMethods_WhenNoUserOrConsultantDefaultAuthority()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
  }

  @Test
  @WithMockUser(authorities = {Authority.CONSULTANT_DEFAULT, Authority.USER_DEFAULT})
  public void getMessageStream_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens()
      throws Exception {

    mvc.perform(get(PATH_GET_MESSAGE_STREAM).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
  }

  /**
   * POST on /messages/new (role: consultant, user)
   *
   */

  @Test
  public void createMessage_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(postGroupMessageFacade);
  }

  @Test
  @WithMockUser
  public void createMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoUserOrConsultantOrTechnicalDefaultAuthority()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(postGroupMessageFacade);
  }

  @Test
  @WithMockUser(authorities = {Authority.CONSULTANT_DEFAULT, Authority.USER_DEFAULT,
      Authority.TECHNICAL_DEFAULT})
  public void createMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_MESSAGE).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(postGroupMessageFacade);
  }

  /**
   * POST on /messages/key (role: technical)
   *
   */

  @Test
  public void updateKey_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(post(PATH_POST_UPDATE_KEY).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(encryptionService);
  }

  @Test
  @WithMockUser(authorities = {Authority.CONSULTANT_DEFAULT, Authority.USER_DEFAULT})
  public void updateKey_Should_ReturnForbiddenAndCallNoMethods_WhenNoTechnicalDefaultAuthority()
      throws Exception {

    mvc.perform(post(PATH_POST_UPDATE_KEY).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(encryptionService);
  }

  @Test
  @WithMockUser(authorities = {Authority.TECHNICAL_DEFAULT})
  public void updateKey_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens() throws Exception {

    mvc.perform(post(PATH_POST_UPDATE_KEY).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(encryptionService);
  }

  /**
   * POST on /messages/forward (Authority.USE_FEEDBACK)
   *
   */

  @Test
  public void forwardMessage_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(postGroupMessageFacade);
  }

  @Test
  @WithMockUser
  public void forwardMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoUserFeedbackAuthority()
      throws Exception {

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(postGroupMessageFacade);
  }

  @Test
  @WithMockUser(authorities = {Authority.USE_FEEDBACK})
  public void forwardMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens()
      throws Exception {

    mvc.perform(post(PATH_POST_FORWARD_MESSAGE).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(postGroupMessageFacade);
  }

  /**
   * POST on /messages/feedback/new (authority: USE_FEEDBACK)
   *
   */

  @Test
  public void createFeedbackMessage_Should_ReturnUnauthorizedAndCallNoMethods_WhenNoKeycloakAuthorization()
      throws Exception {

    mvc.perform(
        post(PATH_POST_CREATE_FEEDBACK_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(postGroupMessageFacade);
  }

  @Test
  @WithMockUser
  public void createFeedbackMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoUserFeedbackAuthority()
      throws Exception {

    mvc.perform(
        post(PATH_POST_CREATE_FEEDBACK_MESSAGE).cookie(csrfCookie).header(CSRF_HEADER, CSRF_VALUE)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(postGroupMessageFacade);
  }

  @Test
  @WithMockUser(authorities = {Authority.USE_FEEDBACK})
  public void createFeedbackMessage_Should_ReturnForbiddenAndCallNoMethods_WhenNoCsrfTokens()
      throws Exception {

    mvc.perform(post(PATH_POST_CREATE_FEEDBACK_MESSAGE).contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

    verifyNoMoreInteractions(rocketChatService);
    verifyNoMoreInteractions(postGroupMessageFacade);
  }

}

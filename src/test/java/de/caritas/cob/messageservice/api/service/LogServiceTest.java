package de.caritas.cob.messageservice.api.service;

import static net.therore.logback.EventMatchers.text;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.caritas.cob.messageservice.api.exception.RocketChatGetGroupMessagesException;
import java.io.PrintWriter;
import net.therore.logback.LogbackRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LogServiceTest {

  private final String ERROR_MESSAGE = "error";
  private final String RC_SERVICE_ERROR_TEXT = "Rocket.Chat service error: ";
  private final String INTERNAL_SERVER_ERROR_TEXT = "Internal Server Error: ";
  private final String BAD_REQUEST_TEXT = "Bad Request: ";

  @Mock
  private RocketChatGetGroupMessagesException rocketChatGetGroupMessagesException;

  @Mock
  Exception exception;

  @Rule
  public LogbackRule rule = new LogbackRule();

  @Test
  public void logRocketChatServiceError_Should_LogExceptionStackTrace() {

    LogService.logRocketChatServiceError(rocketChatGetGroupMessagesException);
    verify(rocketChatGetGroupMessagesException, atLeastOnce())
        .printStackTrace(any(PrintWriter.class));

  }

  @Test
  public void logRocketChatServiceError_Should_LogErrorMessage() {

    LogService.logRocketChatServiceError(ERROR_MESSAGE);
    verify(rule.getLog(), times(1)).contains(argThat(text(RC_SERVICE_ERROR_TEXT + ERROR_MESSAGE)));
  }

  @Test
  public void logRocketChatServiceError_Should_LogErrorMessageAndExceptionStackTrace() {

    LogService.logRocketChatServiceError(ERROR_MESSAGE, rocketChatGetGroupMessagesException);
    verify(rule.getLog(), times(1)).contains(argThat(text(RC_SERVICE_ERROR_TEXT + ERROR_MESSAGE)));
    verify(rocketChatGetGroupMessagesException, atLeastOnce())
        .printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logUserServiceHelperError_Should_LogExceptionStackTrace() {

    LogService.logUserServiceHelperError(exception);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));

  }

  @Test
  public void logInfo_Should_LogMessage() {

    LogService.logInfo(ERROR_MESSAGE);
    verify(rule.getLog(), times(1)).contains(argThat(text(ERROR_MESSAGE)));
  }

  @Test
  public void logEncryptionServiceError_Should_LogExceptionStackTrace() {

    LogService.logEncryptionServiceError(exception);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));

  }

  @Test
  public void logEncryptionPossibleBadKeyError_Should_LogExceptionStackTrace() {

    LogService.logEncryptionPossibleBadKeyError(exception);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logRocketChatBadRequestError_Should_LogExceptionStackTrace() {

    LogService.logRocketChatBadRequestError(exception);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logInternalServerError_Should_LogErrorMessageAndExceptionStackTrace() {

    LogService.logInternalServerError(ERROR_MESSAGE, exception);
    verify(rule.getLog(), times(1))
        .contains(argThat(text(INTERNAL_SERVER_ERROR_TEXT + ERROR_MESSAGE)));
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logBadRequest_Should_LogMessage() {

    LogService.logBadRequest(ERROR_MESSAGE);
    verify(rule.getLog(), times(1)).contains(argThat(text(BAD_REQUEST_TEXT + ERROR_MESSAGE)));
  }
}

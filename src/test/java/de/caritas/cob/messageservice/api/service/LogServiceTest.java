package de.caritas.cob.messageservice.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.setInternalState;

import de.caritas.cob.messageservice.api.exception.RocketChatGetGroupMessagesException;
import java.io.PrintWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class LogServiceTest {

  private static final String ERROR_MESSAGE = "error";
  private static final String RC_SERVICE_ERROR_TEXT = "Rocket.Chat service error: {}";
  private static final String INTERNAL_SERVER_ERROR_TEXT = "Internal Server Error: ";
  private static final String BAD_REQUEST_TEXT = "Bad Request: {}";

  @Mock
  private RocketChatGetGroupMessagesException rocketChatGetGroupMessagesException;

  @Mock
  Exception exception;

  @Mock
  private Logger logger;

  @Before
  public void setup() {
    setInternalState(LogService.class, "LOGGER", logger);
  }

  @Test
  public void logRocketChatServiceError_Should_LogExceptionStackTrace() {

    LogService.logRocketChatServiceError(rocketChatGetGroupMessagesException);
    verify(rocketChatGetGroupMessagesException, atLeastOnce())
        .printStackTrace(any(PrintWriter.class));

  }

  @Test
  public void logRocketChatServiceError_Should_LogErrorMessage() {

    LogService.logRocketChatServiceError(ERROR_MESSAGE);
    verify(logger, times(1)).error(eq(RC_SERVICE_ERROR_TEXT), eq(ERROR_MESSAGE));
  }

  @Test
  public void logRocketChatServiceError_Should_LogErrorMessageAndExceptionStackTrace() {

    LogService.logRocketChatServiceError(ERROR_MESSAGE, rocketChatGetGroupMessagesException);
    verify(logger, times(1)).error(eq(RC_SERVICE_ERROR_TEXT), eq(ERROR_MESSAGE));
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
    verify(logger, times(1)).info(eq(ERROR_MESSAGE));
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
  public void logRocketChatBadRequestError_Should_LogErrorMessage() {

    LogService.logRocketChatBadRequestError(ERROR_MESSAGE);
    verify(logger, times(1))
        .error(eq("Rocket.Chat Bad Request service error: {}"), eq(ERROR_MESSAGE));
  }

  @Test
  public void logInternalServerError_Should_LogErrorMessageAndExceptionStackTrace() {

    LogService.logInternalServerError(ERROR_MESSAGE, exception);
    verify(logger, times(1))
        .error(eq("{}{}"), eq(INTERNAL_SERVER_ERROR_TEXT), eq(ERROR_MESSAGE));
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logBadRequest_Should_LogMessage() {

    LogService.logBadRequest(ERROR_MESSAGE);
    verify(logger, times(1)).error(eq(BAD_REQUEST_TEXT), eq(ERROR_MESSAGE));
  }
}

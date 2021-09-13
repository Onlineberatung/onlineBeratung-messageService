package de.caritas.cob.messageservice.api.service;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.io.PrintWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;

@RunWith(MockitoJUnitRunner.class)
public class LogServiceTest {

  private static final String ERROR_MESSAGE = "error";
  private static final String RC_SERVICE_ERROR_TEXT = "Rocket.Chat service error: {}";
  private static final String INTERNAL_SERVER_ERROR_TEXT = "Internal Server Error: ";
  private static final String BAD_REQUEST_TEXT = "Bad Request: {}";
  private static final String MESSAGE_API_LOG_TEXT = "MessageService API: {}";
  public static final String STATISTICS_EVENT_PROCESSING_ERROR = "StatisticsEventProcessing error: ";
  public static final String STATISTICS_EVENT_PROCESSING_WARNING = "StatisticsEventProcessing warning: ";

  @Mock Exception exception;

  @Mock private Logger logger;

  @Before
  public void setup() {
    setInternalState(LogService.class, "LOGGER", logger);
  }

  @Test
  public void logRocketChatServiceError_Should_LogExceptionStackTrace() {

    LogService.logRocketChatServiceError(exception);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logRocketChatServiceError_Should_LogErrorMessage() {

    LogService.logRocketChatServiceError(ERROR_MESSAGE);
    verify(logger, times(1)).error(RC_SERVICE_ERROR_TEXT, ERROR_MESSAGE);
  }

  @Test
  public void logRocketChatServiceError_Should_LogErrorMessageAndExceptionStackTrace() {

    LogService.logRocketChatServiceError(ERROR_MESSAGE, exception);
    verify(logger, times(1)).error(RC_SERVICE_ERROR_TEXT, ERROR_MESSAGE);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logUserServiceHelperError_Should_LogExceptionStackTrace() {

    LogService.logUserServiceHelperError(exception);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logInfo_Should_LogMessage() {

    LogService.logInfo(ERROR_MESSAGE);
    verify(logger, times(1)).info(ERROR_MESSAGE);
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
        .error("Rocket.Chat Bad Request service error: {}", ERROR_MESSAGE);
  }

  @Test
  public void logInternalServerError_Should_LogErrorMessageAndExceptionStackTrace() {

    LogService.logInternalServerError(ERROR_MESSAGE, exception);
    verify(logger, times(1)).error("{}{}", INTERNAL_SERVER_ERROR_TEXT, ERROR_MESSAGE);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logBadRequest_Should_LogMessage() {

    LogService.logBadRequest(ERROR_MESSAGE);
    verify(logger, times(1)).warn(BAD_REQUEST_TEXT, ERROR_MESSAGE);
  }

  @Test
  public void logInternalServerError_Should_LogExceptionStackTrace() {

    LogService.logInternalServerError(exception);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logWarning_Should_LogWarnMessageWithHttpStatus() {

    LogService.logWarning(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    verify(logger, times(1))
        .warn(
            eq(MESSAGE_API_LOG_TEXT + ": {}"),
            eq(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()),
            anyString());
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logWarning_Should_LogExceptionStackTrace() {

    LogService.logWarning(exception);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
  }

  @Test
  public void logDebug_Should_LogMessage() {

    LogService.logDebug(ERROR_MESSAGE);
    verify(logger, times(1)).debug(MESSAGE_API_LOG_TEXT, ERROR_MESSAGE);
  }

  @Test
  public void logWarning_Should_LogErrorMessage() {

    LogService.logWarning(ERROR_MESSAGE);
    verify(logger, times(1)).warn(MESSAGE_API_LOG_TEXT, ERROR_MESSAGE);
  }

  @Test
  public void logStatisticEventError_Should_LogExceptionStackTraceAndErrorMessage() {

    LogService.logStatisticsEventError(exception);
    verify(exception, atLeastOnce()).printStackTrace(any(PrintWriter.class));
    verify(logger, times(1))
        .error(anyString(), eq(STATISTICS_EVENT_PROCESSING_ERROR), anyString());
  }

  @Test
  public void logStatisticEventWarning_Should_LogErrorMessageAsWarning() {

    LogService.logStatisticsEventWarning(ERROR_MESSAGE);
    verify(logger, times(1))
        .warn(anyString(), eq(STATISTICS_EVENT_PROCESSING_WARNING), eq(ERROR_MESSAGE));
  }
}

package nakadi;

import io.reactivex.exceptions.UndeliverableException;
import java.io.EOFException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ExceptionSupport {

  private static final Map<Integer, Class> CODES_TO_EXCEPTIONS = new HashMap<>();
  private static final Logger logger = LoggerFactory.getLogger(NakadiClient.class.getSimpleName());

  static {
    CODES_TO_EXCEPTIONS.put(400, ClientException.class);
    CODES_TO_EXCEPTIONS.put(401, AuthorizationException.class);
    CODES_TO_EXCEPTIONS.put(403, AuthorizationException.class);
    CODES_TO_EXCEPTIONS.put(404, NotFoundException.class);
    CODES_TO_EXCEPTIONS.put(409, ConflictException.class);
    CODES_TO_EXCEPTIONS.put(412, PreconditionFailedException.class);
    CODES_TO_EXCEPTIONS.put(422, InvalidException.class);
    CODES_TO_EXCEPTIONS.put(429, RateLimitException.class);
    CODES_TO_EXCEPTIONS.put(500, ServerException.class);
    CODES_TO_EXCEPTIONS.put(503, ServerException.class);
  }

  static boolean isInterruptedIOException(Throwable e) {
    // unwrap to see if this is an InterruptedIOException bubbled up from rx/okio
    if (e instanceof UndeliverableException) {
      if (e.getCause() != null && e.getCause() instanceof UncheckedIOException) {
        if (e.getCause().getCause() != null &&
            e.getCause().getCause() instanceof InterruptedIOException) {
          return true;
        }
      }
    }
    return false;
  }

  static Map<Integer, Class> responseCodesToExceptionsMap() {
    return CODES_TO_EXCEPTIONS;
  }

  static boolean isConsumerStreamRetryable(Throwable e) {

    if (e instanceof Error) {
      logger.error(String.format("non_retryable_error_class_consumer %s %s",
          e.getClass(), e.getMessage()), e);

      return false;
    }

    if (nonRetryableException(e)) {
      logger.error(String.format("non_retryable_exception %s %s",
          e.getClass(), e.getMessage()), e);
      return false;
    }

    if (e instanceof NonRetryableNakadiException) {
      logger.error(String.format("non_retryable_nakadi_exception_consumer %s %s",
          e.getClass(), e.getMessage()), e);

      return false;
    }

    if (e instanceof NotFoundException) {
      logger.error(String.format("non_retryable_not_found_exception_consumer %s %s",
              e.getClass(), e.getMessage()), e);

      return false;
    }

    logger.info(String.format("retryable_exception_consumer %s %s", e.getClass(), e.getMessage()));
    return true;
  }

  private static boolean nonRetryableException(Throwable e) {
    return e instanceof IllegalStateException;
  }

  @SuppressWarnings("WeakerAccess")
  @VisibleForTesting
  static boolean isApiRequestRetryable(Throwable e) {

    if (e instanceof Error) {
      logger.error(String.format("non_retryable_error_class_api %s %s",
          e.getClass(), e.getMessage()), e);

      return false;
    }

    if (e instanceof NonRetryableNakadiException) {
      logger.error(String.format("non_retryable_nakadi_exception_api %s %s",
          e.getClass(), e.getMessage()), e);

      return false;
    }

    if (e instanceof NotFoundException) {
      logger.error(String.format("non_retryable_not_found_exception_api %s %s",
              e.getClass(), e.getMessage()), e);

      return false;
    }

    if (e instanceof NakadiException) {
      logger.info(String.format("retryable_nakadi_exception %s %s",
          e.getClass(), e.getMessage()), e);
      return true;
    }

    logger.info(
        String.format("retryable_exception %s %s", e.getClass(), e.getMessage()), e);

    return true;
  }
}

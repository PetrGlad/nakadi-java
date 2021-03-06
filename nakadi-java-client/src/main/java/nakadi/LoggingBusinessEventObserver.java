package nakadi;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to log business events from a stream with Map data.
 */
public class LoggingBusinessEventObserver
    extends StreamObserverBackPressure<BusinessEventMapped<Map<String, Object>>> {

  private static final Logger logger = LoggerFactory.getLogger(LoggingBusinessEventObserver.class);

  @Override public void onStart() {
    logger.info("onStart");
  }

  @Override public void onStop() {
    logger.info("onStop");
  }

  @Override public void onCompleted() {
    logger.info("onCompleted {}", Thread.currentThread().getName());
  }

  @Override public void onError(Throwable e) {
    logger.info("onError {} {}", e.getMessage(), Thread.currentThread().getName());
    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
  }

  @Override public void onNext(StreamBatchRecord<BusinessEventMapped<Map<String, Object>>> record) {
    final StreamOffsetObserver offsetObserver = record.streamOffsetObserver();
    final StreamBatch<BusinessEventMapped<Map<String, Object>>> batch = record.streamBatch();
    final StreamCursorContext cursor = record.streamCursorContext();

    logger.info("partition: {} ------------- {}",
        cursor.cursor().partition(), Thread.currentThread().getName());

    if (batch.isEmpty()) {
      logger.info("partition: %s empty batch", cursor.cursor().partition());
    } else {
      final List<BusinessEventMapped<Map<String, Object>>> events = batch.events();
      for (BusinessEventMapped event : events) {
        int hashCode = event.hashCode();
        logger.info("{} event ------------- ", hashCode);
        logger.info("{} metadata: {} ", hashCode, event.metadata());
        logger.info("{} data: {} ", hashCode, event.data());
      }
    }
    offsetObserver.onNext(record.streamCursorContext());
  }
}

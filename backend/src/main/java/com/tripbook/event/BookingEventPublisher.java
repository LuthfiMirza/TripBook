package com.tripbook.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);
    private final KafkaTemplate<String, BookingConfirmedEvent> kafkaTemplate;

    public BookingEventPublisher(KafkaTemplate<String, BookingConfirmedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(BookingConfirmedEvent event) {
        try {
            kafkaTemplate.send("booking-events", event.bookingReference(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish booking event {}", event.bookingReference(), exception);
                        } else {
                            log.info("Published booking event {} to Kafka", event.bookingReference());
                        }
                    });
        } catch (Exception exception) {
            log.error("Kafka publish was skipped for booking {}", event.bookingReference(), exception);
        }
    }
}

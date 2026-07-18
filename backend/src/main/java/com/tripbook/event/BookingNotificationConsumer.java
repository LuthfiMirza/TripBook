package com.tripbook.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationConsumer.class);

    @KafkaListener(topics = "booking-events", groupId = "notification-service")
    public void handle(BookingConfirmedEvent event) throws InterruptedException {
        Thread.sleep(2_000);
        log.info("[NOTIFICATION] Email sent to {}: Booking {} confirmed", event.userEmail(), event.bookingReference());
    }
}

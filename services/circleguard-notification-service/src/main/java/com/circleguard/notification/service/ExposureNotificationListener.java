package com.circleguard.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExposureNotificationListener {

    /**
     * Listens for status changes and exposure events.
     * In a production environment, this would integrate with Firebase/SendGrid.
     */
    @KafkaListener(topics = "promotion.status.changed", groupId = "notification-group")
    public void handleStatusChange(String eventJson) {
        log.info("Received health status change event: {}", eventJson);
        // Logic to dispatch push notification or email
        dispatchNotification(eventJson);
    }

    private void dispatchNotification(String payload) {
        log.info("--------------------------------------------------");
        log.info("DISPATCHING SECURE NOTIFICATION");
        log.info("Channel: MOBILE_PUSH (Firebase Mock)");
        log.info("Payload: {}", payload);
        log.info("Status: SUCCESS (Mock)");
        log.info("--------------------------------------------------");
    }
}

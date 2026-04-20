package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class ExposureNotificationListenerTest {

    @Autowired
    private ExposureNotificationListener listener;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldHandleStatusChangeEventWithoutError() {
        String mockEvent = "{\"userId\": \"user-123\", \"newStatus\": \"EXPOSED\"}";
        assertDoesNotThrow(() -> listener.handleStatusChange(mockEvent));
    }
}

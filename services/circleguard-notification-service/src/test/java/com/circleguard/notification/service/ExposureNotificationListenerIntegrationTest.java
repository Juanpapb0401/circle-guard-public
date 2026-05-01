package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"promotion.status.changed"})
@TestPropertySource(properties = "spring.kafka.listener.auto-startup=true")
class ExposureNotificationListenerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private NotificationDispatcher dispatcher;

    @MockBean
    private LmsService lmsService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SmsService smsService;

    @MockBean
    private PushService pushService;

    @MockBean
    private TemplateService templateService;

    @Test
    void whenStatusChangedEventReceived_dispatcherIsCalled() {
        String userId = "user-abc-123";
        String eventJson = "{\"anonymousId\":\"" + userId + "\",\"status\":\"CONFIRMED\"}";

        kafkaTemplate.send("promotion.status.changed", userId, eventJson);

        verify(dispatcher, timeout(5_000)).dispatch(eq(userId), eq("CONFIRMED"));
    }

    @Test
    void whenActiveStatusReceived_dispatcherIsNotCalled() {
        String userId = "user-active-456";
        String eventJson = "{\"anonymousId\":\"" + userId + "\",\"status\":\"ACTIVE\"}";

        kafkaTemplate.send("promotion.status.changed", userId, eventJson);

        // ACTIVE status is explicitly ignored by the listener
        verify(dispatcher, after(3_000).never()).dispatch(anyString(), anyString());
    }
}

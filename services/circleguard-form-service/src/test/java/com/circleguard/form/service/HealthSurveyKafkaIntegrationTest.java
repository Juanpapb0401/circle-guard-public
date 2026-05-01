package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.repository.HealthSurveyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test: verifies HealthSurveyService persists the survey and publishes
 * a Kafka event with the correct topic and key. KafkaTemplate is mocked to avoid
 * requiring a real broker; the test focuses on the service-layer integration.
 */
@SpringBootTest
class HealthSurveyKafkaIntegrationTest {

    @Autowired
    private HealthSurveyService healthSurveyService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private HealthSurveyRepository repository;

    @MockBean
    private QuestionnaireService questionnaireService;

    @Test
    void submitSurvey_persistsSurveyAndPublishesEventWithAnonymousIdAsKey() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .hasFever(true)
                .hasCough(false)
                .build();

        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.empty());
        when(repository.save(any(HealthSurvey.class))).thenReturn(survey);

        healthSurveyService.submitSurvey(survey);

        verify(repository).save(any(HealthSurvey.class));
        verify(kafkaTemplate).send(eq("survey.submitted"), eq(anonymousId.toString()), any());
    }
}

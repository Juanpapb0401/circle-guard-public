package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.repository.HealthSurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthSurveyService {
    private final HealthSurveyRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_SURVEY_SUBMITTED = "survey.submitted";

    @Transactional
    public HealthSurvey submitSurvey(HealthSurvey survey) {
        HealthSurvey saved = repository.save(survey);
        
        // Emit Event for Promotion Service
        Map<String, Object> event = Map.of(
            "anonymousId", saved.getAnonymousId(),
            "hasSymptoms", (saved.getHasFever() || saved.getHasCough()),
            "timestamp", System.currentTimeMillis()
        );
        kafkaTemplate.send(TOPIC_SURVEY_SUBMITTED, saved.getAnonymousId().toString(), event);
        
        return saved;
    }
}

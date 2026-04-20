package com.circleguard.form.repository;

import com.circleguard.form.model.HealthSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface HealthSurveyRepository extends JpaRepository<HealthSurvey, UUID> {
}

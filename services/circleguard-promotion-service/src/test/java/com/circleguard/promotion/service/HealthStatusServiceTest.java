package com.circleguard.promotion.service;

import com.circleguard.promotion.repository.UserNodeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class HealthStatusServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public org.springframework.transaction.PlatformTransactionManager transactionManager() {
            return Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        }

        @Bean(name = "neo4jTransactionManager")
        public org.springframework.transaction.PlatformTransactionManager neo4jTransactionManager() {
            return Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        }
    }

    @Autowired
    private HealthStatusService healthStatusService;

    @MockBean
    private UserNodeRepository userNodeRepository;

    @MockBean
    private Neo4jClient neo4jClient;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @Test
    void shouldUpdateStatusSuccessfully() {
        String anonymousId = "user-abc-123";
        String status = "GREEN";

        // Mock Neo4j using Deep Stubs for the fluent API
        Neo4jClient.UnboundRunnableSpec runnableSpec = Mockito.mock(Neo4jClient.UnboundRunnableSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())).thenReturn(runnableSpec);

        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertDoesNotThrow(() -> healthStatusService.updateStatus(anonymousId, status));
        
        Mockito.verify(valueOperations).set(ArgumentMatchers.eq("user:status:" + anonymousId), ArgumentMatchers.eq(status));
    }
}

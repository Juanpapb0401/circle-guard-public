package com.circleguard.promotion.service;

import com.circleguard.promotion.repository.UserNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthStatusService {
    private final UserNodeRepository userNodeRepository;
    private final Neo4jClient neo4jClient;
    private final StringRedisTemplate redisTemplate;

    private static final String STATUS_KEY_PREFIX = "user:status:";

    /**
     * Updates a user's health status and triggers recursive fencing if required.
     */
    @Transactional("neo4jTransactionManager")
    public void updateStatus(String anonymousId, String status) {
        log.info("Updating Status: User {} -> {}", anonymousId, status);
        
        // 1. Update Graph Node
        neo4jClient.query("MATCH (u:User {anonymousId: $id}) SET u.status = $status")
                .bind(anonymousId).to("id")
                .bind(status).to("status")
                .run();

        // 2. Update Fast Cache for Gateway (Redis)
        redisTemplate.opsForValue().set(STATUS_KEY_PREFIX + anonymousId, status);

        // 3. Trigger Fencing if CONTAGIED (RED)
        if ("CONTAGIED".equals(status)) {
            triggerRecursiveFencing(anonymousId);
        }
    }

    /**
     * Fences all members of circles where the infected user is present.
     */
    private void triggerRecursiveFencing(String anonymousId) {
        log.info("Triggering Fencing for contacts of user {}", anonymousId);
        
        // Cypher: Find all users in circles that share a member who has been in active contact with 'anonymousId'
        String fenceQuery = "MATCH (infected:User {anonymousId: $id})-[:ENCOUNTERED*1..2]-(contact:User) " +
                           "WHERE contact.status <> 'CONTAGIED' AND contact.status <> 'RECOVERED' " +
                           "SET contact.status = 'POTENTIAL' " +
                           "RETURN contact.anonymousId";

        neo4jClient.query(fenceQuery)
                .bind(anonymousId).to("id")
                .fetchAs(String.class)
                .all()
                .forEach(contactId -> {
                    // Update Cache for each fenced contact
                    redisTemplate.opsForValue().set(STATUS_KEY_PREFIX + contactId, "POTENTIAL");
                });
    }

    /**
     * Promotion to RECOVERED (Immunity Window)
     */
    @Transactional
    public void promoteToRecovered(String anonymousId) {
        updateStatus(anonymousId, "RECOVERED");
        // Immunize in Redis for 30 days
        redisTemplate.expire(STATUS_KEY_PREFIX + anonymousId, java.time.Duration.ofDays(30));
    }
}

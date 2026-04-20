package com.circleguard.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final JdbcTemplate jdbc;
    private static final int K_THRESHOLD = 10;

    /**
     * Gets entry trends by location with K-Anonymity protection.
     */
    public List<Map<String, Object>> getEntryTrends(UUID locationId) {
        String query = "SELECT date_trunc('hour', entry_time) as hour, count(*) as entry_count " +
                       "FROM entry_logs WHERE location_id = ? " +
                       "GROUP BY hour ORDER BY hour DESC";
        
        List<Map<String, Object>> rows = jdbc.queryForList(query, locationId);
        
        // Apply K-Anonymity
        rows.forEach(row -> {
            long count = (long) row.get("entry_count");
            if (count < K_THRESHOLD) {
                row.put("entry_count", 0); // Privacy Guard: Mask small groups
                row.put("note", "Insufficient data for privacy");
            }
        });
        
        return rows;
    }

    /**
     * Provides aggregated, university-wide health metrics for leadership.
     * Integrates with Epic 5.3 Institutional Dashboard.
     */
    public Map<String, Object> getGlobalHealthStats() {
        // In a real environment, this aggregates from 'promotion-service' cache/DB.
        // For PoC, querying an 'institutional_health' aggregate table.
        String query = "SELECT status, count(*) AS total FROM institutional_health GROUP BY status";
        
        List<Map<String, Object>> stats = jdbc.queryForList(query);
        Map<String, Object> result = new HashMap<>();
        
        stats.forEach(row -> {
            result.put(row.get("status").toString().toLowerCase() + "_count", row.get("total"));
        });
        
        result.put("timestamp", new Date());
        return result;
    }
}

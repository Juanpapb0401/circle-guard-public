package com.circleguard.promotion.repository;

import com.circleguard.promotion.model.graph.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import java.util.Optional;

public interface UserNodeRepository extends Neo4jRepository<UserNode, String> {
    
    @Query("MATCH (u1:User {anonymousId: $a1}), (u2:User {anonymousId: $a2}) " +
           "MERGE (u1)-[r:ENCOUNTERED {locationId: $loc}]->(u2) " +
           "ON CREATE SET r.startTime = $time, r.duration = 60 " +
           "ON MATCH SET r.duration = r.duration + 60")
    void recordEncounter(String a1, String a2, Long time, String loc);
}

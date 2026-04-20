package com.circleguard.promotion.model.graph;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.*;
import java.util.*;

@Node("Circle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleNode {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private Long createdAt;
    private String locationId;
    private Boolean isActive;

    @Relationship(type = "MEMBER_OF", direction = Relationship.Direction.INCOMING)
    private Set<UserNode> members = new HashSet<>();
}

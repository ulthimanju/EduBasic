package com.app.auth.user.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

/**
 * Neo4j graph node representing an application user.
 *
 * <p>googleId (Google "sub" claim) is the primary lookup key — it is stable.
 * Email is stored but NOT used as a merge key because Google emails can change.</p>
 *
 * <p>Constraints enforced at DB level (see SchemaInitializer):
 * <ul>
 *   <li>UNIQUE on googleId</li>
 *   <li>UNIQUE on email</li>
 * </ul>
 */
@Node("User")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNode {

    @Id
    private String id;

    private String googleId;
    private String email;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}

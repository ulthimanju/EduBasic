package com.edubas.backend.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import com.edubas.backend.entity.Announcement;

@Repository
public interface AnnouncementRepository extends Neo4jRepository<Announcement, String> {

    @Query("MATCH (a:Announcement) OPTIONAL MATCH (a)-[r:ANNOUNCED_BY]->(u:User) RETURN a, r, u ORDER BY a.createdAt DESC")
    List<Announcement> findAllOrderByCreatedAtDesc();

    @Query("MATCH (a:Announcement)-[:ANNOUNCED_BY]->(u:User) WHERE u.id = $userId RETURN a, collect(u) ORDER BY a.createdAt DESC")
    List<Announcement> findByCreatedByUserId(String userId);

    @Query("MATCH (a:Announcement) WHERE a.id = $id OPTIONAL MATCH (a)-[r:ANNOUNCED_BY]->(u:User) RETURN a, r, u")
    Announcement findByIdWithUser(String id);

    @Query("MATCH (u:User) WHERE u.id = $userId " +
            "CREATE (a:Announcement {id: $id, title: $title, description: $description, type: $type, createdAt: $createdAt, updatedAt: $updatedAt}) "
            +
            "CREATE (a)-[:ANNOUNCED_BY]->(u) " +
            "RETURN a, collect(u)")
    Announcement saveWithRelationship(String id, String title, String description, String type, Long createdAt,
            Long updatedAt, String userId);
}

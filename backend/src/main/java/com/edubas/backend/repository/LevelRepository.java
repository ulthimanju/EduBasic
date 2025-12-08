package com.edubas.backend.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.edubas.backend.entity.Level;

@Repository
public interface LevelRepository extends Neo4jRepository<Level, String> {
}

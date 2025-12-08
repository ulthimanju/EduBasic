package com.edubas.backend.repository;

import com.edubas.backend.entity.Module;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleRepository extends Neo4jRepository<Module, String> {
}

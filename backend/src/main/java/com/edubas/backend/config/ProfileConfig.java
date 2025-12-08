package com.edubas.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class ProfileConfig {
    // This allows the application to run in dev profile without Neo4j
}

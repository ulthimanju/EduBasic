package com.edubas.backend.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Node("User")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    private String username;
    private String email;
    private String password;
    private String role;
    private String avatar; // SVG string for user avatar
    private Boolean profileVisibility = true; // Default: profile is visible
    private Boolean emailNotifications = false; // Default: notifications off

    public User(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.profileVisibility = true;
        this.emailNotifications = false;
    }
}

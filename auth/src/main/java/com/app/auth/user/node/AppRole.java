package com.app.auth.user.node;

/**
 * Formalized user roles for application-wide authorization.
 *
 * <p>STUDENT (Standard User) — access to core exam taking and results.
 * INSTRUCTOR (New) — access to exam creation and management.
 * ADMIN — access to global user management.</p>
 */
public enum AppRole {
    STUDENT,
    INSTRUCTOR,
    ADMIN
}

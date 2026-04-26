package com.app.auth.session.service;

import com.app.auth.LogMessages;
import com.app.auth.session.node.SessionNode;
import com.app.auth.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements {@link SessionService} using Spring Data Neo4j.
 *
 * <p>Session creation uses a custom Cypher @Query that atomically creates the
 * Session node AND the HAS_SESSION relationship — the UserNode is never loaded
 * into the application heap during this operation.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Algorithm (§9.3 of design doc):
     * <ol>
     *   <li>Convert Instant expiry to LocalDateTime UTC</li>
     *   <li>Run single Cypher: MATCH User → CREATE Session → CREATE HAS_SESSION</li>
     * </ol>
     */
    @Override
    @Transactional
    public SessionNode createSession(String userId, String jwtId, Instant expiresAt) {
        LocalDateTime issuedAt   = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiresAtLdt = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
        String sessionNodeId = UUID.randomUUID().toString();

        SessionNode session = sessionRepository.createSessionForUser(
            sessionNodeId, userId, jwtId, issuedAt, expiresAtLdt);

        log.info(LogMessages.SESSION_CREATED, userId, jwtId);
        return session;
    }

    /**
     * {@inheritDoc}
     * Sets revoked=true on the Session node — the JWT is now invalid even if not expired.
     */
    @Override
    @Transactional
    public void revokeSession(String jwtId) {
        sessionRepository.revokeBySessionId(jwtId)
                .ifPresent(s -> log.info(LogMessages.SESSION_REVOKED, jwtId));
    }

    /**
     * {@inheritDoc}
     * Sets revoked=true on every HAS_SESSION-linked session for the user.
     */
    @Override
    @Transactional
    public void revokeAllForUser(String userId) {
        int count = sessionRepository.revokeAllSessionsForUser(userId).size();
        log.info(LogMessages.REVOKED_SESSIONS_FOR_USER, count, userId);
    }

    @Override
    public boolean isSessionValid(String jwtId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return sessionRepository.findBySessionId(jwtId)
                .map(s -> !s.isRevoked() && s.getExpiresAt().isAfter(now))
                .orElse(false);
    }

    @Override
    @Transactional
    public Optional<SessionNode> findAndRevokeAtomic(String jwtId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return sessionRepository.findAndRevokeAtomic(jwtId, now);
    }
}

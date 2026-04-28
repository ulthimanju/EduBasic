package com.app.exam.service;

import com.app.exam.LogMessages;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.jwks-uri}")
    private String jwksUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing JwtService - pre-fetching JWKS from {}", jwksUri);
        try {
            PublicKey key = fetchPublicKeyFromJwks();
            keyCache.put("default", key);
            log.info("Successfully pre-fetched JWKS public key");
        } catch (Exception e) {
            log.error("CRITICAL: Failed to fetch JWKS on startup from {}. Auth will be unavailable.", jwksUri, e);
            throw new RuntimeException("Failed to initialize JwtService: JWKS fetch failed", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.debug(LogMessages.JWT_VALIDATION_FAILED, e.getMessage());
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    public String extractJwtId(String token) {
        return parseClaims(token).getId();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PublicKey getPublicKey() {
        // In a real app, you'd handle multiple keys and kid matching.
        // For this basic setup, we'll fetch once and cache the "default" key.
        return keyCache.computeIfAbsent("default", k -> fetchPublicKeyFromJwks());
    }

    private PublicKey fetchPublicKeyFromJwks() {
        try {
            log.info("Fetching JWKS from {}", jwksUri);
            String response = restTemplate.getForObject(jwksUri, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode keys = root.path("keys");
            
            if (keys.isMissingNode() || !keys.isArray() || keys.isEmpty()) {
                throw new RuntimeException("JWKS response contains no keys");
            }

            JsonNode keyData = keys.get(0); // Take first key

            String nStr = keyData.path("n").asText();
            String eStr = keyData.path("e").asText();

            if (nStr.isEmpty() || eStr.isEmpty()) {
                throw new RuntimeException("JWKS key data is missing 'n' or 'e'");
            }

            byte[] nBytes = Base64.getUrlDecoder().decode(nStr);
            byte[] eBytes = Base64.getUrlDecoder().decode(eStr);

            BigInteger n = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger(1, eBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            log.error("Failed to fetch public key from JWKS", e);
            throw new RuntimeException("Could not verify JWT: JWKS unavailable", e);
        }
    }
}

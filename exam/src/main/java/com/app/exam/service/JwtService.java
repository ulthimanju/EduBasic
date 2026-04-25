package com.app.exam.service;

import com.app.exam.LogMessages;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
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

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return parseClaims(token).get("roles", List.class);
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
            Map<String, Object> response = restTemplate.getForObject(jwksUri, Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) response.get("keys");
            Map<String, Object> keyData = keys.get(0); // Take first key

            String nStr = (String) keyData.get("n");
            String eStr = (String) keyData.get("e");

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

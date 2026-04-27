package com.nexora.courseservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    @Value("${auth.jwks-uri}")
    private String jwksUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();

    public record JwtPayload(UUID userId, String email, List<String> roles) {}

    @PostConstruct
    public void init() {
        try {
            fetchPublicKeyFromJwks();
        } catch (Exception e) {
            log.error("Failed to fetch JWKS on startup", e);
        }
    }

    public JwtPayload validateToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        
        Object rawRoles = claims.get("roles");
        List<String> roles = rawRoles instanceof List<?> list
                ? list.stream()
                      .filter(String.class::isInstance)
                      .map(String.class::cast)
                      .toList()
                : List.of();

        return new JwtPayload(userId, email, roles);
    }

    private PublicKey getPublicKey() {
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

            JsonNode keyData = keys.get(0);
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
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(spec);
        } catch (Exception e) {
            log.error("Error fetching JWKS", e);
            throw new RuntimeException("Could not fetch public key", e);
        }
    }
}

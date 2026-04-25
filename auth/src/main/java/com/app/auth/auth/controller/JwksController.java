package com.app.auth.auth.controller;

import com.app.auth.auth.dto.JwksResponseDTO;
import com.app.auth.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtService jwtService;

    @GetMapping("/api/auth/.well-known/jwks.json")
    public JwksResponseDTO getJwks() {
        RSAPublicKey publicKey = (RSAPublicKey) jwtService.getPublicKey();
        
        JwksResponseDTO.JwkKey key = JwksResponseDTO.JwkKey.builder()
                .kty("RSA")
                .alg("RS256")
                .use("sig")
                .kid("default") // Simple implementation uses a single static kid
                .n(Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getModulus().toByteArray()))
                .e(Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray()))
                .build();

        return JwksResponseDTO.builder()
                .keys(List.of(key))
                .build();
    }
}

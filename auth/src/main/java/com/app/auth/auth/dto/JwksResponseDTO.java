package com.app.auth.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class JwksResponseDTO {
    private List<JwkKey> keys;

    @Data
    @Builder
    public static class JwkKey {
        private String kty;
        private String alg;
        private String use;
        private String kid;
        private String n;
        private String e;
    }
}

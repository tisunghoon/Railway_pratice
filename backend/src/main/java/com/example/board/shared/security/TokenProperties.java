package com.example.board.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class TokenProperties {

    public static final String DEFAULT_DEV_SECRET = "railway-practice-local-dev-secret-key-2026";
    private final String secret;

    public TokenProperties(
            @Value("${app.auth.secret:}") String secret,
            Environment environment
    ) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            if (secret == null || secret.isBlank() || DEFAULT_DEV_SECRET.equals(secret.trim())) {
                throw new IllegalStateException("운영 프로필(prod) 활성화 시 안전한 AUTH_SECRET 환경변수 주입이 필수입니다.");
            }
        }
        this.secret = (secret == null || secret.isBlank()) ? DEFAULT_DEV_SECRET : secret.trim();
    }

    public String getSecret() {
        return secret;
    }
}

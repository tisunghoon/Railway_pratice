package com.example.board.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenPropertiesTest {

    @Test
    @DisplayName("local 프로필에서는 시크릿 미지정 시 기본 개발용 키가 적용된다")
    void localProfileDefaultSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");

        TokenProperties props = new TokenProperties("", env);

        assertThat(props.getSecret()).isEqualTo(TokenProperties.DEFAULT_DEV_SECRET);
    }

    @Test
    @DisplayName("prod 단독 프로필에서 시크릿 미지정 시 IllegalStateException이 발생한다")
    void prodProfileMissingSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> new TokenProperties("", env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("운영 프로필(prod) 활성화 시");
    }

    @Test
    @DisplayName("prod 단독 프로필에서 기본 개발용 키 사용 시 IllegalStateException이 발생한다")
    void prodProfileDevSecretRejected() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> new TokenProperties(TokenProperties.DEFAULT_DEV_SECRET, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("안전한 AUTH_SECRET 환경변수 주입이 필수");
    }

    @Test
    @DisplayName("prod,metrics 복수 프로필에서 기본 개발용 키 사용 시 기동이 차단된다")
    void multipleProfilesDevSecretRejected() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod", "metrics");

        assertThatThrownBy(() -> new TokenProperties(TokenProperties.DEFAULT_DEV_SECRET, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("운영 프로필(prod) 활성화 시");
    }

    @Test
    @DisplayName("prod,metrics 복수 프로필에서 안전한 운영 키 지정 시 정상 주입된다")
    void multipleProfilesValidSecretAccepted() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod", "metrics");
        String safeSecret = "production-super-secret-key-must-be-long-and-secure-12345";

        TokenProperties props = new TokenProperties(safeSecret, env);

        assertThat(props.getSecret()).isEqualTo(safeSecret);
    }
}

package com.example.board.shared.security;

import com.example.board.shared.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenProviderTest {

    private TokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        TokenProperties properties = new TokenProperties("test-secret-key-1234567890-abcdef", env);
        tokenProvider = new TokenProvider(properties);
    }

    @Test
    @DisplayName("정상 토큰은 memberId가 정상 추출된다")
    void validTokenResolvesMemberId() {
        String token = tokenProvider.createToken(42L);
        Long memberId = tokenProvider.resolveMemberId("Bearer " + token);
        assertThat(memberId).isEqualTo(42L);
    }

    @Test
    @DisplayName("토큰 끝에 추가 구분자(.. 또는 .)가 붙으면 401 형식 오류가 발생한다")
    void trailingDotsFailValidation() {
        String token = tokenProvider.createToken(42L);

        assertThatThrownBy(() -> tokenProvider.resolveMemberId("Bearer " + token + ".."))
                .isInstanceOf(ApiException.class)
                .hasMessage("인증 토큰 형식이 올바르지 않습니다.");

        assertThatThrownBy(() -> tokenProvider.resolveMemberId("Bearer " + token + "."))
                .isInstanceOf(ApiException.class)
                .hasMessage("인증 토큰 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("빈 항목이 포함된 토큰은 401 형식 오류가 발생한다")
    void emptyPartsFailValidation() {
        assertThatThrownBy(() -> tokenProvider.parseAndValidateToken("1..sig"))
                .isInstanceOf(ApiException.class)
                .hasMessage("인증 토큰 형식이 올바르지 않습니다.");
    }
}

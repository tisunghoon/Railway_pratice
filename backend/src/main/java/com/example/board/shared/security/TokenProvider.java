package com.example.board.shared.security;

import com.example.board.shared.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class TokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_VALIDITY_SECONDS = 60 * 60 * 24; // 24시간
    private final byte[] secretKeyBytes;

    public TokenProvider(TokenProperties tokenProperties) {
        this.secretKeyBytes = tokenProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String createToken(Long memberId) {
        long expiresAtEpochSec = Instant.now().getEpochSecond() + TOKEN_VALIDITY_SECONDS;
        String payload = memberId + "." + expiresAtEpochSec;
        String signature = sign(payload);
        return payload + "." + signature;
    }

    public Long resolveMemberId(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증 헤더가 누락되었습니다.");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰 형식이 올바르지 않습니다.");
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        return parseAndValidateToken(token);
    }

    public Long parseAndValidateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰 형식이 올바르지 않습니다.");
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰 형식이 올바르지 않습니다.");
        }

        String memberIdStr = parts[0];
        String expiresAtStr = parts[1];
        String givenSignature = parts[2];

        if (memberIdStr.isBlank() || expiresAtStr.isBlank() || givenSignature.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰 형식이 올바르지 않습니다.");
        }

        String payload = memberIdStr + "." + expiresAtStr;
        String expectedSignature = sign(payload);

        if (!MessageDigest.isEqual(
                givenSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰 서명 검증에 실패했습니다.");
        }

        long expiresAt;
        long memberId;
        try {
            expiresAt = Long.parseLong(expiresAtStr);
            memberId = Long.parseLong(memberIdStr);
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰 형식이 올바르지 않습니다.");
        }

        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다.");
        }

        return memberId;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("토큰 서명 생성 중 오류가 발생했습니다.", e);
        }
    }
}

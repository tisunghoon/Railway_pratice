package com.example.board.shared.error;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("원인 예외의 제약 이름이 uk_members_email인 경우 409 Conflict 및 이메일 중복 메시지를 반환한다")
    void emailConstraintViolationReturns409() {
        SQLException sqlEx = new SQLException("duplicate key", "23505");
        ConstraintViolationException cve = new ConstraintViolationException("error", sqlEx, "uk_members_email");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("could not execute statement", cve);

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(dive);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("이메일과 무관한 다른 유니크 제약 위반은 400 Bad Request를 반환한다")
    void otherConstraintViolationReturns400() {
        SQLException sqlEx = new SQLException("duplicate key", "23505");
        ConstraintViolationException cve = new ConstraintViolationException("error", sqlEx, "uk_posts_code");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("unique index violation", cve);

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(dive);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "데이터 무결성 제약조건을 위반했습니다.");
    }
}

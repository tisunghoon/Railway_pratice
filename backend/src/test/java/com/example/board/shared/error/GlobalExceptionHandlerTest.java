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
    @DisplayName("원인 예외의 제약 이름이 정확히 uk_members_email인 경우 409 Conflict를 반환한다")
    void emailConstraintViolationReturns409() {
        SQLException sqlEx = new SQLException("duplicate key", "23505");
        ConstraintViolationException cve = new ConstraintViolationException("error", sqlEx, "uk_members_email");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("could not execute statement", cve);

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(dive);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("SQLSTATE 23505이며 오류 메시지에 uk_members_email 제약명이 포함된 경우 409를 반환한다")
    void sqlState23505WithConstraintNameReturns409() {
        SQLException sqlEx = new SQLException("ERROR: duplicate key value violates unique constraint \"uk_members_email\"", "23505");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("error", sqlEx);

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(dive);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("uk_members_email_archive처럼 접미사가 붙은 다른 제약 이름은 400 Bad Request를 반환한다")
    void similarConstraintNameReturns400() {
        SQLException sqlEx = new SQLException("duplicate key", "23505");
        ConstraintViolationException cve = new ConstraintViolationException("error", sqlEx, "uk_members_email_archive");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("error", cve);

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(dive);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "데이터 무결성 제약조건을 위반했습니다.");
    }

    @Test
    @DisplayName("SQLSTATE가 23502(NOT NULL)이고 메시지에 데이터로 uk_members_email이 들어있어도 400을 반환한다")
    void notNullViolationWithKeywordInDataReturns400() {
        SQLException sqlEx = new SQLException("null value in column \"nickname\" violates not-null constraint (data: uk_members_email)", "23502");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("error", sqlEx);

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(dive);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "데이터 무결성 제약조건을 위반했습니다.");
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

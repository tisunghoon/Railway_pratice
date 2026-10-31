package com.example.board.shared.error;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TARGET_EMAIL_CONSTRAINT = "uk_members_email";
    private static final Pattern CONSTRAINT_NAME_PATTERN = Pattern.compile("[\\s\"'(`.]" + Pattern.quote(TARGET_EMAIL_CONSTRAINT) + "[\\s\"')`.,;]");

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "요청 파라미터 또는 경로 변수 타입이 올바르지 않습니다."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        if (isEmailUniqueConstraintViolation(ex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "이미 사용 중인 이메일입니다."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "데이터 무결성 제약조건을 위반했습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "서버 내부 오류가 발생했습니다."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = (fieldError != null && fieldError.getDefaultMessage() != null)
                ? fieldError.getDefaultMessage()
                : "입력값이 올바르지 않습니다.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "요청 본문 형식이 올바르지 않습니다."));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).body(Map.of("message", "지원하지 않는 HTTP 메서드입니다."));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).headers(headers).body(Map.of("message", "지원하지 않는 미디어 타입입니다."));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "요청한 리소스를 찾을 수 없습니다."));
    }

    private boolean isEmailUniqueConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if (matchesExactConstraintName(constraintName)) {
                    return true;
                }
            }
            if (current instanceof SQLException sqlEx) {
                String sqlState = sqlEx.getSQLState();
                if ("23505".equals(sqlState)) {
                    String sqlMessage = sqlEx.getMessage();
                    if (sqlMessage != null && containsConstraintToken(sqlMessage)) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean matchesExactConstraintName(String constraintName) {
        if (constraintName == null) {
            return false;
        }
        String normalized = constraintName.trim().toLowerCase();
        if (TARGET_EMAIL_CONSTRAINT.equals(normalized)) {
            return true;
        }
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex != -1) {
            String suffix = normalized.substring(dotIndex + 1);
            if (TARGET_EMAIL_CONSTRAINT.equals(suffix)) {
                return true;
            }
        }
        // H2 auto-generated index name: UK_MEMBERS_EMAIL_INDEX_X
        if (normalized.startsWith(TARGET_EMAIL_CONSTRAINT + "_index_")) {
            return true;
        }
        return false;
    }

    private boolean containsConstraintToken(String message) {
        String msg = message.toLowerCase();
        if (CONSTRAINT_NAME_PATTERN.matcher(msg).find() || msg.endsWith(TARGET_EMAIL_CONSTRAINT)) {
            return true;
        }
        // H2 message contains "PUBLIC.UK_MEMBERS_EMAIL_INDEX_..."
        return msg.contains(TARGET_EMAIL_CONSTRAINT + "_index_");
    }
}

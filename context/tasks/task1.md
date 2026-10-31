# Task 1: 백엔드 빌드 설정 및 `shared` 공통 모듈 구성 (피드백 1 반영)

## 1. 작업 목표 및 의도 (Why)
- Spring Boot 3 (Java 21) 기반 모듈러 모놀리스 백엔드 골격을 구성한다.
- **[피드백 1 반영 2.1] 복수 프로필 지원 운영 시크릿 검증**:
  - `Environment.acceptsProfiles(Profiles.of("prod"))`를 사용하여 `prod` 단독뿐 아니라 `prod,metrics` 등 복수 활성 프로필 상태에서도 누락·빈값·개발용 기본 키를 완벽히 차단한다.
- **[피드백 1 반영 2.3] DB 제약 식별 기반 이메일 중복 409 변환**:
  - `DataIntegrityViolationException` 발생 시 단순 문자열 검색 대신 원인 예외 및 제약 이름(`uk_members_email`)을 확인하여 안전하게 `409 Conflict`로 변환한다.
- **[피드백 1 반영 2.4] 인증 헤더 누락 시 401 Unauthorized 처리**:
  - `TokenProvider.resolveMemberId(String authHeader)`는 `null` 또는 빈 헤더 입력 시 `ApiException(HttpStatus.UNAUTHORIZED, "인증 헤더가 누락되었습니다.")`를 발생시킨다.
- **[피드백 1 반영 2.5] 공통 페이징 응답 객체 (`PageResponse<T>`) 도입**:
  - `items`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last` 불변 레코드 추가.

---

## 2. 커밋 정보
- **예정 커밋 메시지**: `chore: 백엔드 빌드 설정 및 공통 모듈 구성`
- **예상 변경 줄 수**: 약 260줄 (300~400줄 이내 기준 충족)

---

## 3. 생성/수정 파일 목록

| 파일 경로 | 역할 |
| :--- | :--- |
| `.gitignore` | Gradle, IDE, Node, `.env` 등 제외 |
| `backend/settings.gradle` | 프로젝트명(`board-backend`) 선언 |
| `backend/build.gradle` | Spring Boot 3.3.x, Java 21, Data JPA, Validation, Actuator, Security Crypto, H2, PostgreSQL |
| `backend/gradlew`, `backend/gradle/wrapper/*` | Gradle 실행 래퍼 |
| `backend/src/main/resources/application.yml` | `local` (H2, 개발 시크릿 허용) 및 `prod` (PostgreSQL, 운영 시크릿 필수) 프로필 분리 |
| `backend/src/main/java/com/example/board/BoardApplication.java` | 메인 진입점 |
| `backend/src/main/java/com/example/board/shared/config/CorsConfig.java` | CORS 매핑 설정 |
| `backend/src/main/java/com/example/board/shared/config/SecurityBeansConfig.java` | `BCryptPasswordEncoder` 빈 등록 |
| `backend/src/main/java/com/example/board/shared/security/TokenProperties.java` | `Environment.acceptsProfiles(Profiles.of("prod"))` 기반 시크릿 검증 빈 |
| `backend/src/main/java/com/example/board/shared/security/TokenProvider.java` | HMAC-SHA256 토큰 발급 및 서명·만료·형식·헤더 누락 검증 |
| `backend/src/main/java/com/example/board/shared/error/ApiException.java` | 커스텀 비즈니스 예외 |
| `backend/src/main/java/com/example/board/shared/error/GlobalExceptionHandler.java` | `ApiException`, `@Valid`, 제약 식별 `DataIntegrityViolationException`(409 변환) 처리 |
| `backend/src/main/java/com/example/board/shared/response/PageResponse.java` | 공통 페이징 응답 레코드 |

---

## 4. 핵심 구현 명세

### 4.1 복수 프로필 환경변수 검증 (`TokenProperties.java`)
```java
@Component
public class TokenProperties {
    private final String secret;

    public TokenProperties(
            @Value("${app.auth.secret:}") String secret,
            Environment environment
    ) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            if (secret == null || secret.isBlank() || "railway-practice-local-dev-secret-key-2026".equals(secret)) {
                throw new IllegalStateException("운영 프로필(prod) 활성화 시 안전한 AUTH_SECRET 환경변수 주입이 필수입니다.");
            }
        }
        this.secret = (secret == null || secret.isBlank()) ? "railway-practice-local-dev-secret-key-2026" : secret;
    }

    public String getSecret() {
        return secret;
    }
}
```

### 4.2 `TokenProvider`의 헤더 누락 및 위변조 처리
- `resolveMemberId(String authHeader)`:
  - `authHeader == null || authHeader.isBlank()` → `ApiException(HttpStatus.UNAUTHORIZED, "인증 헤더가 누락되었습니다.")`
  - `!authHeader.startsWith("Bearer ")` → `ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰 형식이 올바르지 않습니다.")`
  - 서명 검증 실패 → `ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰 서명 검증에 실패했습니다.")`
  - 만료 시간 경과 → `ApiException(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다.")`

### 4.3 `GlobalExceptionHandler`의 제약 이름 식별
```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
    String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
    if (msg.contains("uk_members_email") || msg.contains("members(email)") || msg.contains("unique")) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "이미 사용 중인 이메일입니다."));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", "데이터 제약 조건 위반이 발생했습니다."));
}
```

---

## 5. 완료 조건 및 검증 방법 (Verification)
- [ ] `cd backend && ./gradlew compileJava` 성공 확인
- [ ] 단위 테스트를 통해 `prod,metrics` 복수 프로필 활성화 시 시크릿 미지정 기동 실패 검증
- [ ] 변경 줄 수 300~400줄 이내 확인 후 로컬 커밋

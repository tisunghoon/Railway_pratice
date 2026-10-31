# Task 2: 백엔드 `member` 도메인 모듈 및 이메일 인증 API 구현 (피드백 1 반영)

## 1. 작업 목표 및 의도 (Why)
- **[피드백 1 반영 2.3] 명시적 유니크 제약 `uk_members_email` 선언**:
  - `Member` 엔티티의 `email` 컬럼에 `@Table(name = "members", uniqueConstraints = @UniqueConstraint(name = "uk_members_email", columnNames = "email"))`을 지정하여 동시 가입 시 일관된 제약 위반 예외가 발생하도록 한다.
- **[피드백 1 반영 2.7] 입력값 길이 및 형식 엄격 검증**:
  - `SignupRequest`: `@Email`, `@NotBlank`, 이메일 최대 120자, 비밀번호 8~64자, 닉네임 2~50자 검증 어노테이션 적용.
- **[피드백 1 반영 2.4] 헤더 누락 시 401 처리를 위한 `required = false` 적용**:
  - `AuthController.getMe(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader)`로 헤더를 수신하여, 헤더가 없어도 400 바인딩 에러 대신 `TokenProvider`의 일관된 `401 Unauthorized` 예외가 발생하도록 한다.
- 계층 간 역방향 의존 금지 원칙(Command / Result 도입) 유지.

---

## 2. 커밋 정보
- **예정 커밋 메시지**: `feat: 회원 도메인 모듈 및 이메일 인증 API 구현`
- **예상 변경 줄 수**: 약 260줄 (300~400줄 이내 기준 충족)

---

## 3. 생성/수정 파일 목록

| 파일 경로 | 계층 | 역할 |
| :--- | :---: | :--- |
| `backend/.../modules/member/domain/Member.java` | `domain` | `uk_members_email` 명시적 유니크 제약을 갖는 회원 JPA 엔티티 |
| `backend/.../modules/member/domain/MemberRepository.java` | `domain` | 이메일 조회 및 중복 확인 JPA 리포지토리 |
| `backend/.../modules/member/application/command/SignupCommand.java` | `application` | 회원가입 유스케이스 입력 모델 (정규화 포함) |
| `backend/.../modules/member/application/command/LoginCommand.java` | `application` | 로그인 유스케이스 입력 모델 (정규화 포함) |
| `backend/.../modules/member/application/MemberInfo.java` | `application` | 회원 요약 불변 DTO (`id`, `email`, `nickname`) |
| `backend/.../modules/member/application/AuthResult.java` | `application` | 인증 결과 모델 (`accessToken`, `memberInfo`) |
| `backend/.../modules/member/application/MemberFacade.java` | `application` | 타 모듈(`post`)에 노출되는 회원 조회 공개 포트 |
| `backend/.../modules/member/application/AuthService.java` | `application` | 회원가입·로그인·회원조회 비즈니스 로직 |
| `backend/.../modules/member/presentation/dto/SignupRequest.java` | `presentation` | 회원가입 HTTP 요청 DTO (길이 유효성 검증 포함) |
| `backend/.../modules/member/presentation/dto/LoginRequest.java` | `presentation` | 로그인 HTTP 요청 DTO |
| `backend/.../modules/member/presentation/dto/AuthResponse.java` | `presentation` | 인증 성공 HTTP 응답 DTO |
| `backend/.../modules/member/presentation/AuthController.java` | `presentation` | HTTP 요청 수신, `required = false` 헤더 파싱 및 DTO 매핑 |

---

## 4. 핵심 구현 명세

### 4.1 `Member` 엔티티 유니크 제약
```java
@Entity
@Table(name = "members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_members_email", columnNames = "email")
})
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String nickname;
    ...
}
```

### 4.2 `AuthController` 헤더 수신 방식
```java
@GetMapping("/me")
public ResponseEntity<MemberInfo> getMe(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
) {
    Long memberId = tokenProvider.resolveMemberId(authHeader);
    MemberInfo memberInfo = authService.getMemberById(memberId);
    return ResponseEntity.ok(memberInfo);
}
```

---

## 5. 완료 조건 및 검증 방법 (Verification)
- [ ] `modules/member/application` 패키지가 `presentation` 패키지를 참조하지 않는지 확인
- [ ] `cd backend && ./gradlew compileJava` 성공 확인
- [ ] 변경 줄 수 300~400줄 이내 확인 후 로컬 커밋

# 설계 피드백 0 — Antigravity 설계 검토

- 검토일: 2026-10-31
- 검토자: Codex
- 검토 대상: `context/requirements.md`, `context/design.md`, `context/tasks/task0.md` ~ `task7.md` (총 10개 문서)
- 검토 범위: 요구사항 정합성, 아키텍처 경계, 인증, 데이터 처리, Railway 배포 및 검증 계획
- 검토 한계: 구현 코드가 없는 단계의 문서 검토다. 코드 실행 및 실제 배포 테스트는 수행하지 않았다.

## 1. 전체 평가

전체 구조는 Railway 풀스택 배포 실습 목적에 맞지만, 구현 전에 인증·배포 설정·검증 계획을 보완해야 한다.

잘 설계된 부분은 다음과 같다.

- Frontend·Backend·PostgreSQL의 배포 단위와 연결 관계가 명확하다.
- `MemberFacade`를 통해 도메인 간 접근을 제한하고, `post`가 `Member` 엔티티를 직접 참조하지 않도록 했다.
- `PostCard`에 삭제 버튼을 슬롯으로 주입하는 방식은 FSD 의존성 방향에 맞는다.
- 논리 단위 작업마다 목적, 생성 파일, 검증 방법을 문서화했다.

## 2. 우선순위별 피드백

### 2.1 [높음] 운영 환경에서 공개된 기본 시크릿을 사용할 수 있다

**근거:** `tasks/task1.md` 40행 및 49~53행

`app.auth.secret: ${AUTH_SECRET:railway-practice-local-dev-secret-key-2026}` 설정은 운영 환경에서도 `AUTH_SECRET`을 누락하면 문서에 공개된 키로 실행된다. 이 키로 다른 회원 ID에 대한 유효한 서명 토큰을 만들 수 있다.

**권장 수정:**

- 기본 시크릿은 `local` 프로필에만 둔다.
- `prod`에서는 시크릿 누락 또는 부적절한 값 설정 시 애플리케이션 기동을 실패시킨다.
- 직접 구현하는 HMAC 토큰보다 검증된 토큰 라이브러리 사용을 우선 검토한다.
- 현재 토큰 방식을 유지하면 변조·만료·잘못된 형식에 대한 검증 규칙과 테스트를 명시한다.
- 로그아웃이 브라우저의 토큰 삭제만 의미하는지 명확히 한다. 서버에서 폐기하지 않는 구조라면 기존 토큰은 만료까지 유효하다.

참고: [Spring Security JWT 지원](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

### 2.2 [높음] Railway 배포 계획을 현재 공식 안내에 맞춰야 한다

**근거:** `tasks/task7.md` 19~23행, `design.md` 220~233행

2026-10-31 확인 기준, Railway 공식 문서는 `railway.json` / `railway.toml` 기반 Config as Code를 deprecated로 안내한다. 신규 서비스에서는 사용할 수 없으며, 기존 서비스에 대한 지원 종료일은 2026-12-01로 명시되어 있다.

**권장 수정:**

- 실습 계획을 대시보드 설정 중심으로 바꾸거나 `.railway/railway.ts` 기반 Infrastructure as Code로 변경한다.
- Dockerfile 빌드, 서비스별 Root Directory, 헬스체크, 재시작 정책을 어느 설정 경로에서 적용하는지 명확히 한다.
- DB 환경변수를 “자동 연결”이라고만 설명하지 말고, Backend 서비스에서 참조 변수를 설정하는 절차를 명시한다.
- 예를 들어 DB 서비스 이름이 `Postgres`라면 Backend에 `PGHOST=${{Postgres.PGHOST}}`를 설정하고, 나머지 `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`도 같은 방식으로 연결한다. 실제 서비스 이름에 맞게 참조 이름을 조정한다.

참고:

- [Railway Config as Code 안내](https://docs.railway.com/config-as-code)
- [Railway PostgreSQL 연결](https://docs.railway.com/databases/postgresql)
- [Railway 변수 참조 문법](https://docs.railway.com/variables/reference)

### 2.3 [중간] 계층 분리 원칙과 서비스 메서드 설계가 충돌한다

**근거:** `tasks/task2.md` 25~27행 및 48~55행, `tasks/task3.md` 23~24행 및 42~55행

`AuthService`가 `presentation.dto`에 정의된 `SignupRequest`를 받고 `AuthResponse`를 반환한다. 선언한 계층 구조와 달리 `application → presentation` 의존성이 생긴다. `PostService`도 요청·응답 DTO에서 같은 문제가 발생한다.

**권장 수정:**

- 컨트롤러가 HTTP 요청 DTO를 서비스 입력값 또는 application 계층의 Command로 변환한다.
- 서비스는 application 계층의 결과를 반환하고, 컨트롤러가 응답 DTO로 매핑한다.
- `Authorization` 헤더 파싱은 인증 처리 지점으로 모으고 서비스에는 인증된 `memberId`를 전달한다.
- 향후 모듈 경계를 검증할 때 다른 도메인의 내부 접근뿐 아니라 계층 역방향 의존성도 확인한다.

### 2.4 [중간] 이메일 중복 검사는 동시 가입을 처리하지 못한다

**근거:** `tasks/task2.md` 49~51행, `tasks/task1.md` 55~59행

두 요청이 동시에 `existsByEmail`을 통과하면 DB 유니크 제약에서 한 요청이 실패한다. 현재 예외 처리 명세에는 이 실패를 `409 Conflict`로 변환하는 규칙이 없다.

**권장 수정:**

- DB 유니크 제약을 이메일 중복 방지의 최종 보장으로 둔다.
- 이메일 중복 제약 위반을 식별해 `409 Conflict`로 변환한다. 모든 무결성 오류를 일괄적으로 이메일 중복으로 처리하지 않는다.
- 가입·로그인에 동일한 이메일 정규화 규칙을 적용한다.
- 제목·닉네임·이메일 등의 입력 길이 제한을 DB 컬럼 제한과 맞춘다.

### 2.5 [중간] H2 테스트만으로 운영 PostgreSQL 동작을 보장하기 어렵다

**근거:** `tasks/task1.md` 42~47행, `tasks/task4.md`의 테스트 계획

H2의 PostgreSQL 호환 모드를 사용하더라도 실제 PostgreSQL에서 스키마 생성과 쿼리를 검증하는 단계가 필요하다.

운영 프로필의 `ddl-auto: update`는 최초 실습에는 간편하지만 스키마 변경 이력이 남지 않는다.

**권장 수정:**

- PostgreSQL 기반 통합 테스트 또는 로컬 PostgreSQL 검증 단계를 추가한다.
- 반복 배포와 스키마 변경까지 실습하려면 Flyway 초기 마이그레이션과 `ddl-auto: validate`를 사용한다.
- 첫 배포와 기존 데이터가 있는 상태의 재배포를 모두 검증한다.

참고: [Spring Boot 데이터베이스 초기화](https://docs.spring.io/spring-boot/how-to/data-initialization.html)

### 2.6 [중간] 완료 조건에 실제 배포 검증이 부족하다

**근거:** `tasks/task4.md` 21~41행, `tasks/task6.md`의 완료 조건, `tasks/task7.md` 52~55행

현재 검증은 MockMvc 테스트와 프론트엔드 정적 빌드 중심이다. 이것만으로 Docker 실행, 브라우저 CORS 연동, Railway 서비스 간 연결을 보장할 수 없다.

**추가할 완료 조건:**

- 만료·변조 토큰과 잘못된 입력에 대한 오류 응답을 확인한다.
- 존재하지 않는 게시글 조회·삭제의 응답을 확인한다.
- 실제 브라우저에서 CORS preflight와 로그인·작성·삭제 흐름을 확인한다.
- Backend·Frontend Docker 이미지를 각각 빌드하고 실행한다.
- Backend 재배포 후에도 기존 게시글이 유지되는지 확인한다.
- Docker 빌드에서 `-x test`를 사용하므로, 자동 배포 전에 테스트를 통과시키는 CI 단계를 구성한다.

### 2.7 [개선] 게시글 목록이 본문 전체를 무제한 반환한다

**근거:** `requirements.md` 19행, `tasks/task3.md` 24행 및 45행

목록 요구사항은 제목·작성자·작성일시인데 `List<PostResponse>`는 본문까지 포함하며 조회 건수 제한도 없다. 데이터가 늘어나면 응답 크기와 조회 비용이 함께 증가한다.

**권장 수정:**

- 목록용 응답과 상세용 응답을 분리한다.
- `page`, `size`와 최대 조회 수를 정한다.
- 작은 실습에서는 오프셋 페이지네이션으로 충분하다.

### 2.8 [개선] 작업량 추정과 문서 사이의 불일치를 정리해야 한다

**근거:** `design.md` 239~250행, `tasks/task0.md` 11행

단위 0은 약 250줄로 추정했지만, 검토 시점의 실제 대상 문서 10개는 총 735줄이다. 상세 작업 계획까지 포함한 변경량으로 다시 추정해야 한다.

**권장 수정:**

- 초기 생성 파일과 테스트를 포함해 커밋 단위를 다시 나눈다.
- 요구사항의 300~400줄 기준과 설계·태스크의 300줄 기준을 일관되게 정리한다.
- 논리적 응집성과 검증 가능한 단위를 유지하면서 변경량을 관리한다.

## 3. 추가 문서 정리 사항

| 위치 | 확인 사항 | 권장 조치 |
| --- | --- | --- |
| `design.md` 15행 | Mermaid 종료 구문이 `end1`이다. | `end`로 수정한다. |
| `design.md`, `tasks/task5.md` | 게시글 UI 이름이 `PostItem`과 `PostCard`로 다르다. | 명칭을 통일한다. |
| `design.md`, `tasks/task5.md`, `tasks/task6.md` | 세션 구성에서 `useSession`과 `sessionStorage`의 역할이 일관되게 설명되지 않는다. | 상태 관리와 토큰 저장 책임을 확정하고 문서를 맞춘다. |
| `design.md` 75행, `tasks/task3.md` | 닉네임 스냅샷 저장과 최신 정보 조회가 선택지로 혼재한다. | 작성 시점 스냅샷인지 최신 닉네임인지 정책을 확정한다. |
| `tasks/task7.md` 37행 | 빌드 이미지가 `node:20-alpine`이다. | 지원 중인 LTS 버전으로 변경한다. |

2026-10-31 기준 Node.js 20은 지원 종료 상태다. 참고: [Node.js 지원 종료 안내](https://nodejs.org/en/about/eol)

## 4. 권장 반영 순서

1. 운영 시크릿 필수화와 토큰·로그아웃 정책을 확정한다.
2. Railway 배포 설정 방식을 현재 공식 안내에 맞춘다.
3. 서비스의 presentation DTO 의존성과 인증 헤더 처리 책임을 정리한다.
4. 이메일 동시 가입 처리와 입력 검증을 보완한다.
5. PostgreSQL·Docker·브라우저·재배포 검증을 완료 조건에 추가한다.
6. 목록 조회 제한, 문서 명칭, 커밋 단위 추정을 정리한다.

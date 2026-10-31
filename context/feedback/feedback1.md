# 설계 피드백 1 — 피드백 반영 후 재검토

- 검토일: 2026-10-31
- 검토자: Codex
- 검토 대상: `context/requirements.md`, `context/design.md`, `context/tasks/task0.md` ~ `task7.md` (총 10개 문서)
- 비교 기준: `feedback0.md`의 지적 사항
- 검토 한계: 설계 문서 기준의 검토다. 구현 코드 실행 및 실제 배포 테스트는 수행하지 않았다.

## 1. 전체 평가 및 반영된 사항

이전보다 설계가 구체화됐다. 다음 항목은 문서에 반영됐다.

- Command와 Result를 통해 application 계층의 presentation DTO 의존성을 제거했다.
- 인증 헤더 파싱 책임을 컨트롤러로 옮기고 서비스에는 인증된 회원 ID를 전달하도록 했다.
- 작성 시점의 닉네임을 스냅샷으로 저장하는 정책을 확정했다.
- 게시글 목록과 상세 응답을 분리하고 목록 페이징을 도입했다.
- Railway 참조 변수와 대시보드 설정 절차를 명시했다.
- 클라이언트 토큰 삭제 방식의 Stateless 로그아웃 정책을 명시했다.
- 운영 시크릿 검사, 토큰 오류 테스트, 재배포 후 데이터 보존 확인을 계획에 추가했다.

다만 세부 계약과 검증 시나리오에 빈틈이 남아 있어 다음 항목을 보완해야 한다.

## 2. 우선순위별 피드백

### 2.1 [높음] 운영 시크릿 검사가 복수 프로필에서 빠질 수 있다

**근거:** `tasks/task1.md` 88행

`spring.profiles.active` 문자열이 정확히 `prod`인지 비교하면 `prod,metrics`처럼 복수 프로필로 실행할 때 검사를 건너뛴다. 이 경우 공개된 개발용 시크릿도 차단되지 않는다.

**권장 수정:**

- `Environment.acceptsProfiles(Profiles.of("prod"))`로 운영 프로필 포함 여부를 확인한다.
- 운영 프로필이 활성화되면 시크릿 누락·빈 문자열·개발용 키를 모두 차단한다.
- 단일 `prod`뿐 아니라 `prod,metrics`와 같은 복수 프로필 조건에서도 기동 차단을 검증한다.

**추가할 검증:**

| 활성 프로필 | 시크릿 | 기대 결과 |
| --- | --- | --- |
| `local` | 미지정 | 개발용 기본값으로 기동 |
| `prod` | 누락 또는 빈 문자열 | 기동 실패 |
| `prod` | 공개된 개발용 키 | 기동 실패 |
| `prod,metrics` | 공개된 개발용 키 | 기동 실패 |
| `prod,metrics` | 적절한 운영 키 | 시크릿 검증 통과 |

참고: [Spring Environment 및 복수 활성 프로필](https://docs.spring.io/spring-framework/reference/core/beans/environment.html)

### 2.2 [중간] 목록 응답에 작성자 ID가 없어 삭제 버튼의 본인 판별과 연결되지 않는다

**근거:** `tasks/task3.md` 31행, `tasks/task6.md` 28~32행

`PostListItemResponse`에는 `id`, `title`, `authorNickname`, `createdAt`만 있다. 반면 프론트엔드는 게시글 목록에서 본인 글에만 삭제 버튼을 표시하도록 설계돼 있다. 현재 목록 데이터만으로는 작성자를 판별할 수 없다.

**권장 수정:**

- 목록 응답에 `authorId`를 추가하거나, 서버에서 `canDelete`를 반환하는 정책을 정한다.
- 현재 공개 목록 API와 인증 구조에서는 `authorId` 추가가 간단하다.
- 프론트엔드는 `currentMember.id === post.authorId`로 표시 여부를 판단한다.
- 서버의 삭제 권한 검증은 계속 유지한다. 버튼 표시 여부가 권한 검증을 대신하지 않는다.

### 2.3 [중간] 이메일 중복 예외를 메시지 문자열로 구분하는 방식이 불안정하다

**근거:** `tasks/task1.md` 97~100행

메시지에 `members` 또는 `email`이 있는지 기준으로 판단하면 다른 제약 위반을 중복 이메일로 오인하거나, DB별 메시지 차이 때문에 중복을 놓칠 수 있다.

**권장 수정:**

- 이메일 유니크 제약에 `uk_members_email`처럼 명시적인 이름을 지정한다.
- 유니크 위반 여부와 해당 제약 이름을 확인해 이메일 중복만 `409 Conflict`로 변환한다.
- PostgreSQL에서는 SQLSTATE `23505`와 제약 이름을 함께 활용할 수 있다.
- H2와 PostgreSQL 양쪽에서 이메일 중복이 동일한 API 오류로 변환되는지 검증한다.

PostgreSQL 공식 문서는 오류 메시지 파싱보다 SQLSTATE와 별도 필드로 제공되는 제약 정보를 사용하는 방식을 안내한다.

참고: [PostgreSQL 오류 코드 및 제약 정보](https://www.postgresql.org/docs/current/errcodes-appendix.html)

### 2.4 [중간] 인증 헤더가 없으면 의도한 401 대신 기본 400이 발생할 수 있다

**근거:** `tasks/task2.md` 57행

`@RequestHeader("Authorization")`는 기본적으로 필수다. 헤더가 없으면 컨트롤러의 토큰 검증 전에 요청 바인딩 예외가 발생하므로, 설계한 인증 오류 처리 경로에 도달하지 못한다.

**권장 수정:**

- 현재 컨트롤러 방식이라면 `@RequestHeader(value = "Authorization", required = false)`로 받는다.
- `TokenProvider`에서 헤더 누락을 `401 Unauthorized`로 처리하도록 명시한다.
- `/api/auth/me`, 게시글 작성, 게시글 삭제에 동일한 규칙을 적용한다.
- 헤더 누락과 잘못된 Bearer 형식을 별도 시나리오로 검증한다.

참고: [Spring RequestHeader의 required 기본값](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/RequestHeader.html)

### 2.5 [중간] 페이징 입력값과 응답 계약이 완성되지 않았다

**근거:** `tasks/task3.md` 66~69행, `tasks/task6.md`의 이전·다음 페이지 버튼 계획

`size = Math.min(size, 50)`은 최대 크기만 제한한다. `size=0`, 음수 크기, `page=-1`에 대한 정책이 없고, 페이징 메타데이터의 JSON 구조도 명시되지 않았다.

**권장 수정:**

- `page >= 0`, `size >= 1`을 검증하고 잘못된 요청은 `400 Bad Request`로 반환한다.
- `size > 50`은 50으로 보정할지 400으로 거절할지 확정한다.
- 예를 들어 `items`, `page`, `size`, `totalPages`처럼 목록 응답 구조를 고정한다.
- 프론트엔드 이전·다음 버튼이 사용하는 필드를 이 계약과 맞춘다.

**추가할 검증:**

- 기본값 요청, 빈 목록, 마지막 페이지, 범위를 벗어난 페이지
- `page=-1`, `size=0`, 음수 크기, 최대 크기 초과
- 목록 본문 제외 및 프론트엔드 페이지 이동

### 2.6 [중간] 검증 계획은 일부만 반영됐다

**근거:** `tasks/task4.md` 32~45행, `tasks/task7.md` 69~73행

“동시 가입 검증”이라고 적었지만 실제 시나리오는 순차 재가입이다. 이 테스트만으로는 두 요청이 선행 중복 검사를 동시에 통과한 상황의 DB 제약 처리를 검증할 수 없다.

**권장 수정:**

- 같은 정규화 이메일로 두 가입 요청을 동시에 실행한다.
- 하나는 `201 Created`, 다른 하나는 `409 Conflict`이고, 최종 회원 레코드는 한 개인지 확인한다.
- 실제 PostgreSQL에서 통합 테스트 또는 명시적인 검증을 수행한다.
- Backend·Frontend Docker 이미지를 각각 빌드하고 실행하는 검증을 추가한다.
- 실제 브라우저의 CORS preflight와 로그인·작성·삭제 흐름을 완료 조건에 넣는다.
- 배포 전 CI에서 백엔드 테스트와 프론트엔드 빌드를 통과시키도록 계획한다.

재배포 후 데이터 보존 확인은 반영됐지만, 위 항목들도 완료 조건에 포함해야 이전 검증 피드백을 충분히 반영했다고 볼 수 있다.

### 2.7 [낮음] 문서 정합성과 작업량 문제는 남아 있다

| 위치 | 남은 사항 | 권장 조치 |
| --- | --- | --- |
| `requirements.md` 40행 | 생성 대상에 `railway.json`이 남아 있다. | 대시보드 기반 배포 계획과 일치하도록 제거한다. |
| `tasks/task0.md`, `design.md`의 커밋 계획 | Task 0 대상 문서 10개는 재검토 시점에 총 820줄인데 하나의 예정 커밋이다. | 줄 수 추정을 삭제하는 대신 실제 논리 단위로 분리한다. |
| 회원·게시글 입력 DTO 명세 | 제목·닉네임·이메일의 구체적인 길이 검증이 빠져 있다. | DB 컬럼 길이와 맞는 입력 검증 및 오류 응답을 정의한다. |
| `tasks/task1.md` 운영 DB 설정 | `ddl-auto: update`가 유지됐다. | 최초 실습에서 허용할지 명시하고, Flyway 도입을 후속 과제로 둘지 결정한다. |

## 3. 권장 반영 순서

1. 운영 시크릿 검사를 프로필 문자열 비교에서 활성 프로필 판정으로 변경한다.
2. 목록 응답의 `authorId`와 페이징 JSON 계약을 확정한다.
3. 인증 헤더 누락 및 이메일 중복 제약 식별 규칙을 보완한다.
4. 실제 동시 가입·PostgreSQL·Docker·CORS·CI 검증을 완료 조건에 추가한다.
5. 문서의 잔여 설정, 입력 검증, 커밋 단위 계획을 정리한다.

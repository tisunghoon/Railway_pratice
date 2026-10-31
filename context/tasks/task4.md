# Task 4: 백엔드 모듈러 모놀리스 통합 테스트 작성 (피드백 1 반영)

## 1. 작업 목표 및 의도 (Why)
- **[피드백 1 반영 2.1] 복수 프로필 시크릿 검증 테스트**:
  - `prod,metrics` 프로필로 실행 시 개발용 시크릿으로 기동이 차단되는지 단위/컨텍스트 테스트로 검증한다.
- **[피드백 1 반영 2.6] 실제 동시 가입 경합 멀티스레드 테스트**:
  - `ExecutorService`를 사용하여 동일한 정규화 이메일로 2개의 가입 요청을 동시에 실행할 때, 하나는 `201 Created`, 다른 하나는 `409 Conflict`가 발생하고 DB에는 단 1건의 회원만 저장되는지 검증한다.
- **[피드백 1 반영 2.4] 헤더 누락 시 401 Unauthorized 테스트**:
  - `Authorization` 헤더 없이 `/api/auth/me`, `POST /api/posts`, `DELETE /api/posts/{id}` 요청 시 400이 아닌 정확히 `401 Unauthorized`가 반환되는지 확인한다.
- **[피드백 1 반영 2.5] 페이징 계약 및 경계값 검증**:
  - `page=-1` 또는 `size=0` 요청 시 `400 Bad Request` 확인
  - `PageResponse` 스키마(`items`, `page`, `size`, `totalElements`, `totalPages`) 확인 및 목록 아이템에 `authorId`가 포함되어 있고 `content`는 없는지 확인.

---

## 2. 커밋 정보
- **예정 커밋 메시지**: `feat: 회원 인증 및 게시판 모듈 통합 테스트 추가`
- **예상 변경 줄 수**: 약 260줄 (300~400줄 이내 기준 충족)

---

## 3. 생성/수정 파일 목록

| 파일 경로 | 역할 |
| :--- | :--- |
| `backend/src/test/java/com/example/board/shared/security/TokenPropertiesTest.java` | 복수 프로필(`prod,metrics`) 시크릿 검증 단위 테스트 |
| `backend/src/test/java/com/example/board/BoardApiIntegrationTest.java` | MockMvc 기반 동시 가입 경합, 헤더 누락 401, 페이징 계약, CRUD 전체 통합 테스트 |

---

## 4. 상세 테스트 시나리오 명세

1. **동시 가입 경합 테스트 (`ExecutorService`)**:
   - `CountDownLatch`로 2개 스레드를 동기화하여 동일 이메일(`test@example.com`) 가입 요청 동시 실행
   - 응답 상태 코드 집합이 `{201, 409}`인지 확인하고, 회원 리포지토리의 해당 이메일 카운트가 `1`인지 단언.
2. **헤더 누락 및 변조/만료 테스트**:
   - 헤더 없는 `/api/auth/me` 호출 → `401 Unauthorized` ("인증 헤더가 누락되었습니다.")
   - 변조 서명 토큰 호출 → `401 Unauthorized` ("인증 토큰 서명 검증에 실패했습니다.")
   - 만료 토큰 호출 → `401 Unauthorized` ("인증 토큰이 만료되었습니다.")
3. **페이징 경계값 및 스키마 테스트**:
   - `GET /api/posts?page=-1` → `400 Bad Request`
   - `GET /api/posts?size=0` → `400 Bad Request`
   - 게시글 생성 후 `GET /api/posts?page=0&size=10` → `items[0].authorId` 존재 확인, `items[0].content` 미존재 확인.
4. **권한별 게시글 삭제 테스트**:
   - 본인 작성 글 삭제 → `204 No Content`
   - 타인 작성 글 삭제 → `403 Forbidden`

---

## 5. 완료 조건 및 검증 방법 (Verification)
- [ ] `cd backend && ./gradlew test` 실행 시 모든 테스트 케이스(단위 + 동시성 통합 테스트)가 Green으로 통과하는지 확인
- [ ] 변경 줄 수 300~400줄 이내 확인 후 로컬 커밋

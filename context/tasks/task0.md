# Task 0: 프로젝트 요구사항·아키텍처 설계 및 작업 단위 계획 문서화 (피드백 1 반영)

## 1. 작업 목표 및 의도 (Why)
- `feedback1.md`의 지적 사항을 수렴하여 `context/requirements.md`, `context/design.md`, `context/tasks/task0.md ~ task7.md` 문서를 최종 정본으로 확정한다.
- **핵심 보완 사항 반영**:
  - `Environment.acceptsProfiles(Profiles.of("prod"))` 기반 복수 프로필 시크릿 검증
  - 목록 응답(`PostListItemResponse`)에 `authorId`를 포함하여 프론트엔드 본인 글 삭제 버튼 표시 조건 충족
  - `uk_members_email` 명시적 유니크 제약 및 DB 제약 식별 기반 안전한 `409 Conflict` 변환
  - `@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)`를 통한 헤더 누락 시 정상 `401 Unauthorized` 처리
  - `PageResponse<T>` 페이징 응답 계약 및 입력 파라미터 유효성 검증 규칙(`page >= 0`, `size >= 1`) 명문화
  - Executor 기반 실제 동시 가입 경합 테스트 및 PostgreSQL/Docker 검증 완료 조건 추가
  - `railway.json` 잔여 언급 제거 및 입력 길이 검증 규칙 명시

---

## 2. 커밋 정보
- **예정 커밋 메시지**: `docs: 피드백 반영 요구사항, 설계서 및 작업 계획 문서 최신화`
- **대상 파일 목록**:
  - `context/requirements.md`
  - `context/design.md`
  - `context/tasks/task0.md` ~ `context/tasks/task7.md`

---

## 3. 완료 조건 및 검증 방법 (Verification)
- [ ] `requirements.md`, `design.md`, `task0.md ~ task7.md` 간의 스키마·API 필드명(`authorId`, `PageResponse`)이 일치하는지 확인
- [ ] `feedback1.md`의 7가지 개선 사항이 전 태스크에 충실히 반영되었는지 교차 검증
- [ ] `git status`로 문서 파일 변경점 확인 후 로컬 커밋

# 실행 계획: 이메일 인증 게시판 풀스택 구축 및 Railway 배포 실습

## 1. 목표 및 배경 (Why)
- **배경**: 클라우드 인프라(Railway) 환경에서 단순 단일 서버가 아닌 **Frontend(React FSD) + Backend(Spring Boot 3 Modular Monolith) + Database(PostgreSQL)** 3-Tier 아키텍처의 멀티 서비스 배포를 직접 설계하고 운영하는 실습 환경이 필요함.
- **목표**: 
  1. 이메일 회원가입·로그인 및 페이징 게시판 풀스택 애플리케이션 구축
  2. Railway 환경변수(`VITE_API_URL`, `${{Postgres.PG*}}`), 동적 `PORT` 바인딩, CORS 연동, Actuator 헬스체크 구현
  3. Railway 공식 가이드에 맞춘 단계별 배포 매뉴얼(`README.md`) 작성

---

## 2. 변경 범위 (모듈 및 아키텍처)
- **문서**: `.github/` (이슈/PR 템플릿), `context/` (요구사항, 설계서, 피드백, 단위별 구현 계획), `README.md`
- **Backend (`backend/`)**:
  - `shared`: CORS 설정, `TokenProperties`(운영 시크릿 검증), `TokenProvider`, `PageResponse`, `GlobalExceptionHandler`
  - `modules.member`: 회원 엔티티(`uk_members_email`), 리포지토리, Command/Result, `MemberFacade`, `AuthController`
  - `modules.post`: 게시글 엔티티(작성자 닉네임 스냅샷, `authorId`), 페이징 CRUD, `PostController`
- **Frontend (`frontend/`)**:
  - FSD 구조: `app`, `pages/board`, `widgets/header`, `widgets/post-board`, `features/auth-by-email`, `features/create-post`, `features/delete-post`, `entities/session`, `entities/post`, `shared/api`
- **배포 설정**:
  - `backend/Dockerfile` (JDK 21 멀티스테이지)
  - `frontend/Dockerfile` (Node 22 LTS 빌드 + Nginx Alpine)
  - `frontend/nginx.conf.template` (동적 `${PORT}` 바인딩 및 SPA 라우팅)

---

## 3. 논리 단위 커밋 분해 계획 (`staged-commit-workflow`)

| 순서 | 논리 단위 작업 내용 | 예정 커밋 메시지 |
| :--- | :--- | :--- |
| **단위 0** | 피드백 반영 설계 문서 및 GitHub 이슈/PR 템플릿 추가 | `docs: 피드백 반영 요구사항, 설계서 및 작업 계획 문서 최신화` |
| **단위 1** | 백엔드 빌드 설정 및 `shared` 공통 모듈 구성 | `chore: 백엔드 빌드 설정 및 공통 모듈 구성` |
| **단위 2** | 회원 도메인 모듈 및 이메일 인증 API 구현 | `feat: 회원 도메인 모듈 및 이메일 인증 API 구현` |
| **단위 3** | 게시판 도메인 모듈 및 페이징 CRUD API 구현 | `feat: 게시판 도메인 모듈 및 페이징 CRUD API 구현` |
| **단위 4** | 회원 인증 및 게시판 모듈 통합 테스트 추가 | `feat: 회원 인증 및 게시판 모듈 통합 테스트 추가` |
| **단위 5** | 프론트엔드 FSD 기본 구조 및 `shared`·`entities` 레이어 구성 | `chore: 프론트엔드 FSD 기본 구조 및 shared·entities 레이어 구성` |
| **단위 6** | 프론트엔드 FSD `features`·`widgets`·`pages`·`app` 화면 구현 | `feat: FSD features·widgets·pages·app 레이어 화면 구현` |
| **단위 7** | 풀스택 Railway 배포 설정 및 단계별 실습 가이드 작성 | `docs: 풀스택 Railway 배포 설정 및 단계별 실습 가이드 작성` |

---

## 4. 테스트 및 검증 계획
1. **백엔드 빌드 및 단위/통합 테스트**:
   - `cd backend && ./gradlew test` (복수 프로필 시크릿 검증, 동시 가입 경합, 페이징 계약, 권한별 CRUD 전체 통과)
2. **프론트엔드 빌드 검증**:
   - `cd frontend && npm install && npm run build` (`dist/` 생성 확인)
3. **배포 설정 검증**:
   - Dockerfile 빌드 및 Nginx 템플릿 정합성 확인
4. **인도 게이트 (Push/PR 게이트)**:
   - 전체 테스트 통과 확인 후 사용자 최종 승인을 거쳐 `git push` 및 PR 생성

---

## 5. 완료 기준
- [ ] 모듈러 모놀리스 및 FSD 아키텍처 규칙 준수
- [ ] `./gradlew test` 및 `npm run build` 성공
- [ ] `README.md` 가이드에 따라 Railway에 PostgreSQL + Backend + Frontend 3개 서비스 정상 배포 가능 상태 확립
- [ ] PR 본문에 배경 → 의도 → 이유(Why) 서사 포함

# Task 7: Railway 멀티 서비스 배포 설정 및 단계별 실습 가이드 작성 (피드백 1 반영)

## 1. 작업 목표 및 의도 (Why)
- **[피드백 1 반영 2.2 & 2.7] Railway 대시보드 UI 공식 배포 가이드 확정**:
  - Deprecated된 `railway.json` / `railway.toml`은 완전히 배제하고, 공식 권장 방식인 Railway 대시보드 설정 절차로 실습을 표준화한다.
- **[피드백 1 반영 2.1 & 2.2] 환경변수 및 참조 변수 정합성 확보**:
  - Backend에 복수 프로필 호환 운영 시크릿 `AUTH_SECRET` 주입 안내
  - PostgreSQL 플러그인 연결 시 Railway 변수 참조 문법(`PGHOST=${{Postgres.PGHOST}}`, `PGPORT=${{Postgres.PGPORT}}` 등)을 단계별로 명시
- **[피드백 1 반영 2.6] 실제 배포 및 동작 검증 절차 완성**:
  - Docker 이미지 로컬 빌드 검증 (`docker build`)
  - 실제 브라우저 접속을 통한 CORS preflight 및 로그인·글쓰기·본인 삭제 동작 검증
  - Backend 서비스 재배포 후에도 PostgreSQL에 기존 게시글이 보존되는 영속성 검증
  - 배포 전 CI 파이프라인(GitHub Actions 등을 통한 테스트/빌드 선행 통과) 권장 가이드 추가.

---

## 2. 커밋 정보
- **예정 커밋 메시지**: `docs: 풀스택 Railway 배포 설정 및 단계별 실습 가이드 작성`
- **예상 변경 줄 수**: 약 260줄 (300~400줄 이내 기준 충족)

---

## 3. 생성/수정 파일 목록

| 파일 경로 | 역할 |
| :--- | :--- |
| `backend/Dockerfile` | JDK 21 빌드 + JRE 21 런타임 멀티스테이지 Dockerfile |
| `frontend/Dockerfile` | Node 22 LTS 빌드(`VITE_API_URL` 수신) + Nginx Alpine 런타임 Dockerfile |
| `frontend/nginx.conf.template` | Railway `${PORT}` 동적 리스닝 및 SPA 라우팅 설정 |
| `README.md` | 로컬 실행, Docker 빌드, Railway 3-Service 단계별 배포 및 검증 실습 매뉴얼 |

---

## 4. `README.md` 단계별 실습 가이드 구성

1. **Step 1: 로컬 개발 환경 및 Docker 빌드 검증**
   - 백엔드: `./gradlew bootRun` (8080 포트, H2 인메모리)
   - 프론트엔드: `npm run dev` (5173 포트)
   - 로컬 Docker 이미지 빌드 테스트:
     - `docker build -t board-backend ./backend`
     - `docker build -t board-frontend ./frontend`
2. **Step 2: GitHub 원격 저장소에 push**
   - 사용자가 직접 `git push origin main` 실행
3. **Step 3: Railway 대시보드에서 Project 생성 및 PostgreSQL 추가**
   - Railway 대시보드 → `New Project` → `Provision PostgreSQL` (서비스 이름 예: `Postgres`)
4. **Step 4: Backend 서비스 배포 및 참조 변수 연결**
   - `Add Service` → GitHub Repo 선택
   - **Settings**: Root Directory를 `/backend`로 설정
   - **Settings**: Healthcheck Path를 `/actuator/health`로 설정
   - **Variables**:
     - `SPRING_PROFILES_ACTIVE=prod`
     - `AUTH_SECRET=<32자 이상의 안전한 난수 문자열>`
     - `PGHOST=${{Postgres.PGHOST}}`
     - `PGPORT=${{Postgres.PGPORT}}`
     - `PGDATABASE=${{Postgres.PGDATABASE}}`
     - `PGUSER=${{Postgres.PGUSER}}`
     - `PGPASSWORD=${{Postgres.PGPASSWORD}}`
   - **Networking**: `Generate Domain`으로 백엔드 공개 HTTPS 도메인 생성
5. **Step 5: Frontend 서비스 배포**
   - `Add Service` → 동일 GitHub Repo 선택
   - **Settings**: Root Directory를 `/frontend`로 설정
   - **Variables**: `VITE_API_URL=https://<Step 4 백엔드 도메인>`
   - **Networking**: `Generate Domain`으로 프론트엔드 공개 HTTPS 도메인 생성
6. **Step 6: Backend 서비스에 Frontend CORS 도메인 허용**
   - Backend 서비스 Variables에 `CORS_ALLOWED_ORIGINS=https://<Step 5 프론트엔드 도메인>` 추가 후 재배포
7. **Step 7: 브라우저 접속 및 기능·재배포 영속성 확인**
   - 프론트엔드 도메인 접속 → 브라우저 개발자 도구(Network 탭)에서 CORS preflight (`OPTIONS 200`) 확인
   - 회원가입 → 로그인 → 글 작성 → 본인 글 삭제 버튼 노출 확인
   - Backend 서비스를 Railway 대시보드에서 `Redeploy`한 뒤 새로고침하여 데이터가 유지되는지 RDBMS 영속성 검증

---

## 5. 완료 조건 및 검증 방법 (Verification)
- [ ] deprecated된 `railway.json`이 완전히 배제되었는지 확인
- [ ] `README.md` 가이드의 변수명(`SPRING_PROFILES_ACTIVE`, `${{Postgres.PG*}}`, `AUTH_SECRET`, `CORS_ALLOWED_ORIGINS`, `VITE_API_URL`)이 실제 코드와 100% 일치하는지 확인
- [ ] 변경 줄 수 300~400줄 이내 확인 후 로컬 커밋 (`git push`는 하지 않고 사용자에게 안내)

# 이메일 인증 게시판 풀스택 구축 및 Railway 배포 실습 가이드

이 프로젝트는 **React 18 (Feature-Sliced Design) 프론트엔드**, **Spring Boot 3 (Modular Monolith) 백엔드**, 그리고 **PostgreSQL 관리형 데이터베이스**로 구성된 3-Tier 애플리케이션을 클라우드 PaaS 플랫폼인 **Railway**에 멀티 서비스로 배포하는 전 과정을 실습하기 위한 저장소입니다.

---

## 1. 시스템 아키텍처

```mermaid
flowchart LR
    subgraph Railway["Railway Project (단일 프로젝트 내 3개 서비스)"]
        direction LR
        FE["Frontend Service<br/>(Root: /frontend)<br/>React + Vite (FSD) + Nginx"]
        BE["Backend Service<br/>(Root: /backend)<br/>Spring Boot 3 (Modular Monolith)"]
        DB[("PostgreSQL Plugin<br/>(Railway Managed DB)")]
    end

    User["사용자 브라우저"] -->|HTTPS| FE
    User -->|REST API 요청<br/>VITE_API_URL| BE
    BE -->|CORS 허용<br/>CORS_ALLOWED_ORIGINS| User
    BE -->|Railway 참조 변수 연결<br/>${{Postgres.PG*}}| DB
```

- **Frontend**: FSD(`app`, `pages`, `widgets`, `features`, `entities`, `shared`) 계층 구조를 준수하는 React SPA
- **Backend**: `shared`, `modules.member`, `modules.post`로 격리된 모듈러 모놀리스 구조. 타 모듈 간 직접 JPA 연관관계를 맺지 않고 `MemberFacade` 공개 계약으로 연동
- **Database**: PostgreSQL (로컬 개발 환경에서는 H2 인메모리 프로필 제공)

---

## 2. 로컬 개발 환경 실행 방법

### 2.1 Backend 실행 (Java 21 필요)
기본적으로 `local` 프로필이 활성화되며 인메모리 H2 DB를 사용합니다.
```bash
cd backend
./gradlew bootRun
```
- 서버 포트: `8080`
- 헬스체크: `http://localhost:8080/actuator/health`

### 2.2 Frontend 실행 (Node 22 LTS 필요)
```bash
cd frontend
npm install
npm run dev
```
- 브라우저 접속: `http://localhost:5173`
- 기본 API 연결 대상: `http://localhost:8080`

### 2.3 전체 테스트 실행
```bash
# 백엔드 통합 테스트 (동시성 가입 경합, 페이징 계약, 시크릿 검증 등)
cd backend && ./gradlew test

# 프론트엔드 프로덕션 번들 빌드
cd frontend && npm run build
```

---

## 3. Railway 멀티 서비스 단계별 배포 가이드

> **참고**: Railway 공식 정책에 따라 `railway.json` / `railway.toml` 방식은 deprecated되었습니다. 아래 절차는 **Railway 대시보드 UI 공식 설정 가이드**를 따릅니다.

### Step 1: GitHub 원격 저장소에 코드 Push
```bash
git push -u origin feature/1-fullstack-board-railway
# 또는 main 브랜치 머지 후 main에 push
```

### Step 2: Railway 프로젝트 생성 및 PostgreSQL 데이터베이스 프로비저닝
1. [Railway 대시보드](https://railway.com/dashboard)에 로그인합니다.
2. **`+ New Project`** 버튼 클릭 → **`Provision PostgreSQL`** 선택.
3. 데이터베이스 서비스 카드(이름 기본값: `Postgres` 또는 `PostgreSQL`)가 생성됩니다.

### Step 3: Backend 서비스 배포 및 환경변수 주입
1. 같은 프로젝트 화면에서 우측 상단 **`+ Create`** (또는 `+ New`) → **`GitHub Repo`** 선택 → 이 저장소(`Railway_pratice`)를 선택합니다.
2. 생성된 서비스 카드를 클릭하고 **`Settings`** 탭으로 이동합니다:
   - **Service Name**: `backend` (또는 원하는 이름)
   - **Root Directory**: `/backend` 입력 후 저장
   - **Healthcheck Path**: `/actuator/health` 입력 후 저장
3. **`Variables`** 탭으로 이동하여 아래 환경변수를 등록합니다:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `AUTH_SECRET`: `32자 이상의 안전한 임의 난수 비밀키 문자열` (운영 시 필수)
   - **Railway 변수 참조 문법 (DB 서비스명이 `Postgres`인 경우)**:
     - `PGHOST`: `${{Postgres.PGHOST}}`
     - `PGPORT`: `${{Postgres.PGPORT}}`
     - `PGDATABASE`: `${{Postgres.PGDATABASE}}`
     - `PGUSER`: `${{Postgres.PGUSER}}`
     - `PGPASSWORD`: `${{Postgres.PGPASSWORD}}`
4. **`Networking`** 탭으로 이동하여 **`Generate Domain`**을 클릭해 백엔드 공개 HTTPS URL을 생성합니다 (예: `https://backend-production-xxxx.up.railway.app`).

### Step 4: Frontend 서비스 배포
1. 동일 프로젝트에서 **`+ Create`** → **`GitHub Repo`** → 같은 저장소(`Railway_pratice`)를 한 번 더 선택합니다.
2. 생성된 새 서비스 카드의 **`Settings`** 탭으로 이동합니다:
   - **Service Name**: `frontend`
   - **Root Directory**: `/frontend` 입력 후 저장
3. **`Variables`** 탭으로 이동하여 Step 3에서 발급받은 백엔드 도메인을 설정합니다:
   - `VITE_API_URL`: `https://backend-production-xxxx.up.railway.app`
4. **`Networking`** 탭으로 이동하여 **`Generate Domain`**을 클릭해 프론트엔드 공개 HTTPS URL을 생성합니다 (예: `https://frontend-production-yyyy.up.railway.app`).

### Step 5: Backend 서비스에 Frontend CORS 도메인 허용
1. **`backend`** 서비스의 **`Variables`** 탭으로 돌아갑니다.
2. 아래 환경변수를 추가합니다:
   - `CORS_ALLOWED_ORIGINS`: `https://frontend-production-yyyy.up.railway.app` (Step 4의 프론트엔드 도메인)
3. 환경변수가 추가되면 백엔드가 자동으로 재배포됩니다.

---

## 4. 최종 동작 및 재배포 영속성 확인

1. **브라우저 접속 및 통신 확인**:
   - 프론트엔드 도메인 접속 후 브라우저 개발자 도구(F12)의 Network 탭을 엽니다.
   - 첫 요청 시 CORS Preflight (`OPTIONS 200 OK`) 통과 여부를 확인합니다.
2. **기능 검증**:
   - `회원가입` 탭에서 이메일·비밀번호·닉네임 입력 후 가입 완료 → 토큰 발급 및 자동 로그인 확인
   - `새 게시글 작성` 폼에서 제목과 본문을 입력하고 등록 → 목록에 즉시 추가되는지 확인
   - 본인이 작성한 글 우측에만 `삭제` 버튼이 노출되는지 확인 및 삭제 동작 검증
3. **재배포 후 데이터 보존 확인 (RDBMS 영속성 검증)**:
   - Railway 대시보드에서 `backend` 서비스 카드의 **`Redeploy`** 버튼을 클릭하여 컨테이너를 재시작합니다.
   - 재배포 완료 후 프론트엔드 페이지를 새로고침했을 때, 이전에 작성한 게시글과 회원 데이터가 그대로 유지되는지 확인합니다.

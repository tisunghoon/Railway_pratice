# Task 5: 프론트엔드 React(Vite) 초기 설정 및 FSD `shared`·`entities` 레이어 구성 (피드백 1 반영)

## 1. 작업 목표 및 의도 (Why)
- React 18 + Vite 기반 프론트엔드 프로젝트를 초기화하고 FSD의 기반 레이어(`shared`, `entities`)를 구성한다.
- **[피드백 1 반영 2.2 & 2.5] 페이징 응답 계약 및 `authorId` 수신**:
  - `postApi.fetchPosts({ page = 0, size = 10 })`는 백엔드의 `PageResponse` 구조(`items`, `page`, `totalPages` 등)를 그대로 수신한다.
  - `PostCard` 엔티티 컴포넌트는 게시글 요약 정보와 `authorId`를 바인딩하며, 상세 본문 토글 및 상위 레이어에서 주입할 삭제 액션 슬롯(`actionSlot`)을 지원한다.
- **`sessionStorage`를 통한 Stateless 인증 토큰 관리**:
  - `localStorage`에 토큰을 저장/조회/삭제하는 순수 헬퍼 모듈 구축.

---

## 2. 커밋 정보
- **예정 커밋 메시지**: `chore: 프론트엔드 FSD 기본 구조 및 shared·entities 레이어 구성`
- **예상 변경 줄 수**: 약 240줄 (300~400줄 이내 기준 충족)

---

## 3. 생성/수정 파일 목록

| 파일 경로 | FSD 레이어 | 역할 |
| :--- | :---: | :--- |
| `frontend/package.json` | 설정 | React 18, Vite 빌드 설정 |
| `frontend/vite.config.js` | 설정 | Vite React 플러그인 및 개발 포트(5173) |
| `frontend/index.html` | 설정 | SPA HTML 엔트리 |
| `frontend/.env.example` | 설정 | `VITE_API_URL=http://localhost:8080` |
| `frontend/src/shared/config/env.js` | `shared` | `import.meta.env.VITE_API_URL` 정규화 |
| `frontend/src/shared/api/httpClient.js` | `shared` | Bearer 헤더 자동 첨부 및 에러 메시지 파싱 클라이언트 |
| `frontend/src/entities/session/model/sessionStorage.js` | `entities` | 토큰 `getToken`, `setToken`, `clearToken` 헬퍼 |
| `frontend/src/entities/session/api/authApi.js` | `entities` | `signup`, `login`, `fetchMe` API 모듈 |
| `frontend/src/entities/session/index.js` | `entities` | `session` Public API |
| `frontend/src/entities/post/api/postApi.js` | `entities` | 페이징 지원 `fetchPosts`, `fetchPostById`, `createPost`, `deletePost` |
| `frontend/src/entities/post/ui/PostCard.jsx` | `entities` | `authorId`, 작성자, 제목 렌더링 및 `actionSlot` 지원 카드 컴포넌트 |
| `frontend/src/entities/post/index.js` | `entities` | `post` Public API |

---

## 4. 완료 조건 및 검증 방법 (Verification)
- [ ] 레이어 간 의존성 방향 위반 여부 확인 (`entities`는 `shared`만 import)
- [ ] `cd frontend && npm install` 오류 없이 완료되는지 확인
- [ ] 변경 줄 수 300~400줄 이내 확인 후 로컬 커밋

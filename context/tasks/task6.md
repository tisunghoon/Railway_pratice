# Task 6: 프론트엔드 FSD `features`·`widgets`·`pages`·`app` 레이어 화면 구현 (피드백 1 반영)

## 1. 작업 목표 및 의도 (Why)
- **[피드백 1 반영 2.2] 본인 작성 글 삭제 버튼 조건부 렌더링**:
  - `PostBoardWidget`에서 `PostCard`의 `actionSlot`으로 `DeletePostButton`을 주입할 때, `currentMember && currentMember.id === post.authorId` 조건을 충족할 때만 버튼을 노출한다.
- **[피드백 1 반영 2.5] 페이징 네비게이션 구현**:
  - `PageResponse`의 `page`, `totalPages`, `first`, `last` 값을 활용하여 이전/다음 페이지 이동 버튼 및 현재 페이지 인디케이터를 렌더링한다.
- **[피드백 1 반영 2.7] 입력값 클라이언트 1차 유효성 검증**:
  - 회원가입 시 이메일 형식, 비밀번호 8자 이상, 닉네임 2자 이상 입력 검증
  - 게시글 작성 시 제목 1~150자, 본문 1자 이상 검증.
- 프로덕션 번들 빌드(`npm run build`)를 성공적으로 검증한다.

---

## 2. 커밋 정보
- **예정 커밋 메시지**: `feat: FSD features·widgets·pages·app 레이어 화면 구현`
- **예상 변경 줄 수**: 약 280줄 (300~400줄 이내 기준 충족)

---

## 3. 생성/수정 파일 목록

| 파일 경로 | FSD 레이어 | 역할 |
| :--- | :---: | :--- |
| `frontend/src/features/auth-by-email/ui/AuthForm.jsx` | `features` | 이메일·비밀번호·닉네임 입력 및 폼 유효성 검증 |
| `frontend/src/features/auth-by-email/index.js` | `features` | `auth-by-email` Public API |
| `frontend/src/features/create-post/ui/CreatePostForm.jsx` | `features` | 새 게시글 작성 폼 (제목 길이 검증) |
| `frontend/src/features/create-post/index.js` | `features` | `create-post` Public API |
| `frontend/src/features/delete-post/ui/DeletePostButton.jsx` | `features` | 본인 글에만 바인딩되는 삭제 버튼 |
| `frontend/src/features/delete-post/index.js` | `features` | `delete-post` Public API |
| `frontend/src/widgets/header/ui/HeaderWidget.jsx` | `widgets` | 서비스 타이틀, 사용자 닉네임, Stateless 로그아웃 버튼 |
| `frontend/src/widgets/header/index.js` | `widgets` | `header` Public API |
| `frontend/src/widgets/post-board/ui/PostBoardWidget.jsx` | `widgets` | 게시글 작성 폼 + 페이징 네비게이션 + `PostCard` + `DeletePostButton` 조합 |
| `frontend/src/widgets/post-board/index.js` | `widgets` | `post-board` Public API |
| `frontend/src/pages/board/ui/BoardPage.jsx` | `pages` | 세션 상태 복원 및 전체 레이아웃 구성 |
| `frontend/src/pages/board/index.js` | `pages` | `board` Public API |
| `frontend/src/app/styles/index.css` | `app` | 반응형 모던 게시판 UI 스타일 |
| `frontend/src/app/App.jsx` | `app` | 앱 루트 컴포넌트 |
| `frontend/src/main.jsx` | 엔트리 | React DOM 마운트 |

---

## 4. 완료 조건 및 검증 방법 (Verification)
- [ ] 본인 글에만 삭제 버튼이 노출되고, 타인 글이나 비로그인 상태에서는 삭제 버튼이 보이지 않는지 검증
- [ ] 페이징 버튼 클릭 시 다음/이전 페이지 글 목록이 정상 로드되는지 확인
- [ ] `cd frontend && npm run build` 정상 완료 확인
- [ ] 변경 줄 수 300~400줄 이내 확인 후 로컬 커밋

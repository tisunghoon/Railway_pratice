# Task 3: 백엔드 `post` 도메인 모듈 및 페이징 CRUD API 구현 (피드백 1 반영)

## 1. 작업 목표 및 의도 (Why)
- **[피드백 1 반영 2.2] 목록 응답에 `authorId` 포함**:
  - `PostListItemResponse`에 `authorId`를 포함하여, 프론트엔드 목록 화면에서 `currentMember?.id === post.authorId` 조건으로 본인 작성 글에만 삭제 버튼을 노출할 수 있도록 계약을 완성한다.
- **[피드백 1 반영 2.5] 페이징 파라미터 유효성 검증 및 공통 응답 계약**:
  - `page < 0` 또는 `size < 1`인 경우 `400 Bad Request` 예외(`ApiException`)를 발생시킨다.
  - `size > 50`은 최대치인 `50`으로 보정한다.
  - 목록 응답을 `PageResponse<PostListItemResponse>` 공통 레코드로 감싸서 반환한다.
- **[피드백 1 반영 2.4] 게시글 작성·삭제 시 헤더 누락 401 처리**:
  - `@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)`를 적용하여 누락 시 `401 Unauthorized`를 보장한다.
- **[피드백 1 반영 2.7] 입력값 유효성 검증**:
  - `PostCreateRequest`: `@NotBlank`, 제목 1~150자, 본문 1자 이상 검증.

---

## 2. 커밋 정보
- **예정 커밋 메시지**: `feat: 게시판 도메인 모듈 및 페이징 CRUD API 구현`
- **예상 변경 줄 수**: 약 260줄 (300~400줄 이내 기준 충족)

---

## 3. 생성/수정 파일 목록

| 파일 경로 | 계층 | 역할 |
| :--- | :---: | :--- |
| `backend/.../modules/post/domain/Post.java` | `domain` | 게시글 엔티티 (`authorId`, `authorNickname` 스냅샷) |
| `backend/.../modules/post/domain/PostRepository.java` | `domain` | `findAllByOrderByCreatedAtDescIdDesc(Pageable pageable)` 지원 리포지토리 |
| `backend/.../modules/post/application/command/CreatePostCommand.java` | `application` | 게시글 생성 유스케이스 입력 모델 |
| `backend/.../modules/post/application/PostService.java` | `application` | 페이징 목록 조회, 상세 조회, 작성, 본인 검증 삭제 비즈니스 로직 |
| `backend/.../modules/post/presentation/dto/PostCreateRequest.java` | `presentation` | 게시글 작성 HTTP 요청 DTO (길이 검증) |
| `backend/.../modules/post/presentation/dto/PostListItemResponse.java` | `presentation` | 목록용 DTO (**`authorId` 포함**, 본문 제외) |
| `backend/.../modules/post/presentation/dto/PostDetailResponse.java` | `presentation` | 상세/작성완료 DTO (본문 포함) |
| `backend/.../modules/post/presentation/PostController.java` | `presentation` | `/api/posts` 페이징/CRUD 컨트롤러 (`required = false` 헤더, `PageResponse` 반환) |

---

## 4. 핵심 구현 명세

### 4.1 `PostListItemResponse` 구조
```java
public record PostListItemResponse(
    Long id,
    String title,
    Long authorId,          // 프론트엔드 본인 글 삭제 버튼 표시 판별용
    String authorNickname,
    LocalDateTime createdAt
) {
    public static PostListItemResponse from(Post post) {
        return new PostListItemResponse(
            post.getId(),
            post.getTitle(),
            post.getAuthorId(),
            post.getAuthorNickname(),
            post.getCreatedAt()
        );
    }
}
```

### 4.2 `PostController`의 페이징 검증 및 헤더 처리
```java
@GetMapping
public ResponseEntity<PageResponse<PostListItemResponse>> getPosts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {
    if (page < 0) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
    }
    if (size < 1) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "페이지 크기는 1 이상이어야 합니다.");
    }
    int safeSize = Math.min(size, 50);

    Page<Post> postPage = postService.getPosts(PageRequest.of(page, safeSize));
    List<PostListItemResponse> items = postPage.getContent().stream()
            .map(PostListItemResponse::from)
            .toList();

    return ResponseEntity.ok(PageResponse.of(
            items,
            postPage.getNumber(),
            postPage.getSize(),
            postPage.getTotalElements(),
            postPage.getTotalPages(),
            postPage.isFirst(),
            postPage.isLast()
    ));
}

@PostMapping
public ResponseEntity<PostDetailResponse> createPost(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
        @Valid @RequestBody PostCreateRequest request
) {
    Long memberId = tokenProvider.resolveMemberId(authHeader);
    Post post = postService.createPost(memberId, request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED).body(PostDetailResponse.from(post));
}
```

---

## 5. 완료 조건 및 검증 방법 (Verification)
- [ ] `PostListItemResponse`에 `authorId` 필드가 정상 포함되어 반환되는지 확인
- [ ] `page < 0` 또는 `size < 1` 요청 시 `400 Bad Request` 에러 응답 확인
- [ ] `cd backend && ./gradlew compileJava` 성공 확인
- [ ] 변경 줄 수 300~400줄 이내 확인 후 로컬 커밋

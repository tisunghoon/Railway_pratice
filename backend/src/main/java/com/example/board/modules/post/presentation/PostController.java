package com.example.board.modules.post.presentation;

import com.example.board.modules.post.application.PostService;
import com.example.board.modules.post.domain.Post;
import com.example.board.modules.post.presentation.dto.PostCreateRequest;
import com.example.board.modules.post.presentation.dto.PostDetailResponse;
import com.example.board.modules.post.presentation.dto.PostListItemResponse;
import com.example.board.shared.error.ApiException;
import com.example.board.shared.response.PageResponse;
import com.example.board.shared.security.TokenProvider;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final TokenProvider tokenProvider;

    public PostController(PostService postService, TokenProvider tokenProvider) {
        this.postService = postService;
        this.tokenProvider = tokenProvider;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable Long id) {
        Post post = postService.getPost(id);
        return ResponseEntity.ok(PostDetailResponse.from(post));
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable Long id
    ) {
        Long memberId = tokenProvider.resolveMemberId(authHeader);
        postService.deletePost(memberId, id);
        return ResponseEntity.noContent().build();
    }
}

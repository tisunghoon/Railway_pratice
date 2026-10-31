package com.example.board.modules.post.presentation.dto;

import com.example.board.modules.post.domain.Post;
import java.time.LocalDateTime;

public record PostListItemResponse(
        Long id,
        String title,
        Long authorId,
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

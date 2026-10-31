package com.example.board.modules.post.presentation.dto;

import com.example.board.modules.post.application.command.CreatePostCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "게시글 제목은 필수 입력값입니다.")
        @Size(min = 1, max = 150, message = "게시글 제목은 1자 이상 150자 이하이어야 합니다.")
        String title,

        @NotBlank(message = "게시글 본문은 필수 입력값입니다.")
        String content
) {
    public CreatePostCommand toCommand() {
        return CreatePostCommand.of(title, content);
    }
}

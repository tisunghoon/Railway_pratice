package com.example.board.modules.post.application.command;

public record CreatePostCommand(
        String title,
        String content
) {
    public static CreatePostCommand of(String title, String content) {
        return new CreatePostCommand(
                title != null ? title.trim() : "",
                content != null ? content.trim() : ""
        );
    }
}

package com.example.board.modules.post.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_author_id", columnList = "authorId"),
        @Index(name = "idx_posts_created_at", columnList = "createdAt")
})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 50)
    private String authorNickname;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Post() {
    }

    private Post(String title, String content, Long authorId, String authorNickname, LocalDateTime createdAt) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
        this.createdAt = createdAt;
    }

    public static Post create(String title, String content, Long authorId, String authorNickname) {
        return new Post(title, content, authorId, authorNickname, LocalDateTime.now());
    }

    public boolean isWrittenBy(Long memberId) {
        return this.authorId != null && this.authorId.equals(memberId);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

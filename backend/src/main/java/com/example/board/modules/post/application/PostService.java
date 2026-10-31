package com.example.board.modules.post.application;

import com.example.board.modules.member.application.MemberFacade;
import com.example.board.modules.member.application.MemberInfo;
import com.example.board.modules.post.application.command.CreatePostCommand;
import com.example.board.modules.post.domain.Post;
import com.example.board.modules.post.domain.PostRepository;
import com.example.board.shared.error.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final MemberFacade memberFacade;

    public PostService(PostRepository postRepository, MemberFacade memberFacade) {
        this.postRepository = postRepository;
        this.memberFacade = memberFacade;
    }

    @Transactional(readOnly = true)
    public Page<Post> getPosts(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    @Transactional
    public Post createPost(Long memberId, CreatePostCommand command) {
        MemberInfo author = memberFacade.getMemberById(memberId);
        Post post = Post.create(command.title(), command.content(), author.id(), author.nickname());
        return postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        if (!post.isWrittenBy(memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인이 작성한 게시글만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }
}

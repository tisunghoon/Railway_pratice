import React from 'react';
import { PostCard } from '../../../entities/post';
import { CreatePostForm } from '../../../features/create-post';
import { DeletePostButton } from '../../../features/delete-post';

export const PostBoardWidget = ({
  posts,
  page,
  totalPages,
  onPageChange,
  currentMember,
  onPostCreated,
  onPostDeleted
}) => {
  return (
    <section className="post-board-section">
      {currentMember ? (
        <CreatePostForm onPostCreated={onPostCreated} />
      ) : (
        <div className="login-prompt-banner">
          게시글을 작성하려면 상단에서 로그인 또는 회원가입을 진행해 주세요.
        </div>
      )}

      <div className="posts-container">
        <h2 className="section-title">최신 게시글 목록</h2>

        {posts.length === 0 ? (
          <div className="empty-state">등록된 게시글이 없습니다. 첫 글을 남겨보세요!</div>
        ) : (
          <div className="posts-list">
            {posts.map((post) => {
              const isOwner = currentMember && currentMember.id === post.authorId;
              const actionSlot = isOwner ? (
                <DeletePostButton postId={post.id} onPostDeleted={onPostDeleted} />
              ) : null;

              return <PostCard key={post.id} post={post} actionSlot={actionSlot} />;
            })}
          </div>
        )}

        {totalPages > 1 && (
          <div className="pagination-bar">
            <button
              type="button"
              className="page-btn"
              disabled={page <= 0}
              onClick={() => onPageChange(page - 1)}
            >
              이전
            </button>
            <span className="page-indicator">
              {page + 1} / {totalPages}
            </span>
            <button
              type="button"
              className="page-btn"
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
            >
              다음
            </button>
          </div>
        )}
      </div>
    </section>
  );
};

import React, { useState } from 'react';
import { postApi } from '../api/postApi';

export const PostCard = ({ post, actionSlot }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [content, setContent] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleToggleContent = async () => {
    if (isExpanded) {
      setIsExpanded(false);
      return;
    }

    if (content !== null) {
      setIsExpanded(true);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const detail = await postApi.fetchPostById(post.id);
      setContent(detail.content);
      setIsExpanded(true);
    } catch (err) {
      setError(err.message || '본문을 불러오지 못했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const formattedDate = post.createdAt
    ? new Date(post.createdAt).toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    : '';

  return (
    <article className="post-card">
      <div className="post-card-header">
        <h3 className="post-title" onClick={handleToggleContent} role="button" tabIndex={0}>
          {post.title}
        </h3>
        {actionSlot && <div className="post-action-slot">{actionSlot}</div>}
      </div>
      <div className="post-meta">
        <span className="post-author">작성자: {post.authorNickname}</span>
        <span className="post-date">{formattedDate}</span>
      </div>
      {isLoading && <p className="post-loading">본문 로딩 중...</p>}
      {error && <p className="post-error">{error}</p>}
      {isExpanded && content && (
        <div className="post-body">
          <p>{content}</p>
        </div>
      )}
    </article>
  );
};

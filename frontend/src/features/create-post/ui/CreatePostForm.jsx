import React, { useState } from 'react';
import { postApi } from '../../../entities/post';

export const CreatePostForm = ({ onPostCreated }) => {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!title.trim() || !content.trim()) {
      setError('제목과 본문을 모두 입력해 주세요.');
      return;
    }

    if (title.trim().length > 150) {
      setError('제목은 최대 150자까지 입력 가능합니다.');
      return;
    }

    try {
      setIsLoading(true);
      await postApi.createPost({
        title: title.trim(),
        content: content.trim()
      });
      setTitle('');
      setContent('');
      if (onPostCreated) {
        onPostCreated();
      }
    } catch (err) {
      setError(err.message || '게시글 작성에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="create-post-card">
      <h3 className="section-subtitle">새 게시글 작성</h3>
      <form onSubmit={handleSubmit} className="create-post-form">
        <div className="form-group">
          <input
            type="text"
            placeholder="제목을 입력하세요 (최대 150자)"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={150}
            required
          />
        </div>
        <div className="form-group">
          <textarea
            rows="4"
            placeholder="본문 내용을 입력하세요"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            required
          />
        </div>
        {error && <div className="alert-box error">{error}</div>}
        <button type="submit" className="submit-btn primary" disabled={isLoading}>
          {isLoading ? '등록 중...' : '게시글 등록'}
        </button>
      </form>
    </div>
  );
};

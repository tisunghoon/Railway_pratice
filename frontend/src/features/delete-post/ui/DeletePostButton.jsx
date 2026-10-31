import React, { useState } from 'react';
import { postApi } from '../../../entities/post';

export const DeletePostButton = ({ postId, onPostDeleted }) => {
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDelete = async () => {
    if (!window.confirm('정말 이 게시글을 삭제하시겠습니까?')) {
      return;
    }

    try {
      setIsDeleting(true);
      await postApi.deletePost(postId);
      if (onPostDeleted) {
        onPostDeleted(postId);
      }
    } catch (err) {
      alert(err.message || '게시글 삭제에 실패했습니다.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <button
      type="button"
      className="delete-btn"
      onClick={handleDelete}
      disabled={isDeleting}
      title="게시글 삭제"
    >
      {isDeleting ? '삭제 중...' : '삭제'}
    </button>
  );
};

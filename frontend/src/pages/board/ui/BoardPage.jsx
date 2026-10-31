import React, { useState, useEffect, useCallback } from 'react';
import { HeaderWidget } from '../../../widgets/header';
import { PostBoardWidget } from '../../../widgets/post-board';
import { AuthForm } from '../../../features/auth-by-email';
import { authApi, sessionStorage } from '../../../entities/session';
import { postApi } from '../../../entities/post';

export const BoardPage = () => {
  const [currentMember, setCurrentMember] = useState(null);
  const [posts, setPosts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isSessionLoading, setIsSessionLoading] = useState(true);

  const loadPosts = useCallback(async (targetPage = 0) => {
    try {
      const data = await postApi.fetchPosts({ page: targetPage, size: 10 });
      setPosts(data.items || []);
      setPage(data.page || 0);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      console.error('게시글 목록 로드 실패:', err);
    }
  }, []);

  const restoreSession = useCallback(async () => {
    const token = sessionStorage.getToken();
    if (!token) {
      setIsSessionLoading(false);
      return;
    }

    try {
      const member = await authApi.fetchMe();
      setCurrentMember(member);
    } catch (err) {
      console.warn('세션 만료 또는 유효하지 않은 토큰:', err);
      sessionStorage.clearToken();
      setCurrentMember(null);
    } finally {
      setIsSessionLoading(false);
    }
  }, []);

  useEffect(() => {
    restoreSession();
    loadPosts(0);
  }, [restoreSession, loadPosts]);

  const handleAuthSuccess = (member) => {
    setCurrentMember(member);
    loadPosts(page);
  };

  const handleLogout = () => {
    setCurrentMember(null);
  };

  const handlePostCreated = () => {
    loadPosts(0);
  };

  const handlePostDeleted = () => {
    loadPosts(page);
  };

  const handlePageChange = (newPage) => {
    loadPosts(newPage);
  };

  return (
    <div className="board-page">
      <HeaderWidget currentMember={currentMember} onLogout={handleLogout} />

      <main className="main-content">
        {!isSessionLoading && !currentMember && (
          <section className="auth-section">
            <AuthForm onAuthSuccess={handleAuthSuccess} />
          </section>
        )}

        <PostBoardWidget
          posts={posts}
          page={page}
          totalPages={totalPages}
          onPageChange={handlePageChange}
          currentMember={currentMember}
          onPostCreated={handlePostCreated}
          onPostDeleted={handlePostDeleted}
        />
      </main>
    </div>
  );
};

import React, { useState } from 'react';
import { authApi, sessionStorage } from '../../../entities/session';

export const AuthForm = ({ onAuthSuccess }) => {
  const [isLoginMode, setIsLoginMode] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !password) {
      setError('이메일과 비밀번호를 모두 입력해 주세요.');
      return;
    }

    if (!isLoginMode) {
      if (password.length < 8 || password.length > 64) {
        setError('비밀번호는 8자 이상 64자 이하이어야 합니다.');
        return;
      }
      if (nickname.trim().length < 2 || nickname.trim().length > 50) {
        setError('닉네임은 2자 이상 50자 이하이어야 합니다.');
        return;
      }
    }

    try {
      setIsLoading(true);
      let res;
      if (isLoginMode) {
        res = await authApi.login({ email: email.trim(), password });
      } else {
        res = await authApi.signup({
          email: email.trim(),
          password,
          nickname: nickname.trim()
        });
      }

      sessionStorage.setToken(res.accessToken);
      if (onAuthSuccess) {
        onAuthSuccess(res.member);
      }
    } catch (err) {
      setError(err.message || '인증 처리에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-card">
      <div className="auth-tabs">
        <button
          type="button"
          className={`tab-btn ${isLoginMode ? 'active' : ''}`}
          onClick={() => {
            setIsLoginMode(true);
            setError(null);
          }}
        >
          로그인
        </button>
        <button
          type="button"
          className={`tab-btn ${!isLoginMode ? 'active' : ''}`}
          onClick={() => {
            setIsLoginMode(false);
            setError(null);
          }}
        >
          회원가입
        </button>
      </div>

      <form onSubmit={handleSubmit} className="auth-form">
        <div className="form-group">
          <label htmlFor="auth-email">이메일</label>
          <input
            id="auth-email"
            type="email"
            placeholder="user@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            maxLength={120}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="auth-password">비밀번호</label>
          <input
            id="auth-password"
            type="password"
            placeholder={isLoginMode ? '비밀번호 입력' : '8자 이상 64자 이하'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        {!isLoginMode && (
          <div className="form-group">
            <label htmlFor="auth-nickname">닉네임</label>
            <input
              id="auth-nickname"
              type="text"
              placeholder="2자 이상 50자 이하"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              maxLength={50}
              required
            />
          </div>
        )}

        {error && <div className="alert-box error">{error}</div>}

        <button type="submit" className="submit-btn" disabled={isLoading}>
          {isLoading ? '처리 중...' : isLoginMode ? '로그인하기' : '회원가입 완료하기'}
        </button>
      </form>
    </div>
  );
};

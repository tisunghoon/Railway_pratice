import React from 'react';
import { sessionStorage } from '../../../entities/session';

export const HeaderWidget = ({ currentMember, onLogout }) => {
  const handleLogoutClick = () => {
    sessionStorage.clearToken();
    if (onLogout) {
      onLogout();
    }
  };

  return (
    <header className="app-header">
      <div className="header-inner">
        <div className="brand">
          <h1 className="logo-title">Railway Board</h1>
          <span className="badge">Fullstack Demo</span>
        </div>
        <div className="header-actions">
          {currentMember ? (
            <div className="user-profile">
              <span className="user-greeting">
                <strong>{currentMember.nickname}</strong> 님
              </span>
              <button
                type="button"
                className="logout-btn"
                onClick={handleLogoutClick}
              >
                로그아웃
              </button>
            </div>
          ) : (
            <span className="guest-note">게스트 모드 (로그인 필요)</span>
          )}
        </div>
      </div>
    </header>
  );
};

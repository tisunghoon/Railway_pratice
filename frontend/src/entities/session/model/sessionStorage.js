const TOKEN_KEY = 'board_access_token';

export const sessionStorage = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  setToken: (token) => {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    }
  },
  clearToken: () => {
    localStorage.removeItem(TOKEN_KEY);
  }
};

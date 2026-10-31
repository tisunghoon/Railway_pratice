import { httpClient } from '../../../shared/api/httpClient';

export const authApi = {
  signup: (payload) => httpClient.post('/api/auth/signup', payload),
  login: (payload) => httpClient.post('/api/auth/login', payload),
  fetchMe: () => httpClient.get('/api/auth/me')
};

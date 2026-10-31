import { httpClient } from '../../../shared/api/httpClient';

export const postApi = {
  fetchPosts: ({ page = 0, size = 10 } = {}) =>
    httpClient.get(`/api/posts?page=${page}&size=${size}`),
  fetchPostById: (id) => httpClient.get(`/api/posts/${id}`),
  createPost: (payload) => httpClient.post('/api/posts', payload),
  deletePost: (id) => httpClient.delete(`/api/posts/${id}`)
};

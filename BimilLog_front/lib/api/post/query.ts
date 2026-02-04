import { apiClient } from '../client'
import { Post, SimplePost } from '@/types/domains/post'
import { ApiResponse, PageResponse, CursorPageResponse } from '@/types/common'

export const postQuery = {
  // 커서 기반 페이징 (전체 게시글 목록)
  getAll: (cursor?: number | null, size = 20): Promise<ApiResponse<CursorPageResponse<SimplePost>>> => {
    const params = new URLSearchParams({ size: String(size) });
    if (cursor != null) params.set('cursor', String(cursor));
    return apiClient.get(`/api/post?${params}`);
  },

  getById: (postId: number): Promise<ApiResponse<Post>> =>
    apiClient.get<Post>(`/api/post/${postId}`),
  
  search: (type: "TITLE" | "TITLE_CONTENT" | "WRITER", query: string, page = 0, size = 10): Promise<ApiResponse<PageResponse<SimplePost>>> => {
    return apiClient.get(
      `/api/post/search?type=${type}&query=${encodeURIComponent(query)}&page=${page}&size=${size}`
    )
  },

  getRealtimePosts: (page = 0, size = 5): Promise<ApiResponse<PageResponse<SimplePost>>> =>
    apiClient.get(`/api/post/realtime?page=${page}&size=${size}`),

  getWeeklyPosts: (page = 0, size = 10): Promise<ApiResponse<PageResponse<SimplePost>>> =>
    apiClient.get(`/api/post/weekly?page=${page}&size=${size}`),

  getLegend: (page = 0, size = 10): Promise<ApiResponse<PageResponse<SimplePost>>> =>
    apiClient.get(`/api/post/legend?page=${page}&size=${size}`),

  getNoticePosts: (page = 0, size = 10): Promise<ApiResponse<PageResponse<SimplePost>>> =>
    apiClient.get(`/api/post/notice?page=${page}&size=${size}`),
}

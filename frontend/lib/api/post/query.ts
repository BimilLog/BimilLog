import { apiClient } from '../client'
import { Post, SimplePost } from '@/types/domains/post'
import { ApiResponse, PageResponse } from '@/types/common'

export const postQuery = {
  getAll: (page = 0, size = 10): Promise<ApiResponse<PageResponse<SimplePost>>> => 
    apiClient.get(`/api/post?page=${page}&size=${size}`),
  
  getById: (postId: number): Promise<ApiResponse<Post>> => 
    apiClient.get(`/api/post/${postId}`),
  
  search: (type: "TITLE" | "TITLE_CONTENT" | "WRITER", query: string, page = 0, size = 10): Promise<ApiResponse<PageResponse<SimplePost>>> => {
    return apiClient.get(
      `/api/post/search?type=${type}&query=${encodeURIComponent(query)}&page=${page}&size=${size}`
    )
  },

  getRealtimePosts: (): Promise<ApiResponse<SimplePost[]>> =>
    apiClient.get("/api/post/realtime"),

  getWeeklyPosts: (): Promise<ApiResponse<SimplePost[]>> =>
    apiClient.get("/api/post/weekly"),

  getLegend: (page = 0, size = 10): Promise<ApiResponse<PageResponse<SimplePost>>> =>
    apiClient.get(`/api/post/legend?page=${page}&size=${size}`),

  getNoticePosts: (): Promise<ApiResponse<SimplePost[]>> =>
    apiClient.get("/api/post/notice"),
}
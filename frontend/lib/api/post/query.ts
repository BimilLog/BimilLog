import { apiClient } from '../client'
import { Post, SimplePost } from '@/types/domains/post'
import { ApiResponse, PageResponse } from '@/types/common'

export const postQuery = {
  getAll: (page = 0, size = 10): Promise<ApiResponse<PageResponse<SimplePost>>> => 
    apiClient.get(`/api/post?page=${page}&size=${size}`),
  
  getById: (postId: number): Promise<ApiResponse<Post>> => 
    apiClient.get(`/api/post/${postId}`),
  
  search: (type: "TITLE" | "TITLE_CONTENT" | "AUTHOR", query: string, page = 0, size = 10): Promise<ApiResponse<PageResponse<SimplePost>>> => {
    const typeMap: Record<string, string> = {
      "TITLE": "title",
      "AUTHOR": "writer",
      "TITLE_CONTENT": "title_content"
    }
    const backendType = typeMap[type] || "title"
    
    return apiClient.get(
      `/api/post/search?type=${backendType}&query=${encodeURIComponent(query)}&page=${page}&size=${size}`
    )
  },

  getPopular: (): Promise<ApiResponse<{ realtime: SimplePost[]; weekly: SimplePost[] }>> =>
    apiClient.get("/api/post/popular"),
}
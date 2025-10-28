import { apiClient } from '../client'
import { Comment } from '@/types/domains/comment'
import { ApiResponse, PageResponse } from '@/types/common'

export const commentQuery = {
  getByPostId: (postId: number, page = 0): Promise<ApiResponse<PageResponse<Comment>>> => 
    apiClient.get(`/api/comment/${postId}?page=${page}`),
  
  getPopular: (postId: number): Promise<ApiResponse<Comment[]>> => 
    apiClient.get(`/api/comment/${postId}/popular`),
}
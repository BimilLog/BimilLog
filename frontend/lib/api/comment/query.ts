import { apiClient } from '../client'
import { Comment } from '@/types/domains/comment'
import { ApiResponse, PageResponse } from '@/types/api/common'

export const commentQuery = {
  getByPostId: (postId: number, page = 0): Promise<ApiResponse<PageResponse<Comment>>> => 
    apiClient.get(`/api/comment/query/list/${postId}?page=${page}`),
  
  getPopular: (postId: number): Promise<ApiResponse<Comment[]>> => 
    apiClient.get(`/api/comment/query/popular/${postId}`),
}
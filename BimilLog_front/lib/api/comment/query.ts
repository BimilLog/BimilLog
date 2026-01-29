import { apiClient } from '../client'
import { CommentDTO } from '@/types/domains/comment'
import { ApiResponse } from '@/types/common'

export const commentQuery = {
  getByPostId: (postId: number, page = 0): Promise<ApiResponse<CommentDTO>> =>
    apiClient.get(`/api/comment/${postId}?page=${page}`),
}
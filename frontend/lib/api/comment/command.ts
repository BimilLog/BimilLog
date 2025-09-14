import { apiClient } from '../client'
import { ApiResponse } from '@/types/common'

export const commentCommand = {
  create: (comment: {
    postId: number
    content: string
    parentId?: number
    password?: number
  }): Promise<ApiResponse<void>> => {
    const payload = {
      postId: comment.postId,
      content: comment.content,
      parentId: comment.parentId,
      password: comment.password?.toString().padStart(4, '0')
    }
    return apiClient.post("/api/comment/write", payload)
  },

  update: (data: { commentId: number; content: string; password?: number }): Promise<ApiResponse<void>> => {
    const payload = {
      id: data.commentId,  // Backend expects 'id', not 'commentId'
      content: data.content,
      password: data.password?.toString().padStart(4, '0')
    }
    return apiClient.post("/api/comment/update", payload)
  },

  delete: (data: { commentId: number; password?: number }): Promise<ApiResponse<void>> => {
    const payload = {
      id: data.commentId,  // Backend expects 'id', not 'commentId'
      password: data.password?.toString().padStart(4, '0')
    }
    return apiClient.post("/api/comment/delete", payload)
  },
  
  like: (commentId: number): Promise<ApiResponse<void>> =>
    apiClient.post("/api/comment/like", { commentId }),
}
import { apiClient } from '../client'
import { ApiResponse } from '@/types/api/common'

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
      password: comment.password
    }
    return apiClient.post("/api/comment/write", payload)
  },
  
  update: (commentId: number, data: { content: string; password?: number }): Promise<ApiResponse<void>> => {
    const payload = {
      content: data.content,
      password: data.password?.toString().padStart(4, '0')
    }
    return apiClient.put(`/api/comment/${commentId}`, payload)
  },
  
  delete: (commentId: number, password?: number): Promise<ApiResponse<void>> => {
    if (password !== undefined) {
      return apiClient.delete(`/api/comment/${commentId}?password=${password.toString().padStart(4, '0')}`)
    }
    return apiClient.delete(`/api/comment/${commentId}`)
  },
  
  like: (commentId: number): Promise<ApiResponse<void>> => 
    apiClient.post("/api/comment/like", { commentId }),
  
  cancelLike: (commentId: number): Promise<ApiResponse<void>> =>
    apiClient.delete("/api/comment/like", { commentId }),
}
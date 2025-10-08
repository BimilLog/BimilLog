import { apiClient } from '../client'
import { Post } from '@/types/domains/post'
import { ApiResponse } from '@/types/common'

export const postCommand = {
  create: (post: {
    title: string
    content: string
    password?: number
  }): Promise<ApiResponse<Post>> => {
    const payload: { title: string; content: string; password?: string } = {
      title: post.title,
      content: post.content
    }

    if (post.password !== undefined) {
      payload.password = post.password.toString().padStart(4, '0')
    }

    return apiClient.post<{ id: number }>("/api/post", payload).then(response => {
      if (response.success && response.data) {
        return { ...response, data: { id: response.data.id } as unknown as Post }
      }
      return response as ApiResponse<Post>
    })
  },
  
  update: (post: Post & { password?: number }): Promise<ApiResponse<void>> => {
    const payload: { title: string; content: string; password?: string } = {
      title: post.title,
      content: post.content
    }

    if (post.password !== undefined) {
      payload.password = post.password.toString().padStart(4, '0')
    }

    return apiClient.put(`/api/post/${post.id}`, payload)
  },
  
  delete: (postId: number): Promise<ApiResponse<void>> =>
    apiClient.delete(`/api/post/${postId}`),

  like: (postId: number): Promise<ApiResponse<void>> =>
    apiClient.post(`/api/post/${postId}/like`, {}),

  toggleNotice: (postId: number): Promise<ApiResponse<void>> =>
    apiClient.post(`/api/post/${postId}/notice`, {}),
}
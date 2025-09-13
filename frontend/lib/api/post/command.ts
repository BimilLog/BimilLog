import { apiClient } from '../client'
import { Post } from '@/types/domains/post'
import { ApiResponse } from '@/types/common'

export const postCommand = {
  create: (post: {
    userName: string | null
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
    
    return apiClient.post<{ id: number }>("/api/post/create", payload).then(response => {
      if (response.success && response.data) {
        return { ...response, data: { ...post, id: response.data.id } as Post }
      }
      return response as ApiResponse<Post>
    })
  },
  
  update: (post: Post): Promise<ApiResponse<void>> => {
    const payload = {
      title: post.title,
      content: post.content
    }
    return apiClient.put(`/api/post/update/${post.id}`, payload)
  },
  
  delete: (postId: number): Promise<ApiResponse<void>> => 
    apiClient.delete(`/api/post/delete/${postId}`),
  
  like: (postId: number): Promise<ApiResponse<void>> => 
    apiClient.post(`/api/post/like`, { postId }),
  
  cancelLike: (postId: number): Promise<ApiResponse<void>> =>
    apiClient.delete(`/api/post/unlike`, { postId }),

  toggleNotice: (postId: number): Promise<ApiResponse<boolean>> =>
    apiClient.post(`/api/post/notice/${postId}`, {}),
}
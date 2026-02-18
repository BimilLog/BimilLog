import { apiClient } from '../client'
import { Post } from '@/types/domains/post'
import { ApiResponse } from '@/types/common'

export const postCommand = {
  create: (post: {
    title: string
    content: string
    password?: number
  }): Promise<ApiResponse<Post>> => {
    const payload: { title: string; content: string; password?: number } = {
      title: post.title,
      content: post.content
    }

    if (post.password !== undefined) {
      payload.password = post.password;
    }

    return apiClient.post<{ id: number }>("/api/post", payload).then(response => {
      // apiClient가 Location 헤더에서 추출한 { id: 56 } 형태의 데이터를 그대로 사용
      console.log('[postCommand.create] API 응답:', response);
      if (response.success && response.data?.id) {
        console.log('[postCommand.create] 게시글 ID:', response.data.id);
        return {
          success: true,
          data: { id: response.data.id } as unknown as Post
        }
      }
      console.warn('[postCommand.create] 응답에 ID가 없음:', response);
      return response as ApiResponse<Post>
    })
  },
  
  update: (post: Post & { password?: number }): Promise<ApiResponse<void>> => {
    const payload: { title: string; content: string; password?: number } = {
      title: post.title,
      content: post.content
    }

    if (post.password !== undefined) {
      payload.password = post.password;
    }

    return apiClient.put(`/api/post/${post.id}`, payload)
  },
  
  delete: (postId: number, password?: number): Promise<ApiResponse<void>> => {
    let payload = undefined;
    if (password !== undefined) {
      payload = { password: password };
    }
    return apiClient.delete(`/api/post/${postId}`, payload);
  },

  like: (postId: number): Promise<ApiResponse<void>> =>
    apiClient.post(`/api/post/${postId}/like`, {}),

  toggleNotice: (postId: number, notice: boolean): Promise<ApiResponse<void>> =>
    apiClient.post(`/api/post/notice`, { postId, notice }),
}
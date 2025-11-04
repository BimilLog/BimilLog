import { apiClient } from '../client'
import { Setting, KakaoFriendList, SimpleMember } from '@/types/domains/user'
import { SimplePost, SimpleComment } from '@/types'
import { PageResponse } from '@/types/common'

export const userQuery = {
  checkUserName: (memberName: string) =>
    apiClient.get<boolean>(`/api/member/username/check?memberName=${encodeURIComponent(memberName)}`),

  getSettings: () =>
    apiClient.get<Setting>("/api/member/setting"),

  getFriendList: async (offset = 0, limit = 10) => {
    const response = await apiClient.get<KakaoFriendList>(`/api/member/friendlist?offset=${offset}&limit=${limit}`)

    if (!response.success) {
      throw new Error(response.error || '친구 목록을 불러올 수 없습니다.')
    }

    return response
  },

  getUserPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/post/me?page=${page}&size=${size}`),

  getUserComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/comment/me?page=${page}&size=${size}`),

  getUserLikedPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/post/me/liked?page=${page}&size=${size}`),

  getUserLikedComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/comment/me/liked?page=${page}&size=${size}`),

  getAllMembers: (page = 0, size = 20) =>
    apiClient.get<PageResponse<SimpleMember>>(`/api/member/all?page=${page}&size=${size}`),
}
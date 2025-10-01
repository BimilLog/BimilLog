import { apiClient } from '../client'
import { Setting, KakaoFriendList } from '@/types/domains/user'
import { SimplePost, SimpleComment } from '@/types'
import { PageResponse } from '@/types/common'

export const userQuery = {
  checkUserName: (userName: string) =>
    apiClient.get<boolean>(`/api/member/username/check?userName=${encodeURIComponent(userName)}`),

  getSettings: () =>
    apiClient.get<Setting>("/api/member/setting"),

  getFriendList: (offset = 0, limit = 10) =>
    apiClient.get<KakaoFriendList>(`/api/member/friendlist?offset=${offset}&limit=${limit}`),

  getUserPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/member/posts?page=${page}&size=${size}`),

  getUserComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/member/comments?page=${page}&size=${size}`),

  getUserLikedPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/member/likeposts?page=${page}&size=${size}`),

  getUserLikedComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/member/likecomments?page=${page}&size=${size}`),
}
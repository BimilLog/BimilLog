import { apiClient } from '../client'
import { Setting, KakaoFriendList } from '@/types/domains/user'
import { SimplePost, SimpleComment } from '@/types'
import { PageResponse } from '@/types/api/common'

export const userQuery = {
  checkUserName: (userName: string) => 
    apiClient.get<boolean>(`/api/user/query/username/check?userName=${encodeURIComponent(userName)}`),
  
  getSettings: () => 
    apiClient.get<Setting>("/api/user/query/setting"),
  
  getFriendList: (offset = 0, limit = 10) => 
    apiClient.get<KakaoFriendList>(`/api/user/query/friendlist?offset=${offset}&limit=${limit}`),
  
  getUserPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/user/query/posts?page=${page}&size=${size}`),
  
  getUserComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/user/query/comments?page=${page}&size=${size}`),
  
  getUserLikedPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/user/query/liked-posts?page=${page}&size=${size}`),
  
  getUserLikedComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/user/query/liked-comments?page=${page}&size=${size}`),
}
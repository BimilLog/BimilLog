import { apiClient } from '../client'
import { Setting, KakaoFriendList } from '@/types/domains/user'
import { SimplePost, SimpleComment } from '@/types'
import { PageResponse } from '@/types/api/common'

export const userQuery = {
  checkUserName: (userName: string) => 
    apiClient.get<boolean>(`/api/user/username/check?userName=${encodeURIComponent(userName)}`),
  
  getSettings: () => 
    apiClient.get<Setting>("/api/user/setting"),
  
  getFriendList: (offset = 0, limit = 10) => 
    apiClient.get<KakaoFriendList>(`/api/user/friendlist?offset=${offset}&limit=${limit}`),
  
  getUserPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/user/posts?page=${page}&size=${size}`),
  
  getUserComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/user/comments?page=${page}&size=${size}`),
  
  getUserLikedPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/user/likeposts?page=${page}&size=${size}`),
  
  getUserLikedComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/user/likecomments?page=${page}&size=${size}`),
}
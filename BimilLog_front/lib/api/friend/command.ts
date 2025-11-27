/**
 * 친구 관련 명령 API (Command)
 * POST, PUT, DELETE 요청만 포함
 */

import { apiClient } from '../client';
import { ApiResponse } from '@/types/common';
import { SendFriendRequestDTO, FriendshipIdResponse } from '@/types/domains/friend';

export const friendCommand = {
  /**
   * 친구 요청 보내기
   * @param data 받는 사람 ID
   */
  sendRequest: async (
    data: SendFriendRequestDTO
  ): Promise<ApiResponse<FriendshipIdResponse>> => {
    return apiClient.post('/api/friend/send', data);
  },

  /**
   * 친구 요청 취소 (보낸 요청)
   * @param requestId 친구 요청 ID
   */
  cancelRequest: async (requestId: number): Promise<ApiResponse<void>> => {
    return apiClient.delete(`/api/friend/send/${requestId}`);
  },

  /**
   * 친구 요청 수락
   * @param requestId 친구 요청 ID
   */
  acceptRequest: async (requestId: number): Promise<ApiResponse<void>> => {
    return apiClient.post(`/api/friend/receive/${requestId}`);
  },

  /**
   * 친구 요청 거절
   * @param requestId 친구 요청 ID
   */
  rejectRequest: async (requestId: number): Promise<ApiResponse<void>> => {
    return apiClient.delete(`/api/friend/receive/${requestId}`);
  },

  /**
   * 친구 삭제 (친구 관계 끊기)
   * @param friendshipId 친구 관계 ID
   */
  removeFriend: async (friendshipId: number): Promise<ApiResponse<void>> => {
    return apiClient.delete(`/api/friend/friendship/${friendshipId}`);
  },
};

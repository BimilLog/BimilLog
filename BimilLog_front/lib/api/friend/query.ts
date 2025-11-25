/**
 * 친구 관련 조회 API (Query)
 * GET 요청만 포함
 */

import { apiClient } from '../client';
import { ApiResponse, PageResponse } from '@/types/common';
import { Friend, FriendRequest, RecommendedFriend } from '@/types/domains/friend';

export const friendQuery = {
  /**
   * 내 친구 목록 조회
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   */
  getMyFriends: async (
    page: number,
    size: number = 20
  ): Promise<ApiResponse<PageResponse<Friend>>> => {
    return apiClient.get(`/api/friend/list?page=${page}&size=${size}`);
  },

  /**
   * 받은 친구 요청 조회
   * @param page 페이지 번호
   * @param size 페이지 크기
   */
  getReceivedRequests: async (
    page: number,
    size: number = 20
  ): Promise<ApiResponse<PageResponse<FriendRequest>>> => {
    return apiClient.get(`/api/friend/receive?page=${page}&size=${size}`);
  },

  /**
   * 보낸 친구 요청 조회
   * @param page 페이지 번호
   * @param size 페이지 크기
   */
  getSentRequests: async (
    page: number,
    size: number = 20
  ): Promise<ApiResponse<PageResponse<FriendRequest>>> => {
    return apiClient.get(`/api/friend/send?page=${page}&size=${size}`);
  },

  /**
   * 추천 친구 조회 (⭐ 신규)
   * 2촌, 3촌 친구를 추천 점수별로 정렬하여 반환
   * @param page 페이지 번호
   * @param size 페이지 크기 (최대 10명 권장)
   */
  getRecommended: async (
    page: number,
    size: number = 10
  ): Promise<ApiResponse<PageResponse<RecommendedFriend>>> => {
    return apiClient.get(`/api/friend/recommend?page=${page}&size=${size}`);
  },
};

/**
 * 친구 관련 Query Hooks
 * TanStack Query를 사용한 데이터 조회 훅
 */

import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { friendQuery } from '@/lib/api/friend/query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import type { ApiResponse, PageResponse } from '@/types/common';
import type { Friend, ReceivedFriendRequest, SentFriendRequest, RecommendedFriend } from '@/types/domains/friend';

/**
 * 내 친구 목록 조회
 *
 * 라운드 9 B-002/B-009-B: queryKey 에 page/size 포함 →
 * 페이지네이션·관계 체크가 같은 슬롯을 덮어쓰지 않도록 분리.
 */
export const useMyFriends = (
  page: number = 0,
  size: number = 20,
  enabled: boolean = true,
  initialData?: PageResponse<Friend> | null,
) => {
  return useQuery({
    queryKey: queryKeys.friend.lists(page, size),
    queryFn: () => friendQuery.getMyFriends(page, size),
    staleTime: 5 * 60 * 1000,
    enabled,
    placeholderData: keepPreviousData,
    initialData: initialData ? { success: true, data: initialData } as ApiResponse<PageResponse<Friend>> : undefined,
    initialDataUpdatedAt: initialData ? Date.now() : undefined,
  });
};

/**
 * 받은 친구 요청 조회
 *
 * 라운드 9 B-009-A: keepPreviousData → 알림→received 진입 시 빈 상태 깜박임 방지.
 */
export const useReceivedFriendRequests = (
  page: number = 0,
  size: number = 20,
  enabled: boolean = true,
  initialData?: PageResponse<ReceivedFriendRequest> | null,
) => {
  return useQuery({
    queryKey: queryKeys.friend.receivedRequests(page, size),
    queryFn: () => friendQuery.getReceivedRequests(page, size),
    staleTime: 1 * 60 * 1000,
    enabled,
    placeholderData: keepPreviousData,
    initialData: initialData ? { success: true, data: initialData } as ApiResponse<PageResponse<ReceivedFriendRequest>> : undefined,
    initialDataUpdatedAt: initialData ? Date.now() : undefined,
  });
};

/**
 * 보낸 친구 요청 조회
 */
export const useSentFriendRequests = (
  page: number = 0,
  size: number = 20,
  enabled: boolean = true,
  initialData?: PageResponse<SentFriendRequest> | null,
) => {
  return useQuery({
    queryKey: queryKeys.friend.sentRequests(page, size),
    queryFn: () => friendQuery.getSentRequests(page, size),
    staleTime: 1 * 60 * 1000,
    enabled,
    placeholderData: keepPreviousData,
    initialData: initialData ? { success: true, data: initialData } as ApiResponse<PageResponse<SentFriendRequest>> : undefined,
    initialDataUpdatedAt: initialData ? Date.now() : undefined,
  });
};

/**
 * 추천 친구 조회
 */
export const useRecommendedFriends = (
  page: number = 0,
  size: number = 10,
  initialData?: PageResponse<RecommendedFriend> | null,
) => {
  return useQuery({
    queryKey: queryKeys.friend.recommended(page, size),
    queryFn: () => friendQuery.getRecommended(page, size),
    staleTime: 10 * 60 * 1000,
    placeholderData: keepPreviousData,
    initialData: initialData ? { success: true, data: initialData } as ApiResponse<PageResponse<RecommendedFriend>> : undefined,
    initialDataUpdatedAt: initialData ? Date.now() : undefined,
  });
};

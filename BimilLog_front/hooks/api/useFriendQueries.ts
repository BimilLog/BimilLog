/**
 * 친구 관련 Query Hooks
 * TanStack Query를 사용한 데이터 조회 훅
 */

import { useQuery } from '@tanstack/react-query';
import { friendQuery } from '@/lib/api/friend/query';
import { queryKeys } from '@/lib/tanstack-query/keys';

/**
 * 내 친구 목록 조회
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 */
export const useMyFriends = (page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: queryKeys.friend.lists(),
    queryFn: () => friendQuery.getMyFriends(page, size),
    staleTime: 5 * 60 * 1000, // 5분 - 친구 목록은 자주 변하지 않음
  });
};

/**
 * 받은 친구 요청 조회
 * @param page 페이지 번호
 * @param size 페이지 크기
 */
export const useReceivedFriendRequests = (page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: queryKeys.friend.receivedRequests(page, size),
    queryFn: () => friendQuery.getReceivedRequests(page, size),
    staleTime: 1 * 60 * 1000, // 1분 - 실시간성이 중요함
  });
};

/**
 * 보낸 친구 요청 조회
 * @param page 페이지 번호
 * @param size 페이지 크기
 */
export const useSentFriendRequests = (page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: queryKeys.friend.sentRequests(page, size),
    queryFn: () => friendQuery.getSentRequests(page, size),
    staleTime: 1 * 60 * 1000, // 1분
  });
};

/**
 * 추천 친구 조회 (⭐ 신규)
 * 2촌, 3촌 친구를 추천 점수별로 정렬하여 반환
 * @param page 페이지 번호
 * @param size 페이지 크기 (최대 10명 권장)
 */
export const useRecommendedFriends = (page: number = 0, size: number = 10) => {
  return useQuery({
    queryKey: queryKeys.friend.recommended(page, size),
    queryFn: () => friendQuery.getRecommended(page, size),
    staleTime: 10 * 60 * 1000, // 10분 - 추천 목록은 자주 변하지 않음
  });
};

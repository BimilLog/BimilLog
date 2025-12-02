"use client";

import { useMemo } from "react";
import {
  useMyFriends,
  useSentFriendRequests,
  useReceivedFriendRequests,
} from "@/hooks/api/useFriendQueries";

/**
 * 특정 사용자와의 친구 관계 상태를 확인하는 hook
 *
 * @param memberName - 확인할 사용자 이름
 * @param enabled - API 호출 활성화 여부 (기본값: true)
 * @returns 친구 관계 상태 정보
 *
 * @example
 * const { isFriend, hasSentRequest, targetMemberId } = useFriendRelationshipCheck("홍길동", true);
 */
export const useFriendRelationshipCheck = (memberName: string, enabled: boolean = true) => {
  // 3개 API 동시 조회 (TanStack Query 캐싱으로 최적화)
  const { data: friendsResponse, isLoading: isLoadingFriends } = useMyFriends(0, 100, enabled);
  const { data: sentResponse, isLoading: isLoadingSent } = useSentFriendRequests(0, 100, enabled);
  const { data: receivedResponse, isLoading: isLoadingReceived } = useReceivedFriendRequests(0, 100, enabled);

  // useMemo로 memberName 검색 및 관계 상태 계산
  const result = useMemo(() => {
    if (!memberName) {
      return {
        isFriend: false,
        hasSentRequest: false,
        hasReceivedRequest: false,
        sentRequestId: null,
        receivedRequestId: null,
        targetMemberId: null,
      };
    }

    // 1. 친구 목록에서 검색 (최우선)
    const friend = friendsResponse?.data?.content?.find(
      (f) => f.memberName === memberName
    );
    if (friend) {
      return {
        isFriend: true,
        hasSentRequest: false,
        hasReceivedRequest: false,
        sentRequestId: null,
        receivedRequestId: null,
        targetMemberId: friend.friendMemberId,
      };
    }

    // 2. 보낸 요청에서 검색
    const sentRequest = sentResponse?.data?.content?.find(
      (r) => r.receiverMemberName === memberName
    );
    if (sentRequest) {
      return {
        isFriend: false,
        hasSentRequest: true,
        hasReceivedRequest: false,
        sentRequestId: sentRequest.friendRequestId,
        receivedRequestId: null,
        targetMemberId: sentRequest.receiverMemberId,
      };
    }

    // 3. 받은 요청에서 검색
    const receivedRequest = receivedResponse?.data?.content?.find(
      (r) => r.senderMemberName === memberName
    );
    if (receivedRequest) {
      return {
        isFriend: false,
        hasSentRequest: false,
        hasReceivedRequest: true,
        sentRequestId: null,
        receivedRequestId: receivedRequest.friendRequestId,
        targetMemberId: receivedRequest.senderMemberId,
      };
    }

    // 4. 일반 상태 (관계 없음)
    return {
      isFriend: false,
      hasSentRequest: false,
      hasReceivedRequest: false,
      sentRequestId: null,
      receivedRequestId: null,
      targetMemberId: null,
    };
  }, [friendsResponse, sentResponse, receivedResponse, memberName]);

  return {
    ...result,
    isLoading: isLoadingFriends || isLoadingSent || isLoadingReceived,
  };
};

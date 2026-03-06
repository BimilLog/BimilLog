"use client";

import React from "react";
import { Button } from "flowbite-react";
import { UserPlus, UserMinus, UserCheck as UserCheckIcon, X } from "lucide-react";
import { useAuth } from "@/hooks";
import { useFriendRelationshipCheck } from "@/hooks/features/friend/useFriendRelationshipCheck";
import { useBlacklistCheck } from "@/hooks/features/blacklist/useBlacklistCheck";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";
import {
  useSendFriendRequestAction,
  useCancelFriendRequestAction,
  useAcceptFriendRequestAction,
  useRejectFriendRequestAction,
} from "@/hooks/actions/useFriendActions";
import { useToastStore } from "@/stores/toast.store";

interface FriendActionButtonsProps {
  nickname: string;
  ownerId?: number | null;
  isOwner?: boolean;
}

export const FriendActionButtons: React.FC<FriendActionButtonsProps> = React.memo(({
  nickname,
  ownerId,
  isOwner = false,
}) => {
  const { isAuthenticated } = useAuth();
  const showToast = useToastStore((state) => state.showToast);
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  // 친구 관계 확인
  const {
    isFriend,
    hasSentRequest,
    hasReceivedRequest,
    sentRequestId,
    receivedRequestId,
    targetMemberId,
    isLoading: isLoadingRelationship,
  } = useFriendRelationshipCheck(nickname);

  // 블랙리스트 확인
  const { isBlacklisted } = useBlacklistCheck(nickname);

  // 친구 요청 Actions
  const { sendRequest, isPending: isSendPending } = useSendFriendRequestAction();
  const { cancelRequest, isPending: isCancelPending } = useCancelFriendRequestAction();
  const { acceptRequest, isPending: isAcceptPending } = useAcceptFriendRequestAction();
  const { rejectRequest, isPending: isRejectPending } = useRejectFriendRequestAction();

  // targetMemberId가 없으면 messages에서 추출한 ownerId 사용
  const finalMemberId = targetMemberId ?? ownerId;

  // 친구 요청 보내기
  const handleSendRequest = async () => {
    if (!finalMemberId) {
      showToast({ type: 'error', message: '사용자 정보를 찾을 수 없습니다.' });
      return;
    }

    const confirmed = await confirm({
      title: "친구 요청",
      message: `${nickname}님에게 친구 요청을 보내시겠습니까?`,
      confirmText: "보내기",
      cancelText: "취소",
      icon: <UserPlus className="h-8 w-8 stroke-purple-600 fill-purple-100" />,
    });

    if (confirmed) {
      sendRequest(finalMemberId);
    }
  };

  // 친구 요청 취소
  const handleCancelRequest = async () => {
    if (!sentRequestId) return;

    const confirmed = await confirm({
      title: "친구 요청 취소",
      message: `${nickname}님에게 보낸 친구 요청을 취소하시겠습니까?`,
      confirmText: "취소",
      cancelText: "돌아가기",
      confirmButtonVariant: "destructive",
      icon: <UserMinus className="h-8 w-8 stroke-red-600 fill-red-100" />,
    });

    if (confirmed) {
      cancelRequest(sentRequestId);
    }
  };

  // 친구 요청 수락
  const handleAcceptRequest = () => {
    if (!receivedRequestId) return;
    acceptRequest(receivedRequestId);
  };

  // 친구 요청 거절
  const handleRejectRequest = async () => {
    if (!receivedRequestId) return;

    const confirmed = await confirm({
      title: "친구 요청 거절",
      message: `${nickname}님의 친구 요청을 거절하시겠습니까?`,
      confirmText: "거절",
      cancelText: "돌아가기",
      confirmButtonVariant: "destructive",
      icon: <X className="h-8 w-8 stroke-red-600 fill-red-100" />,
    });

    if (confirmed) {
      rejectRequest(receivedRequestId);
    }
  };

  const isPending =
    isSendPending ||
    isCancelPending ||
    isAcceptPending ||
    isRejectPending;

  // 관계 정보 로딩 중이면 버튼 표시 안함
  if (isLoadingRelationship) {
    return null;
  }

  // 버튼을 렌더링하지 않는 경우
  if (!isAuthenticated || isOwner || isFriend || isBlacklisted) {
    return null;
  }

  if (hasReceivedRequest) {
    // 받은 요청: 수락/거절 버튼
    return (
      <>
        <Button
          onClick={handleAcceptRequest}
          color="success"
          size="sm"
          disabled={isPending}
          className="text-xs"
        >
          <UserCheckIcon className="w-4 h-4 mr-1" />
          수락
        </Button>
        <Button
          onClick={handleRejectRequest}
          color="light"
          size="sm"
          disabled={isPending}
          className="text-xs text-red-600 hover:text-red-700"
        >
          <X className="w-4 h-4 mr-1" />
          거절
        </Button>
        <ConfirmModalComponent />
      </>
    );
  }

  if (hasSentRequest) {
    // 보낸 요청: 취소 버튼
    return (
      <>
        <Button
          onClick={handleCancelRequest}
          color="light"
          size="sm"
          disabled={isPending}
          className="text-xs text-orange-600 hover:text-orange-700"
        >
          <UserMinus className="w-4 h-4 mr-1" />
          {isPending ? "처리 중..." : "요청 취소"}
        </Button>
        <ConfirmModalComponent />
      </>
    );
  }

  // 일반: 친구 요청 버튼
  return (
    <>
      <Button
        onClick={handleSendRequest}
        color="purple"
        size="sm"
        disabled={isPending || !finalMemberId}
        className="text-xs"
      >
        <UserPlus className="w-4 h-4 mr-1" />
        {isPending ? "처리 중..." : "친구 요청"}
      </Button>
      <ConfirmModalComponent />
    </>
  );
});

FriendActionButtons.displayName = "FriendActionButtons";

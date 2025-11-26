"use client";

import React from "react";
import { Button } from "flowbite-react";
import { MessageSquare, Share2, List, UserPlus, UserMinus, UserCheck as UserCheckIcon, X } from "lucide-react";
import { KakaoShareButton } from "@/components";
import { useRollingPaperShare } from "@/hooks/features/useRollingPaperShare";
import { useAuth } from "@/hooks";
import { useFriendRelationshipCheck } from "@/hooks/features/friend/useFriendRelationshipCheck";
import { useBlacklistCheck } from "@/hooks/features/blacklist/useBlacklistCheck";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";
import {
  useSendFriendRequest,
  useCancelFriendRequest,
  useAcceptFriendRequest,
  useRejectFriendRequest,
} from "@/hooks/api/useFriendMutations";
import { useToastStore } from "@/stores/toast.store";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";

interface RollingPaperHeaderProps {
  nickname: string;
  messageCount: number;
  messages: (RollingPaperMessage | VisitMessage)[];
  isOwner?: boolean;
  onShowMessages?: () => void;
  className?: string;
}

export const RollingPaperHeader: React.FC<RollingPaperHeaderProps> = React.memo(({
  nickname,
  messageCount,
  messages,
  isOwner = false,
  onShowMessages,
  className = "",
}) => {
  const { user, isAuthenticated } = useAuth();
  const showToast = useToastStore((state) => state.showToast);
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  const { handleWebShare } = useRollingPaperShare({
    nickname,
    messageCount,
    isOwner,
  });

  // messages 배열에서 ownerId 추출 (모든 메시지는 동일한 ownerId를 가짐)
  const ownerId = messages.length > 0 && 'ownerId' in messages[0]
    ? messages[0].ownerId
    : null;

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

  // 친구 요청 Mutations
  const sendMutation = useSendFriendRequest();
  const cancelMutation = useCancelFriendRequest();
  const acceptMutation = useAcceptFriendRequest();
  const rejectMutation = useRejectFriendRequest();

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
      sendMutation.mutate({ receiverMemberId: finalMemberId });
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
      cancelMutation.mutate(sentRequestId);
    }
  };

  // 친구 요청 수락
  const handleAcceptRequest = () => {
    if (!receivedRequestId) return;
    acceptMutation.mutate(receivedRequestId);
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
      rejectMutation.mutate(receivedRequestId);
    }
  };

  const isPending =
    sendMutation.isPending ||
    cancelMutation.isPending ||
    acceptMutation.isPending ||
    rejectMutation.isPending;

  // 친구 요청 버튼 렌더링 로직
  const renderFriendButton = () => {
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
        </>
      );
    }

    if (hasSentRequest) {
      // 보낸 요청: 취소 버튼
      return (
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
      );
    }

    // 일반: 친구 요청 버튼
    return (
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
    );
  };

  return (
    <header className={`bg-white/80 backdrop-blur-md border-b ${className}`}>
      <div className="px-4 py-4">
        <div className="max-w-screen-xl mx-auto">
          {/* 데스크톱 레이아웃 */}
          <div className="hidden md:flex items-center justify-between">
            <div className="flex items-center space-x-2 flex-1 min-w-0">
              <div className="w-8 h-8 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center">
                <MessageSquare className="w-5 h-5 stroke-white fill-green-200" />
              </div>
              <div className="min-w-0 flex-1">
                <h1 className="font-bold text-gray-900 text-base truncate">
                  {nickname}님의 롤링페이퍼
                </h1>
              </div>
            </div>
            <div className="flex items-center space-x-2 flex-shrink-0">
              {onShowMessages && (
                <Button
                  onClick={onShowMessages}
                  color="blue"
                  size="sm"
                  className="text-xs"
                >
                  <List className="w-4 h-4 mr-1 stroke-slate-600 fill-slate-100" />
                  메시지 목록 보기
                </Button>
              )}
              {renderFriendButton()}
              <KakaoShareButton
                type="rollingPaper"
                userName={nickname}
                messageCount={messageCount}
                color="yellow"
                size="sm"
                className="text-xs"
              />
              <Button
                onClick={handleWebShare}
                color="gray"
                size="sm"
                className="text-xs"
              >
                <Share2 className="w-4 h-4 mr-1" />
                링크 공유
              </Button>
            </div>
          </div>

          {/* 모바일 레이아웃 */}
          <div className="md:hidden">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2 flex-1 min-w-0">
                <div className="w-7 h-7 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center">
                  <MessageSquare className="w-4 h-4 stroke-white fill-green-200" />
                </div>
                <h1 className="font-bold text-gray-900 text-sm truncate">
                  {nickname}님의 롤링페이퍼
                </h1>
              </div>
              <div className="flex items-center space-x-1 flex-shrink-0">
                {onShowMessages && (
                  <Button
                    onClick={onShowMessages}
                    color="blue"
                    size="sm"
                    className="px-2 py-1 text-xs h-7"
                  >
                    <List className="w-3 h-3 mr-1 stroke-slate-600 fill-slate-100" />
                    목록
                  </Button>
                )}
                {renderFriendButton()}
                <KakaoShareButton
                  type="rollingPaper"
                  userName={nickname}
                  messageCount={messageCount}
                  color="yellow"
                  size="sm"
                  className="px-2 py-1 text-xs h-7"
                />
                <Button
                  onClick={handleWebShare}
                  color="gray"
                  size="sm"
                  className="px-2 py-1 text-xs h-7"
                >
                  <Share2 className="w-3 h-3 mr-1" />
                  링크
                </Button>
              </div>
            </div>

            {/* 모바일 메시지 카운트 */}
            {messageCount > 0 && (
              <div className="mt-2 text-center">
                <span className="text-xs text-brand-muted">
                  총 {messageCount}개의 메시지
                </span>
              </div>
            )}
          </div>
        </div>
      </div>
      <ConfirmModalComponent />
    </header>
  );
});

RollingPaperHeader.displayName = "RollingPaperHeader";
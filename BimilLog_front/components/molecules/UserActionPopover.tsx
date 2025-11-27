"use client";

import React from "react";
import Link from "next/link";
import { Popover } from "flowbite-react";
import { User, ExternalLink, UserX, UserCheck, UserPlus, UserMinus, X } from "lucide-react";
import { Button } from "@/components";
import { useAuth } from "@/hooks/common/useAuth";
import { useToast } from "@/hooks/common/useToast";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";
import { useBlacklistCheck } from "@/hooks/features/blacklist/useBlacklistCheck";
import { useFriendRelationshipCheck } from "@/hooks/features/friend/useFriendRelationshipCheck";
import { useAddToBlacklist, useRemoveFromBlacklist } from "@/hooks/api/useBlacklistMutations";
import {
  useSendFriendRequest,
  useCancelFriendRequest,
  useAcceptFriendRequest,
  useRejectFriendRequest,
} from "@/hooks/api/useFriendMutations";

interface UserActionPopoverProps {
  memberName: string;
  memberId?: number;
  trigger: React.ReactNode;
  placement?: "top" | "bottom" | "left" | "right";
}

export const UserActionPopover: React.FC<UserActionPopoverProps> = ({
  memberName,
  memberId,
  trigger,
  placement = "bottom",
}) => {
  const { user, isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  // 블랙리스트 상태
  const { isBlacklisted, blacklistId } = useBlacklistCheck(memberName);

  // 친구 관계 상태
  const {
    isFriend,
    hasSentRequest,
    hasReceivedRequest,
    sentRequestId,
    receivedRequestId,
    targetMemberId,
    isLoading: isLoadingRelationship,
  } = useFriendRelationshipCheck(memberName);

  // 친구 요청 Mutations
  const sendMutation = useSendFriendRequest();
  const cancelMutation = useCancelFriendRequest();
  const acceptMutation = useAcceptFriendRequest();
  const rejectMutation = useRejectFriendRequest();

  // 블랙리스트 Mutations
  const addMutation = useAddToBlacklist();
  const removeMutation = useRemoveFromBlacklist();

  // 본인 여부 확인
  const isOwnProfile = user?.memberName === memberName;

  // 최종 memberId 결정 (Props 우선 → 훅에서 찾은 값)
  const finalMemberId = memberId ?? targetMemberId;

  // 핸들러 1: 친구 요청 보내기
  const handleSendRequest = async () => {
    if (!finalMemberId) {
      showToast({ type: 'error', message: '사용자 정보를 찾을 수 없습니다.' });
      return;
    }

    const confirmed = await confirm({
      title: "친구 요청",
      message: `${memberName}님에게 친구 요청을 보내시겠습니까?`,
      confirmText: "보내기",
      cancelText: "취소",
      icon: <UserPlus className="h-8 w-8 stroke-purple-600 fill-purple-100" />,
    });

    if (confirmed) {
      sendMutation.mutate({ receiverMemberId: finalMemberId });
    }
  };

  // 핸들러 2: 친구 요청 취소
  const handleCancelRequest = async () => {
    if (!sentRequestId) return;

    const confirmed = await confirm({
      title: "친구 요청 취소",
      message: `${memberName}님에게 보낸 친구 요청을 취소하시겠습니까?`,
      confirmText: "취소",
      cancelText: "돌아가기",
      confirmButtonVariant: "destructive",
      icon: <UserMinus className="h-8 w-8 stroke-red-600 fill-red-100" />,
    });

    if (confirmed) {
      cancelMutation.mutate(sentRequestId);
    }
  };

  // 핸들러 3: 친구 요청 수락 (모달 없음)
  const handleAcceptRequest = () => {
    if (!receivedRequestId) return;
    acceptMutation.mutate(receivedRequestId);
  };

  // 핸들러 4: 친구 요청 거절
  const handleRejectRequest = async () => {
    if (!receivedRequestId) return;

    const confirmed = await confirm({
      title: "친구 요청 거절",
      message: `${memberName}님의 친구 요청을 거절하시겠습니까?`,
      confirmText: "거절",
      cancelText: "돌아가기",
      confirmButtonVariant: "destructive",
      icon: <X className="h-8 w-8 stroke-red-600 fill-red-100" />,
    });

    if (confirmed) {
      rejectMutation.mutate(receivedRequestId);
    }
  };

  // 핸들러 5: 블랙리스트 추가
  const handleAddToBlacklist = async () => {
    const confirmed = await confirm({
      title: "블랙리스트 추가",
      message: `'${memberName}' 님을 블랙리스트에 추가하시겠습니까?\n\n이 사용자는 회원님의 롤링페이퍼에 메시지를 남길 수 없게 됩니다.`,
      confirmText: "추가",
      cancelText: "취소",
      confirmButtonVariant: "destructive",
      icon: <UserX className="h-8 w-8 stroke-red-600 fill-red-100" />,
    });

    if (confirmed) {
      addMutation.mutate(memberName);
    }
  };

  // 핸들러 6: 블랙리스트 제거
  const handleRemoveFromBlacklist = async () => {
    if (!blacklistId) return;

    const confirmed = await confirm({
      title: "블랙리스트 제거",
      message: `'${memberName}' 님을 블랙리스트에서 제거하시겠습니까?`,
      confirmText: "제거",
      cancelText: "취소",
      icon: <UserCheck className="h-8 w-8 stroke-green-600 fill-green-100" />,
    });

    if (confirmed) {
      removeMutation.mutate({ id: blacklistId, page: 0, size: 100 });
    }
  };

  const isPending =
    sendMutation.isPending ||
    cancelMutation.isPending ||
    acceptMutation.isPending ||
    rejectMutation.isPending ||
    addMutation.isPending ||
    removeMutation.isPending;

  const popoverContent = (
    <div className="p-3 w-56">
      <div className="flex flex-col space-y-2">
        {/* 사용자 정보 */}
        <div className="flex items-center space-x-2 mb-1">
          <User className="w-4 h-4" />
          <span className="font-medium">{memberName}</span>
        </div>

        {/* 롤링페이퍼 보기 */}
        <Link href={`/rolling-paper/${encodeURIComponent(memberName)}`}>
          <Button size="sm" className="w-full justify-start">
            <ExternalLink className="w-4 h-4 mr-2" />
            롤링페이퍼 보기
          </Button>
        </Link>

        {/* 조건부 버튼 렌더링 */}
        {isAuthenticated && !isOwnProfile && !isFriend && (
          <>
            {/* 친구 요청 관련 버튼 */}
            {hasReceivedRequest ? (
              // 3. 받은 요청 → 수락/거절 버튼
              <>
                <Button
                  size="sm"
                  color="success"
                  className="w-full justify-start"
                  onClick={handleAcceptRequest}
                  disabled={isPending}
                >
                  <UserCheck className="w-4 h-4 mr-2" />
                  {acceptMutation.isPending ? "처리 중..." : "친구 요청 수락"}
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="w-full justify-start text-red-600 hover:text-red-700 hover:bg-red-50 border-red-300"
                  onClick={handleRejectRequest}
                  disabled={isPending}
                >
                  <X className="w-4 h-4 mr-2" />
                  {rejectMutation.isPending ? "처리 중..." : "거절"}
                </Button>
              </>
            ) : hasSentRequest ? (
              // 4. 보낸 요청 → 취소 버튼
              <Button
                size="sm"
                variant="outline"
                className="w-full justify-start text-orange-600 hover:text-orange-700 hover:bg-orange-50 border-orange-300"
                onClick={handleCancelRequest}
                disabled={isPending}
              >
                <UserMinus className="w-4 h-4 mr-2" />
                {cancelMutation.isPending ? "처리 중..." : "친구 요청 취소"}
              </Button>
            ) : !isBlacklisted ? (
              // 6. 일반 → 친구 요청 버튼
              <Button
                size="sm"
                variant="outline"
                className="w-full justify-start text-purple-600 hover:text-purple-700 hover:bg-purple-50 border-purple-300"
                onClick={handleSendRequest}
                disabled={isPending || !finalMemberId}
              >
                <UserPlus className="w-4 h-4 mr-2" />
                {sendMutation.isPending ? "처리 중..." : "친구 요청"}
              </Button>
            ) : null}

            {/* 블랙리스트 버튼 (기존 로직 유지, 위치만 아래로) */}
            {isBlacklisted ? (
              // 5. 블랙리스트 → 제거 버튼만
              <Button
                size="sm"
                variant="outline"
                className="w-full justify-start text-green-600 hover:text-green-700 hover:bg-green-50 border-green-300"
                onClick={handleRemoveFromBlacklist}
                disabled={isPending}
              >
                <UserCheck className="w-4 h-4 mr-2" />
                {removeMutation.isPending ? "처리 중..." : "블랙리스트에서 제거"}
              </Button>
            ) : (
              // 6. 일반 → 블랙리스트 추가 버튼
              <Button
                size="sm"
                variant="outline"
                className="w-full justify-start text-red-600 hover:text-red-700 hover:bg-red-50 border-red-300"
                onClick={handleAddToBlacklist}
                disabled={isPending}
              >
                <UserX className="w-4 h-4 mr-2" />
                {addMutation.isPending ? "처리 중..." : "블랙리스트 추가"}
              </Button>
            )}
          </>
        )}
      </div>
    </div>
  );

  return (
    <>
      <Popover trigger="click" placement={placement} content={popoverContent}>
        {trigger}
      </Popover>
      <ConfirmModalComponent />
    </>
  );
};

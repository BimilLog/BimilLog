"use client";

import React from "react";
import Link from "next/link";
import { Check, X } from "lucide-react";
import { Button } from "@/components";
import { ReceivedFriendRequest } from "@/types/domains/friend";
import { useAcceptFriendRequestAction, useRejectFriendRequestAction } from "@/hooks/actions/useFriendActions";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";

interface ReceivedRequestItemProps {
  request: ReceivedFriendRequest;
}

/**
 * 받은 친구 요청 아이템 컴포넌트
 *
 * 라운드 9: paper/ink 토큰, 닉네임을 페이퍼 링크로 (라운드 5 visit 일관),
 * 토큰화된 confirm 아이콘, 모바일 44px 타겟.
 */
export const ReceivedRequestItem: React.FC<ReceivedRequestItemProps> = React.memo(({ request }) => {
  const { acceptRequest, isPending: isAccepting } = useAcceptFriendRequestAction();
  const { rejectRequest, isPending: isRejecting } = useRejectFriendRequestAction();
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  const handleAccept = () => {
    acceptRequest(request.friendRequestId);
  };

  const handleReject = async () => {
    const confirmed = await confirm({
      title: "친구 요청 거절",
      message: `${request.senderMemberName}님의 친구 요청을 거절할까요?`,
      confirmText: "거절",
      cancelText: "돌아가기",
      confirmButtonVariant: "destructive",
      icon: <X className="h-8 w-8 stroke-stamp-red" />,
    });

    if (confirmed) {
      rejectRequest(request.friendRequestId);
    }
  };

  const isPending = isAccepting || isRejecting;

  return (
    <>
      <li className="flex items-center justify-between p-4 hover:bg-paper-100 transition-colors border-b border-postal-navy/10 last:border-b-0">
        {/* 왼쪽: 발신자명 (페이퍼 링크) */}
        <div className="flex-1 min-w-0">
          <Link
            href={`/rolling-paper/${encodeURIComponent(request.senderMemberName)}`}
            className="font-medium text-ink truncate break-keep hover:text-postal-navy hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-postal-navy rounded"
          >
            {request.senderMemberName}
          </Link>
        </div>

        {/* 오른쪽: 수락/거절 버튼 */}
        <div className="flex items-center gap-2 shrink-0">
          <Button
            color="success"
            size="sm"
            onClick={handleAccept}
            disabled={isPending}
            aria-label={`${request.senderMemberName}님 친구 요청 수락`}
            className="min-h-[44px]"
          >
            <Check className="w-4 h-4 sm:mr-1" aria-hidden="true" />
            <span className="hidden sm:inline">수락</span>
          </Button>
          <Button
            color="failure"
            size="sm"
            onClick={handleReject}
            disabled={isPending}
            aria-label={`${request.senderMemberName}님 친구 요청 거절`}
            className="min-h-[44px]"
          >
            <X className="w-4 h-4 sm:mr-1" aria-hidden="true" />
            <span className="hidden sm:inline">거절</span>
          </Button>
        </div>
      </li>
      <ConfirmModalComponent />
    </>
  );
});

ReceivedRequestItem.displayName = "ReceivedRequestItem";

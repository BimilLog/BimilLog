"use client";

import React from "react";
import { Check, X } from "lucide-react";
import { Button } from "@/components";
import { ReceivedFriendRequest } from "@/types/domains/friend";
import { useAcceptFriendRequest, useRejectFriendRequest } from "@/hooks/api/useFriendMutations";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";

interface ReceivedRequestItemProps {
  request: ReceivedFriendRequest;
}

/**
 * 받은 친구 요청 아이템 컴포넌트
 */
export const ReceivedRequestItem: React.FC<ReceivedRequestItemProps> = ({ request }) => {
  const { mutate: acceptRequest, isPending: isAccepting } = useAcceptFriendRequest();
  const { mutate: rejectRequest, isPending: isRejecting } = useRejectFriendRequest();
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  const handleAccept = () => {
    acceptRequest(request.friendRequestId);
  };

  const handleReject = async () => {
    const confirmed = await confirm({
      title: "친구 요청 거절",
      message: `${request.senderMemberName}님의 친구 요청을 거절하시겠습니까?`,
      confirmText: "거절",
      cancelText: "돌아가기",
      confirmButtonVariant: "destructive",
      icon: <X className="h-8 w-8 stroke-red-600 fill-red-100" />
    });

    if (confirmed) {
      rejectRequest(request.friendRequestId);
    }
  };

  const isPending = isAccepting || isRejecting;

  return (
    <>
      <li className="flex items-center justify-between p-4 hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-b-0">
        {/* 왼쪽: 사용자명 */}
        <div className="flex-1 min-w-0">
          <h3 className="font-medium text-gray-900 truncate">
            {request.senderMemberName}
          </h3>
        </div>

        {/* 오른쪽: 수락/거절 버튼 */}
        <div className="flex items-center gap-2 shrink-0">
          <Button
            color="success"
            size="sm"
            onClick={handleAccept}
            disabled={isPending}
          >
            <Check className="w-4 h-4 mr-1" />
            수락
          </Button>
          <Button
            color="failure"
            size="sm"
            onClick={handleReject}
            disabled={isPending}
          >
            <X className="w-4 h-4 mr-1" />
            거절
          </Button>
        </div>
      </li>
      <ConfirmModalComponent />
    </>
  );
};

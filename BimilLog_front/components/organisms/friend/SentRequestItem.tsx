"use client";

import React from "react";
import { X } from "lucide-react";
import { Avatar } from "flowbite-react";
import { Button, Badge } from "@/components";
import { SentFriendRequest } from "@/types/domains/friend";
import { useCancelFriendRequestAction } from "@/hooks/actions/useFriendActions";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";
import { getInitials } from "@/lib/utils/format";
import { formatDate } from "@/lib/utils";

interface SentRequestItemProps {
  request: SentFriendRequest;
}

/**
 * 보낸 친구 요청 아이템 컴포넌트
 */
export const SentRequestItem: React.FC<SentRequestItemProps> = ({ request }) => {
  const { cancelRequest, isPending } = useCancelFriendRequestAction();
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  const handleCancel = async () => {
    const confirmed = await confirm({
      title: "친구 요청 취소",
      message: `${request.receiverMemberName}님에게 보낸 친구 요청을 취소하시겠습니까?`,
      confirmText: "취소",
      cancelText: "돌아가기",
      confirmButtonVariant: "destructive",
      icon: <X className="h-8 w-8 stroke-red-600 fill-red-100" />
    });

    if (confirmed) {
      cancelRequest(request.friendRequestId);
    }
  };

  return (
    <>
      <li className="flex items-center justify-between p-4 hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-b-0">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <h3 className="font-medium text-gray-900 truncate">
                {request.receiverMemberName}
              </h3>
              <Badge color="warning" className="text-xs shrink-0">
                대기중
              </Badge>
            </div>
          </div>

        {/* 오른쪽: 취소 버튼 */}
        <Button
          color="light"
          size="sm"
          onClick={handleCancel}
          disabled={isPending}
          className="ml-4 shrink-0"
        >
          <X className="w-4 h-4 mr-1" />
          취소
        </Button>
      </li>
      <ConfirmModalComponent />
    </>
  );
};

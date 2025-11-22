"use client";

import React from "react";
import { Check, X } from "lucide-react";
import { Avatar } from "flowbite-react";
import { Button } from "@/components";
import { FriendRequest } from "@/types/domains/friend";
import { useAcceptFriendRequest, useRejectFriendRequest } from "@/hooks/api/useFriendMutations";
import { getInitials } from "@/lib/utils/format";
import { formatDate } from "@/lib/utils";

interface ReceivedRequestItemProps {
  request: FriendRequest;
}

/**
 * 받은 친구 요청 아이템 컴포넌트
 */
export const ReceivedRequestItem: React.FC<ReceivedRequestItemProps> = ({ request }) => {
  const { mutate: acceptRequest, isPending: isAccepting } = useAcceptFriendRequest();
  const { mutate: rejectRequest, isPending: isRejecting } = useRejectFriendRequest();

  const handleAccept = () => {
    acceptRequest(request.id);
  };

  const handleReject = () => {
    if (confirm(`${request.senderName}님의 친구 요청을 거절하시겠습니까?`)) {
      rejectRequest(request.id);
    }
  };

  const isPending = isAccepting || isRejecting;

  return (
    <li className="flex items-center justify-between p-4 hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-b-0">
      {/* 왼쪽: 프로필 정보 */}
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <Avatar
          img={request.senderThumbnail}
          alt={request.senderName}
          placeholderInitials={getInitials(request.senderName)}
          rounded
          size="md"
          className="w-12 h-12 shrink-0"
        />

        <div className="flex-1 min-w-0">
          <h3 className="font-medium text-gray-900 truncate">
            {request.senderName}
          </h3>
          {request.createdAt && (
            <p className="text-xs text-gray-500 mt-1">
              {formatDate(request.createdAt)}
            </p>
          )}
        </div>
      </div>

      {/* 오른쪽: 수락/거절 버튼 */}
      <div className="flex items-center gap-2 ml-4 shrink-0">
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
  );
};

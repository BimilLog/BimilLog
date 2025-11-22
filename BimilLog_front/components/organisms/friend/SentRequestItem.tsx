"use client";

import React from "react";
import { X } from "lucide-react";
import { Avatar } from "flowbite-react";
import { Button, Badge } from "@/components";
import { FriendRequest } from "@/types/domains/friend";
import { useCancelFriendRequest } from "@/hooks/api/useFriendMutations";
import { getInitials } from "@/lib/utils/format";
import { formatDate } from "@/lib/utils";

interface SentRequestItemProps {
  request: FriendRequest;
}

/**
 * 보낸 친구 요청 아이템 컴포넌트
 */
export const SentRequestItem: React.FC<SentRequestItemProps> = ({ request }) => {
  const { mutate: cancelRequest, isPending } = useCancelFriendRequest();

  const handleCancel = () => {
    if (confirm(`${request.receiverName}님에게 보낸 친구 요청을 취소하시겠습니까?`)) {
      cancelRequest(request.id);
    }
  };

  return (
    <li className="flex items-center justify-between p-4 hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-b-0">
      {/* 왼쪽: 프로필 정보 */}
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <Avatar
          img={request.receiverThumbnail}
          alt={request.receiverName}
          placeholderInitials={getInitials(request.receiverName)}
          rounded
          size="md"
          className="w-12 h-12 shrink-0"
        />

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <h3 className="font-medium text-gray-900 truncate">
              {request.receiverName}
            </h3>
            <Badge color="warning" className="text-xs shrink-0">
              대기중
            </Badge>
          </div>
          {request.createdAt && (
            <p className="text-xs text-gray-500 mt-1">
              {formatDate(request.createdAt)}
            </p>
          )}
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
  );
};

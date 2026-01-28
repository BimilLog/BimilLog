"use client";

import React from "react";
import { MessageCircle, Trash2 } from "lucide-react";
import { Avatar } from "flowbite-react";
import { Button } from "@/components";
import { Friend } from "@/types/domains/friend";
import { useRemoveFriendAction } from "@/hooks/actions/useFriendActions";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";
import { getInitials } from "@/lib/utils/format";
import { useRouter } from "next/navigation";

interface FriendListItemProps {
  friend: Friend;
}

/**
 * 친구 목록 아이템 컴포넌트
 */
export const FriendListItem: React.FC<FriendListItemProps> = ({ friend }) => {
  const router = useRouter();
  const { removeFriend, isPending } = useRemoveFriendAction();
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  const handleRemove = async () => {
    const confirmed = await confirm({
      title: "친구 삭제",
      message: `${friend.memberName}님을 친구 목록에서 삭제하시겠습니까?`,
      confirmText: "삭제",
      cancelText: "취소",
      confirmButtonVariant: "destructive",
      icon: <Trash2 className="h-8 w-8 stroke-red-600 fill-red-100" />
    });

    if (confirmed) {
      removeFriend(friend.friendshipId);
    }
  };

  const handleVisitRollingPaper = () => {
    router.push(`/rolling-paper/${encodeURIComponent(friend.memberName)}`);
  };

  return (
    <>
      <li className="flex items-center justify-between p-4 hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-b-0">
      {/* 왼쪽: 프로필 정보 */}
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <Avatar
          img={friend.thumbnailImage}
          alt={friend.memberName}
          placeholderInitials={getInitials(friend.memberName)}
          rounded
          size="md"
          className="w-12 h-12 shrink-0"
        />

        <div className="flex-1 min-w-0">
          <h3 className="font-medium text-gray-900 truncate">
            {friend.memberName}
          </h3>
        </div>
      </div>

      {/* 오른쪽: 액션 버튼 */}
      <div className="flex items-center gap-2 ml-4 shrink-0">
        <Button
          color="purple"
          size="sm"
          onClick={handleVisitRollingPaper}
        >
          <MessageCircle className="w-4 h-4 mr-1" />
          롤링페이퍼
        </Button>
        <Button
          color="light"
          size="sm"
          onClick={handleRemove}
          disabled={isPending}
          className="text-red-600 hover:text-red-700 hover:bg-red-50"
        >
          <Trash2 className="w-4 h-4" />
        </Button>
      </div>
    </li>
      <ConfirmModalComponent />
    </>
  );
};

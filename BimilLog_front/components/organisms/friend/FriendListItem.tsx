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
 *
 * 라운드 9: paper/ink 토큰, useConfirmModal 메타포 강화, 모바일 44px 타겟.
 */
export const FriendListItem: React.FC<FriendListItemProps> = React.memo(({ friend }) => {
  const router = useRouter();
  const { removeFriend, isPending } = useRemoveFriendAction();
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  const handleRemove = async () => {
    const confirmed = await confirm({
      title: "친구 삭제",
      message: `${friend.memberName}님을 친구 목록에서 삭제할까요?`,
      confirmText: "삭제",
      cancelText: "돌아가기",
      confirmButtonVariant: "destructive",
      icon: <Trash2 className="h-8 w-8 stroke-stamp-red" />,
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
      <li className="flex items-center justify-between p-4 hover:bg-paper-100 transition-colors border-b border-postal-navy/10 last:border-b-0">
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
            <h3 className="font-medium text-ink truncate break-keep">
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
            className="min-h-[44px]"
          >
            <MessageCircle className="w-4 h-4 sm:mr-1" aria-hidden="true" />
            <span className="hidden sm:inline">롤링페이퍼</span>
            <span className="sr-only sm:hidden">{friend.memberName}님의 롤링페이퍼</span>
          </Button>
          <Button
            color="light"
            size="sm"
            onClick={handleRemove}
            disabled={isPending}
            aria-label={`${friend.memberName}님 친구에서 삭제`}
            className="text-stamp-red hover:text-stamp-red hover:bg-stamp-red/10 min-h-[44px] min-w-[44px]"
          >
            <Trash2 className="w-4 h-4" aria-hidden="true" />
          </Button>
        </div>
      </li>
      <ConfirmModalComponent />
    </>
  );
});

FriendListItem.displayName = "FriendListItem";

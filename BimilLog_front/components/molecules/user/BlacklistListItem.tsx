"use client";

import React from "react";
import { Avatar } from "flowbite-react";
import { UserCheck, Trash2 } from "lucide-react";
import { Button } from "@/components";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";
import { useRemoveFromBlacklistAction } from "@/hooks/actions/useBlacklistActions";
import { getInitials } from "@/lib/utils/format";
import { formatRelativeDate } from "@/lib/utils/date";
import type { BlacklistDTO } from "@/types/domains/blacklist";

interface BlacklistListItemProps {
  item: BlacklistDTO;
}

/**
 * 블랙리스트 목록 아이템 (molecule).
 *
 * 라운드 15 F-15-BUG-2/4/10/12/14:
 * - useConfirmModal 차용 (window.confirm 제거).
 * - confirm 아이콘은 paper 메타포 (UserCheck stroke-postal-navy).
 * - formatRelativeDate + <time dateTime=> 시맨틱.
 * - 삭제 버튼 aria-label, 아이콘 aria-hidden.
 * - 라운드 9 FriendListItem 톤 차용 (Avatar 이니셜, min-h-[44px]).
 *
 * 옵티미스틱 패턴이 useRemoveFromBlacklistAction 내부에서 처리되므로
 * 카드별 isPending 분리는 불필요 (캐시에서 즉시 사라짐).
 */
export const BlacklistListItem: React.FC<BlacklistListItemProps> = React.memo(({ item }) => {
  const { removeFromBlacklist, isPending } = useRemoveFromBlacklistAction();
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  const handleRemove = async () => {
    const confirmed = await confirm({
      title: "차단 해제",
      message: `${item.memberName}님 차단을 풀어드릴까요?\n다시 편지를 받을 수 있어요.`,
      confirmText: "차단 해제",
      cancelText: "돌아가기",
      icon: <UserCheck className="h-8 w-8 stroke-postal-navy" />,
    });

    if (confirmed && item.id != null) {
      removeFromBlacklist(item.id);
    }
  };

  return (
    <>
      <li className="flex items-center justify-between p-4 hover:bg-paper-100 transition-colors border-b border-postal-navy/10 last:border-b-0">
        {/* 왼쪽: 프로필 + 차단 시간 */}
        <div className="flex items-center gap-3 flex-1 min-w-0">
          <Avatar
            placeholderInitials={getInitials(item.memberName)}
            rounded
            size="md"
            className="w-12 h-12 shrink-0"
            alt={item.memberName}
          />
          <div className="flex-1 min-w-0">
            <h3 className="font-medium text-ink dark:text-foreground truncate break-keep">
              {item.memberName}
            </h3>
            {item.createdAt && (
              <time
                dateTime={item.createdAt}
                className="text-xs text-ink-soft dark:text-muted-foreground mt-1 block break-keep"
              >
                {formatRelativeDate(item.createdAt)} 차단
              </time>
            )}
          </div>
        </div>

        {/* 오른쪽: 차단 해제 버튼 (라운드 9 톤) */}
        <div className="flex items-center gap-2 ml-4 shrink-0">
          <Button
            color="light"
            size="sm"
            onClick={handleRemove}
            disabled={isPending}
            aria-label={`${item.memberName}님 차단 해제`}
            className="text-stamp-red hover:text-stamp-red hover:bg-stamp-red/10 min-h-[44px] min-w-[44px]"
          >
            <Trash2 className="w-4 h-4 sm:mr-1" aria-hidden="true" />
            <span className="hidden sm:inline">차단 해제</span>
          </Button>
        </div>
      </li>
      <ConfirmModalComponent />
    </>
  );
});

BlacklistListItem.displayName = "BlacklistListItem";

"use client";

import React from "react";
import { UserPlus, UsersRound } from "lucide-react";
import { Avatar } from "flowbite-react";
import { Badge, Button } from "@/components";
import { RecommendedFriend } from "@/types/domains/friend";
import { useSendFriendRequestAction } from "@/hooks/actions/useFriendActions";
import { getInitials } from "@/lib/utils/format";

interface RecommendedFriendItemProps {
  friend: RecommendedFriend;
}

/**
 * 추천 친구 아이템 컴포넌트 (리스트형)
 *
 * 라운드 9:
 * - paper/ink 토큰 일관성 (친구 카드와 동일)
 * - thumbnailImage 활용해 아바타 노출 (응답 신설 X, 시각 일관성 강화)
 * - 옵티미스틱 — sendRequest 시 즉시 카드 사라짐 (B-009-F)
 */
export const RecommendedFriendItem: React.FC<RecommendedFriendItemProps> = React.memo(({ friend }) => {
  const { sendRequest, isPending } = useSendFriendRequestAction();

  const handleAddFriend = () => {
    sendRequest(friend.friendMemberId);
  };

  const depthLabel =
    friend.depth === 2 ? '친구의 친구' : friend.depth === 3 ? '친구의 친구의 친구' : null;

  return (
    <li className="flex items-center justify-between p-4 hover:bg-paper-100 transition-colors border-b border-postal-navy/10 last:border-b-0">
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
          {/* 이름 + 촌수 배지 */}
          <div className="flex items-center gap-2 mb-1 flex-wrap">
            <h3 className="font-medium text-ink truncate break-keep">
              {friend.memberName}
            </h3>
            {depthLabel && (
              <Badge
                color={friend.depth === 2 ? 'info' : 'gray'}
                className="text-xs shrink-0 break-keep"
                aria-label={depthLabel}
              >
                {friend.depth}촌
              </Badge>
            )}
          </div>

          {/* 소개 문구 */}
          {friend.introduce && (
            <p className="text-sm text-ink-soft flex items-center gap-1 truncate break-keep">
              <UsersRound className="w-3.5 h-3.5 shrink-0" aria-hidden="true" />
              <span className="truncate">{friend.introduce}</span>
            </p>
          )}
        </div>
      </div>

      {/* 오른쪽: 친구 요청 버튼 */}
      <Button
        color="purple"
        size="sm"
        onClick={handleAddFriend}
        disabled={isPending}
        aria-label={`${friend.memberName}님에게 친구 요청 보내기`}
        className="ml-4 shrink-0 min-h-[44px]"
      >
        {isPending ? (
          <span>요청 중...</span>
        ) : (
          <>
            <UserPlus className="w-4 h-4 sm:mr-1" aria-hidden="true" />
            <span className="hidden sm:inline">친구 요청</span>
          </>
        )}
      </Button>
    </li>
  );
});

RecommendedFriendItem.displayName = "RecommendedFriendItem";

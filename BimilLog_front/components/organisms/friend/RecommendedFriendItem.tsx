"use client";

import React from "react";
import { UserPlus, UsersRound } from "lucide-react";
import { Avatar } from "flowbite-react";
import { Badge, Button } from "@/components";
import { RecommendedFriend } from "@/types/domains/friend";
import { useSendFriendRequest } from "@/hooks/api/useFriendMutations";
import { getInitials } from "@/lib/utils/format";

interface RecommendedFriendItemProps {
  friend: RecommendedFriend;
}

/**
 * 추천 친구 아이템 컴포넌트 (리스트형)
 * 2촌/3촌 표시 및 공통 친구 소개 문구 포함
 */
export const RecommendedFriendItem: React.FC<RecommendedFriendItemProps> = ({ friend }) => {
  const { mutate: sendRequest, isPending } = useSendFriendRequest();

  const handleAddFriend = () => {
    sendRequest({ receiverMemberId: friend.friendMemberId });
  };

  return (
    <li className="flex items-center justify-between p-4 hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-b-0">
        <div className="flex-1 min-w-0">
          {/* 이름 + 촌수 배지 (depth가 null이 아닐 때만 표시) */}
          <div className="flex items-center gap-2 mb-1">
            <h3 className="font-medium text-gray-900 truncate">
              {friend.memberName}
            </h3>
            {friend.depth !== null && (
              <Badge
                color={friend.depth === 2 ? "info" : "gray"}
                className="text-xs shrink-0"
              >
                {friend.depth}촌
              </Badge>
            )}
          </div>

          {/* 소개 문구 (2촌만 - "홍길동의 친구" 또는 "홍길동 외 다수의 친구") */}
          {friend.introduce && (
            <p className="text-sm text-gray-500 flex items-center gap-1 truncate">
              <UsersRound className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">{friend.introduce}</span>
            </p>
          )}
        </div>

      {/* 오른쪽: 친구 요청 버튼 */}
      <Button
        color="purple"
        size="sm"
        onClick={handleAddFriend}
        disabled={isPending}
        className="ml-4 shrink-0"
      >
        {isPending ? (
          <>요청 중...</>
        ) : (
          <>
            <UserPlus className="w-4 h-4 mr-1" />
            친구 요청
          </>
        )}
      </Button>
    </li>
  );
};

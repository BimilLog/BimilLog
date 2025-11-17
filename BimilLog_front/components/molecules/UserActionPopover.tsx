"use client";

import React from "react";
import Link from "next/link";
import { Popover } from "flowbite-react";
import { User, ExternalLink, UserX, UserCheck } from "lucide-react";
import { Button } from "@/components";
import { useAuth } from "@/hooks/common/useAuth";
import { useToast } from "@/hooks/common/useToast";
import { useBlacklistCheck } from "@/hooks/features/blacklist/useBlacklistCheck";
import { useAddToBlacklist, useRemoveFromBlacklist } from "@/hooks/api/useBlacklistMutations";

interface UserActionPopoverProps {
  memberName: string;
  trigger: React.ReactNode;
  placement?: "top" | "bottom" | "left" | "right";
}

export const UserActionPopover: React.FC<UserActionPopoverProps> = ({
  memberName,
  trigger,
  placement = "bottom",
}) => {
  const { user, isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const { isBlacklisted, blacklistId } = useBlacklistCheck(memberName);
  const addMutation = useAddToBlacklist();
  const removeMutation = useRemoveFromBlacklist();

  // 본인 여부 확인
  const isOwnProfile = user?.memberName === memberName;

  // 블랙리스트 추가 핸들러
  const handleAddToBlacklist = () => {
    if (!confirm(`'${memberName}' 님을 블랙리스트에 추가하시겠습니까?\n\n이 사용자는 회원님의 롤링페이퍼에 메시지를 남길 수 없게 됩니다.`)) {
      return;
    }

    addMutation.mutate(memberName);
  };

  // 블랙리스트 제거 핸들러
  const handleRemoveFromBlacklist = () => {
    if (!blacklistId) return;

    if (!confirm(`'${memberName}' 님을 블랙리스트에서 제거하시겠습니까?`)) {
      return;
    }

    removeMutation.mutate({ id: blacklistId, page: 0, size: 100 });
  };

  const popoverContent = (
    <div className="p-3 w-56">
      <div className="flex flex-col space-y-2">
        {/* 사용자 정보 */}
        <div className="flex items-center space-x-2 mb-1">
          <User className="w-4 h-4" />
          <span className="font-medium">{memberName}</span>
        </div>

        {/* 롤링페이퍼 보기 */}
        <Link href={`/rolling-paper/${encodeURIComponent(memberName)}`}>
          <Button size="sm" className="w-full justify-start">
            <ExternalLink className="w-4 h-4 mr-2" />
            롤링페이퍼 보기
          </Button>
        </Link>

        {/* 블랙리스트 버튼 (로그인 + 본인 아님) */}
        {isAuthenticated && !isOwnProfile && (
          <>
            {isBlacklisted ? (
              <Button
                size="sm"
                variant="outline"
                className="w-full justify-start text-green-600 hover:text-green-700 hover:bg-green-50 border-green-300"
                onClick={handleRemoveFromBlacklist}
                disabled={removeMutation.isPending}
              >
                <UserCheck className="w-4 h-4 mr-2" />
                {removeMutation.isPending ? "처리 중..." : "블랙리스트에서 제거"}
              </Button>
            ) : (
              <Button
                size="sm"
                variant="outline"
                className="w-full justify-start text-red-600 hover:text-red-700 hover:bg-red-50 border-red-300"
                onClick={handleAddToBlacklist}
                disabled={addMutation.isPending}
              >
                <UserX className="w-4 h-4 mr-2" />
                {addMutation.isPending ? "처리 중..." : "블랙리스트 추가"}
              </Button>
            )}
          </>
        )}
      </div>
    </div>
  );

  return (
    <Popover trigger="click" placement={placement} content={popoverContent}>
      {trigger}
    </Popover>
  );
};

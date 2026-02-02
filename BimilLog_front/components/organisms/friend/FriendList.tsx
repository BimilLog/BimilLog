"use client";

import React, { useState } from "react";
import { Users } from "lucide-react";
import { Button, Spinner } from "@/components";
import { useMyFriends } from "@/hooks/api/useFriendQueries";
import { FriendListItem } from "./FriendListItem";
import type { PageResponse } from "@/types/common";
import type { Friend } from "@/types/domains/friend";

interface FriendListProps {
  initialData?: PageResponse<Friend> | null;
}

/**
 * 내 친구 목록 컴포넌트
 */
export const FriendList: React.FC<FriendListProps> = ({ initialData }) => {
  const [page, setPage] = useState(0);
  const size = 20;

  const { data, isLoading, error } = useMyFriends(page, size, true, page === 0 ? initialData : undefined);

  const friendData = data?.data;
  const friends = friendData?.content || [];
  const totalPages = friendData?.totalPages || 0;
  const isEmpty = friendData?.empty ?? true;

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner message="친구 목록을 불러오는 중..." />
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className="p-8 text-center">
        <Users className="w-16 h-16 mx-auto mb-4 text-gray-400" />
        <p className="text-gray-600">친구 목록을 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div>
      {/* 헤더 */}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
          <Users className="w-5 h-5 text-blue-600" />
          내 친구
        </h2>
        {!isEmpty && (
          <span className="text-sm text-gray-500">
            {friends.length}명
          </span>
        )}
      </div>

      {/* 리스트 */}
      {isEmpty ? (
        <div className="text-center py-16 bg-gray-50 rounded-lg">
          <Users className="w-16 h-16 mx-auto mb-4 text-gray-400" />
          <p className="text-gray-600 font-medium">친구가 없습니다</p>
          <p className="text-sm text-gray-500 mt-2">
            추천 친구 탭에서 새로운 친구를 추가해보세요
          </p>
        </div>
      ) : (
        <>
          <ul className="bg-white border border-gray-200 rounded-lg overflow-hidden shadow-sm">
            {friends.map((friend) => (
              <FriendListItem key={friend.friendMemberId} friend={friend} />
            ))}
          </ul>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-6">
              <Button
                color="light"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                이전
              </Button>
              <span className="text-sm text-gray-600 px-3">
                {page + 1} / {totalPages}
              </span>
              <Button
                color="light"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
              >
                다음
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

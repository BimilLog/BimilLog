"use client";

import React, { useState } from "react";
import { Users } from "lucide-react";
import { Button, Spinner } from "@/components";
import { useRecommendedFriends } from "@/hooks/api/useFriendQueries";
import { RecommendedFriendItem } from "./RecommendedFriendItem";
import type { PageResponse } from "@/types/common";
import type { RecommendedFriend } from "@/types/domains/friend";

interface RecommendedFriendListProps {
  initialData?: PageResponse<RecommendedFriend> | null;
}

/**
 * 추천 친구 목록 컴포넌트
 * 2촌, 3촌 친구를 추천 점수별로 표시
 */
export const RecommendedFriendList: React.FC<RecommendedFriendListProps> = ({ initialData }) => {
  const [page, setPage] = useState(0);
  const size = 10;

  const { data, isLoading, error } = useRecommendedFriends(page, size, page === 0 ? initialData : undefined);

  const recommendedData = data?.data;
  const friends = recommendedData?.content || [];
  const totalPages = recommendedData?.totalPages || 0;
  const isEmpty = recommendedData?.empty ?? true;

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner message="추천 친구를 불러오는 중..." />
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className="p-8 text-center">
        <Users className="w-16 h-16 mx-auto mb-4 text-gray-400" />
        <p className="text-gray-600">추천 친구를 불러올 수 없습니다.</p>
        <p className="text-sm text-gray-500 mt-2">
          {error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다'}
        </p>
      </div>
    );
  }

  return (
    <div>
      {/* 헤더 */}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
          <Users className="w-5 h-5 text-purple-600" />
          알 수도 있는 친구
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
          <p className="text-gray-600 font-medium">추천 친구가 없습니다</p>
          <p className="text-sm text-gray-500 mt-2">
            친구를 추가하면 더 많은 추천을 받을 수 있어요
          </p>
        </div>
      ) : (
        <>
          <ul className="bg-white border border-gray-200 rounded-lg overflow-hidden shadow-sm">
            {friends.map((friend) => (
              <RecommendedFriendItem key={friend.friendMemberId} friend={friend} />
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

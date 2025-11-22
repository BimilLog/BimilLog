"use client";

import React, { useState } from "react";
import { Send } from "lucide-react";
import { Button, Spinner, EmptyState } from "@/components";
import { useSentFriendRequests } from "@/hooks/api/useFriendQueries";
import { SentRequestItem } from "./SentRequestItem";

/**
 * 보낸 친구 요청 목록 컴포넌트
 */
export const SentRequestList: React.FC = () => {
  const [page, setPage] = useState(0);
  const size = 20;

  const { data, isLoading, error } = useSentFriendRequests(page, size);

  const requestData = data?.data;
  const requests = requestData?.content || [];
  const totalPages = requestData?.totalPages || 0;
  const isEmpty = requestData?.empty ?? true;

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner message="보낸 요청을 불러오는 중..." />
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className="p-8 text-center">
        <Send className="w-16 h-16 mx-auto mb-4 text-gray-400" />
        <p className="text-gray-600">보낸 요청을 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div>
      {/* 헤더 */}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900 flex items-center gap-2">
          <Send className="w-5 h-5 text-blue-600" />
          보낸 친구 요청
        </h2>
        {!isEmpty && (
          <span className="text-sm text-gray-500">
            {requests.length}개
          </span>
        )}
      </div>

      {/* 리스트 */}
      {isEmpty ? (
        <div className="text-center py-16 bg-gray-50 rounded-lg">
          <Send className="w-16 h-16 mx-auto mb-4 text-gray-400" />
          <p className="text-gray-600 font-medium">보낸 친구 요청이 없습니다</p>
        </div>
      ) : (
        <>
          <ul className="bg-white border border-gray-200 rounded-lg overflow-hidden shadow-sm">
            {requests.map((request) => (
              <SentRequestItem key={request.id} request={request} />
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

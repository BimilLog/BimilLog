"use client";

import { useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { Button } from "@/components";
import { useAllMembers } from "@/hooks/api/useUserQueries";
import { BoardPagination } from "@/components/organisms/board/board-pagination";

/**
 * 모든 사용자 목록 컴포넌트
 * 페이징과 페이지 크기 선택 기능 제공
 */
export const AllUsersList = () => {
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data, isLoading, error } = useAllMembers(currentPage, pageSize);

  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize);
    setCurrentPage(0); // 페이지 크기 변경 시 첫 페이지로 리셋
  };

  if (error) {
    return (
      <Card className="border border-gray-200 dark:border-slate-700 shadow-md">
        <CardContent className="p-6">
          <p className="text-red-500 dark:text-red-400 text-center">
            사용자 목록을 불러올 수 없습니다.
          </p>
        </CardContent>
      </Card>
    );
  }

  const users = data?.data?.content || [];
  const totalPages = data?.data?.totalPages || 0;
  const totalElements = data?.data?.totalElements || 0;

  return (
    <Card className="border border-gray-200 dark:border-slate-700 shadow-md">
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg font-bold text-gray-900 dark:text-gray-100">
            롤링페이퍼 보기
          </CardTitle>
          <div className="flex items-center gap-2">

            <select
              value={pageSize}
              onChange={(e) => handlePageSizeChange(Number(e.target.value))}
              className="text-sm border border-gray-300 dark:border-slate-600 rounded-md px-2 py-1 bg-white dark:bg-slate-800 text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value={10}>10개씩</option>
              <option value={20}>20개씩</option>
              <option value={30}>30개씩</option>
            </select>
          </div>
        </div>
      </CardHeader>

      <CardContent className="p-4">
        {isLoading ? (
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <div
                key={i}
                className="animate-pulse flex items-center justify-between p-3 bg-gray-100 dark:bg-slate-800 rounded-lg"
              >
                <div className="h-4 w-32 bg-gray-300 dark:bg-slate-700 rounded" />
                <div className="h-9 w-20 bg-gray-300 dark:bg-slate-700 rounded" />
              </div>
            ))}
          </div>
        ) : users.length === 0 ? (
          <p className="text-center text-gray-500 dark:text-gray-400 py-8">
            등록된 사용자가 없습니다.
          </p>
        ) : (
          <div className="space-y-2">
            {users.map((user) => (
              <div
                key={user.memberId}
                className="flex items-center justify-between p-3 hover:bg-gray-50 dark:hover:bg-slate-800/50 rounded-lg transition-colors border border-transparent hover:border-gray-200 dark:hover:border-slate-700"
              >
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900 dark:text-gray-100 truncate">
                    {user.memberName}
                  </p>
                </div>
                <Link href={`/rolling-paper/${user.memberName}`}>
                  <Button
                    size="sm"
                    variant="link"
                    className="flex-shrink-0 ml-3"
                  >
                    바로가기
                  </Button>
                </Link>
              </div>
            ))}
          </div>
        )}

        {/* 페이지네이션 */}
        {!isLoading && users.length > 0 && (
          <BoardPagination
            currentPage={currentPage}
            totalPages={totalPages}
            setCurrentPage={setCurrentPage}
          />
        )}
      </CardContent>
    </Card>
  );
};

"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components";
import { useAllMembers, useSearchMembers } from "@/hooks/api/useUserQueries";
import { BoardPagination } from "@/components/organisms/board/board-pagination";
interface AllUsersListProps {
  searchKeyword?: string;
}
/**
 * 모든 사용자 목록 컴포넌트
 * 검색어가 있으면 검색 API를 호출하고, 없으면 전체 목록을 가져옵니다.
 * SearchSection Card 내부에 렌더링되므로 Card wrapper 없이 구성됩니다.
 */
export const AllUsersList = ({ searchKeyword = "" }: AllUsersListProps) => {
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  // 검색 모드 여부
  const isSearchMode = searchKeyword.trim().length > 0;
  // 조건부 쿼리 선택
  const allMembersQuery = useAllMembers(currentPage, pageSize);
  const searchQuery = useSearchMembers(searchKeyword, currentPage, pageSize);
  const activeQuery = isSearchMode ? searchQuery : allMembersQuery;
  // 검색어 변경 시 페이지를 0으로 리셋
  useEffect(() => {
    setCurrentPage(0);
  }, [searchKeyword]);
  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize);
    setCurrentPage(0); // 페이지 크기 변경 시 첫 페이지로 리셋
  };
  // 에러 처리
  if (activeQuery.error) {
    return (
      <div className="p-6">
        <p className="text-red-500 dark:text-red-400 text-center">
          {isSearchMode ? "검색 중 오류가 발생했습니다." : "사용자 목록을 불러올 수 없습니다."}
        </p>
      </div>
    );
  }
  // 데이터 추출
  let memberNames: string[] = [];
  let totalPages = 0;
  if (isSearchMode) {
    // 검색 API는 Page<String> 반환
    const searchData = searchQuery.data?.data;
    memberNames = searchData?.content || [];
    totalPages = searchData?.totalPages || 0;
  } else {
    // 전체 목록 API는 Page<SimpleMember> 반환
    const allData = allMembersQuery.data?.data;
    const users = allData?.content || [];
    memberNames = users.map((user) => user.memberName);
    totalPages = allData?.totalPages || 0;
  }
  return (
    <>
      {/* 페이지 크기 선택 드롭다운 (우측 정렬) */}
      <div className="flex justify-end mb-4">
        <select
          value={pageSize}
          onChange={(e) => handlePageSizeChange(Number(e.target.value))}
          className="text-sm border border-gray-300 dark:border-slate-600 rounded-md px-3 py-2 pr-10 bg-white dark:bg-slate-800 text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer appearance-none"
          style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3E%3Cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3E%3C/svg%3E")`,
            backgroundPosition: 'right 0.5rem center',
            backgroundRepeat: 'no-repeat',
            backgroundSize: '1.5em 1.5em',
          }}
        >
          <option value={10}>10개씩</option>
          <option value={20}>20개씩</option>
          <option value={30}>30개씩</option>
        </select>
      </div>
      {/* 테이블 */}
      {activeQuery.isLoading ? (
        <div className="space-y-2">
          {[...Array(5)].map((_, i) => (
            <div
              key={i}
              className="animate-pulse flex items-center justify-between p-3 bg-gray-100 dark:bg-slate-800 rounded"
            >
              <div className="h-4 w-32 bg-gray-300 dark:bg-slate-700 rounded" />
              <div className="h-9 w-20 bg-gray-300 dark:bg-slate-700 rounded" />
            </div>
          ))}
        </div>
      ) : memberNames.length === 0 ? (
        <p className="text-center text-gray-500 dark:text-gray-400 py-8">
          {isSearchMode ? "검색 결과가 없습니다." : "등록된 사용자가 없습니다."}
        </p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b-2 border-gray-300 dark:border-slate-600">
                <th className="text-left py-3 px-4 font-semibold text-gray-700 dark:text-gray-300 text-sm">
                  사용자 이름
                </th>
                <th className="text-center py-3 px-4 font-semibold text-gray-700 dark:text-gray-300 text-sm w-32">
                  롤링페이퍼
                </th>
              </tr>
            </thead>
            <tbody>
              {memberNames.map((memberName, index) => (
                <tr
                  key={`${memberName}-${index}`}
                  className={`
                    border-b border-gray-200 dark:border-slate-700
                    transition-colors
                    ${
                      index % 2 === 0
                        ? 'bg-white dark:bg-slate-900'
                        : 'bg-gray-50 dark:bg-slate-800/50'
                    }
                    hover:bg-blue-50 dark:hover:bg-slate-700/50
                  `}
                >
                  <td className="py-3 px-4 text-gray-900 dark:text-gray-100">
                    <span className="font-medium">{memberName}</span>
                  </td>
                  <td className="py-3 px-4 text-center">
                    <Link href={`/rolling-paper/${encodeURIComponent(memberName)}`}>
                      <Button
                        size="sm"
                        variant="link"
                        className="inline-flex items-center justify-center"
                      >
                        바로가기
                      </Button>
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {/* 페이지네이션 */}
      {!activeQuery.isLoading && memberNames.length > 0 && (
        <div className="mt-6">
          <BoardPagination
            currentPage={currentPage}
            totalPages={totalPages}
            setCurrentPage={setCurrentPage}
          />
        </div>
      )}
    </>
  );
};
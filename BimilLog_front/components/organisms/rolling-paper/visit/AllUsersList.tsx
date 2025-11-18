"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import { Table, TableBody, TableCell, TableHead, TableHeadCell, TableRow } from "flowbite-react";
import { User } from "lucide-react";
import { Button } from "@/components";
import { UserActionPopover } from "@/components/molecules/UserActionPopover";
import { useAllMembers, useSearchMembers } from "@/hooks/api/useUserQueries";
import { BoardPagination } from "@/components/organisms/board/board-pagination";
interface AllUsersListProps {
  searchKeyword?: string;
}
/**
 * Renders visit-page member listings, switching between search results and the full list.
 * Rendered inside the SearchSection card so no additional card wrapper is needed here.
 */
export const AllUsersList = ({ searchKeyword = "" }: AllUsersListProps) => {
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  // Determine whether we are currently showing filtered results
  const isSearchMode = searchKeyword.trim().length > 0;
  // Choose the query result set based on the current mode
  const allMembersQuery = useAllMembers(currentPage, pageSize);
  const searchQuery = useSearchMembers(searchKeyword, currentPage, pageSize);
  const activeQuery = isSearchMode ? searchQuery : allMembersQuery;
  // Reset pagination when the keyword changes
  useEffect(() => {
    setCurrentPage(0);
  }, [searchKeyword]);
  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize);
    setCurrentPage(0); // reset pagination when the page size changes
  };
  // Error handling
  if (activeQuery.error) {
    return (
      <div className="p-6">
        <p className="text-red-500 dark:text-red-400 text-center">
          {isSearchMode ? "Search failed." : "Unable to load member list."}
        </p>
      </div>
    );
  }
  // Prepare table rows
  type MemberRow = {
    key: string;
    memberName: string;
  };

  let memberRows: MemberRow[] = [];
  let totalPages = 0;
  if (isSearchMode) {
    const searchData = searchQuery.data?.data;
    const names = searchData?.content || [];
    memberRows = names.map((name, index) => ({
      key: name ? `search-${name}-${index}` : `search-index-${index}`,
      memberName: name ?? "익명",
    }));
    totalPages = searchData?.totalPages || 0;
  } else {
    const allData = allMembersQuery.data?.data;
    const users = allData?.content || [];
    memberRows = users.map((user, index) => {
      const idPart = user.memberId ? `member-${user.memberId}` : null;
      const namePart = user.memberName ? `name-${user.memberName}-${index}` : null;
      return {
        key: idPart ?? namePart ?? `member-index-${index}`,
        memberName: user.memberName ?? "익명",
      };
    });
    totalPages = allData?.totalPages || 0;
  }

  return (
    <>
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
      {/* Table */}
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
      ) : memberRows.length === 0 ? (
        <p className="text-center text-gray-500 dark:text-gray-400 py-8">
          {isSearchMode ? "No search results." : "No members available."}
        </p>
      ) : (
        <div className="overflow-x-auto">
          <Table hoverable className="w-full">
            <TableHead>
              <TableRow className="border-b-2 border-gray-300 dark:border-slate-600">
                <TableHeadCell className="py-3 font-semibold text-gray-700 dark:text-gray-300 text-sm">
                  닉네임
                </TableHeadCell>
                <TableHeadCell className="py-3 text-center font-semibold text-gray-700 dark:text-gray-300 text-sm w-40">
                  롤링페이퍼
                </TableHeadCell>
              </TableRow>
            </TableHead>
            <TableBody className="divide-y divide-gray-200 dark:divide-slate-700">
              {memberRows.map((member, index) => (
                <TableRow
                  key={member.key}
                  className={index % 2 === 0 ? "bg-white dark:bg-slate-900" : "bg-gray-50 dark:bg-slate-800/50"}
                >
                                    <TableCell className="py-3 text-gray-900 dark:text-gray-100">
                    {member.memberName && member.memberName !== "익명" ? (
                      <UserActionPopover
                        memberName={member.memberName}
                        trigger={
                          <button className="font-medium hover:text-purple-600 hover:underline transition-colors inline-flex items-center space-x-1">
                            <User className="w-3 h-3" />
                            <span>{member.memberName}</span>
                          </button>
                        }
                        placement="bottom"
                      />
                    ) : (
                      <span className="font-medium inline-flex items-center space-x-1 text-gray-500 dark:text-gray-400">
                        <User className="w-3 h-3" />
                        <span>{member.memberName || "익명"}</span>
                      </span>
                    )}
                  </TableCell>
                  <TableCell className="py-3 text-center">
                    <Link href={`/rolling-paper/${encodeURIComponent(member.memberName)}`}>
                      <Button
                        size="sm"
                        variant="link"
                        className="inline-flex items-center justify-center"
                      >
                        롤링페이퍼
                      </Button>
                    </Link>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      {/* Pagination */}
      {!activeQuery.isLoading && memberRows.length > 0 && (
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

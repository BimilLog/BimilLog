"use client";
import { useState, useEffect, memo, useId } from "react";
import Link from "next/link";
import { Table, TableBody, TableCell, TableHead, TableHeadCell, TableRow } from "flowbite-react";
import { User, MailOpen, ChevronDown } from "lucide-react";
import { Button } from "@/components";
import { UserActionPopover } from "@/components/molecules/UserActionPopover";
import { useAllMembers, useSearchMembers } from "@/hooks/api/useUserQueries";
import { BoardPagination } from "@/components/organisms/board/board-pagination";

interface AllUsersListProps {
  /** 현재 활성 검색어 (디바운스 적용된 effectiveKeyword) */
  searchKeyword?: string;
}

/**
 * Renders visit-page member listings, switching between search results and the full list.
 * Rendered inside the SearchSection card so no additional card wrapper is needed here.
 *
 * 라운드 5 변경:
 * - 빈 상태 / 에러 메시지 한국어화 (종이/편지 메타포)
 * - 페이지 사이즈 select 토큰 정리 + 라벨 명시
 * - 검색 결과 헤더 "총 N건 / '키워드'" 캡션 (totalElements 활용)
 * - PageResponse.empty / first / last / numberOfElements 활용
 */
export const AllUsersList = memo(({ searchKeyword = "" }: AllUsersListProps) => {
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const pageSizeId = useId();

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
        <p className="font-body text-stamp-red text-center">
          {isSearchMode
            ? "검색 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요."
            : "멤버 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요."}
        </p>
      </div>
    );
  }

  // Prepare table rows + 메타데이터 추출
  type MemberRow = {
    key: string;
    memberName: string;
    memberId?: number;
  };

  const pageData = isSearchMode ? searchQuery.data?.data : allMembersQuery.data?.data;
  const users = pageData?.content ?? [];

  const memberRows: MemberRow[] = users.map((user, index) => {
    const idPart = user.memberId ? `member-${user.memberId}` : null;
    const namePart = user.memberName ? `name-${user.memberName}-${index}` : null;
    return {
      key: idPart ?? namePart ?? `member-index-${index}`,
      memberName: user.memberName ?? "익명",
      memberId: user.memberId,
    };
  });

  // 백엔드 응답 메타 활용 — PageResponse.totalElements / empty / numberOfElements
  const totalPages = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;
  const isEmptyPage = pageData?.empty ?? memberRows.length === 0;
  const numberOfElements = pageData?.numberOfElements ?? memberRows.length;

  return (
    <>
      {/* 검색 결과 헤더 — 검색 모드일 때만 (B-003 / 백엔드 응답 활용) */}
      {isSearchMode && !activeQuery.isLoading && pageData && (
        <div
          data-testid="search-result-header"
          className="mb-3 flex items-center justify-between gap-2 px-1"
        >
          <p className="text-sm font-body text-ink-soft dark:text-muted-foreground">
            <span className="font-semibold text-postal-navy dark:text-ink-900">
              &lsquo;{searchKeyword}&rsquo;
            </span>
            {" "}검색 결과 총{" "}
            <span className="font-semibold text-foreground">
              {totalElements.toLocaleString()}
            </span>
            건
          </p>
        </div>
      )}

      <div className="flex items-center justify-end gap-2 mb-4">
        <label
          htmlFor={pageSizeId}
          className="text-xs font-body text-ink-soft dark:text-muted-foreground"
        >
          페이지당
        </label>
        <div className="relative">
          <select
            id={pageSizeId}
            value={pageSize}
            onChange={(e) => handlePageSizeChange(Number(e.target.value))}
            aria-label="페이지당 멤버 수"
            className="appearance-none cursor-pointer text-sm font-body rounded-md border border-ink-soft bg-card text-foreground hover:border-stamp-red/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red/30 focus-visible:border-stamp-red/60 px-3 py-2 pr-9 transition-colors"
          >
            <option value={10}>10명씩</option>
            <option value={20}>20명씩</option>
            <option value={30}>30명씩</option>
          </select>
          <ChevronDown
            className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-soft dark:text-muted-foreground"
            aria-hidden="true"
          />
        </div>
      </div>

      {/* Table */}
      {activeQuery.isLoading ? (
        <div className="space-y-2" data-testid="all-users-loading">
          {[...Array(5)].map((_, i) => (
            <div
              key={i}
              className="animate-pulse flex items-center justify-between p-3 bg-paper-aged/40 dark:bg-muted/20 rounded"
            >
              <div className="h-4 w-32 bg-ink-soft/30 dark:bg-muted/40 rounded" />
              <div className="h-9 w-20 bg-ink-soft/30 dark:bg-muted/40 rounded" />
            </div>
          ))}
        </div>
      ) : isEmptyPage ? (
        <div
          className="flex flex-col items-center justify-center text-center py-10 px-4 gap-3"
          data-testid="all-users-empty"
        >
          <MailOpen
            className="w-10 h-10 text-ink-soft/60 dark:text-muted-foreground/60"
            aria-hidden="true"
          />
          {isSearchMode ? (
            <>
              <p className="font-body text-ink dark:text-foreground text-sm">
                <span className="font-semibold">&lsquo;{searchKeyword}&rsquo;</span>
                에 해당하는 닉네임을 찾지 못했어요.
              </p>
              <p className="font-body text-xs text-ink-soft dark:text-muted-foreground">
                철자를 확인하거나 더 짧게 입력해 보세요.
              </p>
            </>
          ) : (
            <p className="font-body text-ink-soft dark:text-muted-foreground text-sm">
              아직 표시할 멤버가 없어요.
            </p>
          )}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <Table hoverable className="w-full">
            <TableHead className="!bg-paper-aged/40 dark:!bg-muted/30">
              <TableRow className="border-b-2 border-ink-soft/50 dark:border-border">
                <TableHeadCell className="py-3 font-display !text-ink dark:!text-foreground text-sm !bg-paper-aged/40 dark:!bg-muted/30">
                  닉네임
                </TableHeadCell>
                <TableHeadCell className="py-3 text-center font-display !text-ink dark:!text-foreground text-sm w-40 !bg-paper-aged/40 dark:!bg-muted/30">
                  롤링페이퍼
                </TableHeadCell>
              </TableRow>
            </TableHead>
            <TableBody className="divide-y divide-ink-soft/30 dark:divide-border">
              {memberRows.map((member, index) => (
                <TableRow
                  key={member.key}
                  className={
                    index % 2 === 0
                      ? "!bg-paper-card dark:!bg-card"
                      : "!bg-paper-aged/30 dark:!bg-muted/20"
                  }
                >
                  <TableCell className="py-3 text-ink dark:text-foreground">
                    {member.memberName && member.memberName !== "익명" ? (
                      <UserActionPopover
                        memberName={member.memberName}
                        memberId={member.memberId}
                        trigger={
                          <button
                            type="button"
                            aria-label={`${member.memberName}님의 옵션 메뉴 열기`}
                            className="font-medium text-ink dark:text-foreground hover:text-postal-navy dark:hover:text-postal-navy hover:underline transition-colors inline-flex items-center space-x-1"
                          >
                            <User className="w-3 h-3" aria-hidden="true" />
                            <span>{member.memberName}</span>
                          </button>
                        }
                        placement="bottom"
                      />
                    ) : (
                      <span className="font-medium inline-flex items-center space-x-1 text-ink-soft dark:text-muted-foreground">
                        <User className="w-3 h-3" aria-hidden="true" />
                        <span>{member.memberName || "익명"}</span>
                      </span>
                    )}
                  </TableCell>
                  <TableCell className="py-3 text-center">
                    <Link
                      href={`/rolling-paper/${encodeURIComponent(member.memberName)}`}
                      prefetch={false}
                      aria-label={`${member.memberName}님의 롤링페이퍼로 이동`}
                    >
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

      {/* Pagination — totalElements 전달로 단일페이지에도 캡션 표시 (B-001 보강) */}
      {!activeQuery.isLoading && !isEmptyPage && (
        <div className="mt-6">
          <BoardPagination
            currentPage={currentPage}
            totalPages={totalPages}
            setCurrentPage={setCurrentPage}
            totalElements={totalElements}
            itemLabel="명"
          />
          {numberOfElements > 0 && totalPages > 1 && (
            <p className="mt-2 text-center text-xs text-ink-soft dark:text-muted-foreground">
              이 페이지에 {numberOfElements}명 표시 중
            </p>
          )}
        </div>
      )}
    </>
  );
});

AllUsersList.displayName = "AllUsersList";

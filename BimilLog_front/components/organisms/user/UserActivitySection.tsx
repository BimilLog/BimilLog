"use client";

import React, { useState, useCallback, useEffect, useMemo, useRef, memo } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { UserActivityTable } from "./UserActivityTable";
import { BoardPagination } from "@/components/organisms/board/board-pagination";
import { useUserActivityTabs } from "@/hooks/features/user/useUserActivity";
import { useMyPageInfo } from "@/hooks/api/useMyPageQueries";
import { Select } from "flowbite-react";
import { FileText, MessageSquare, Heart, ThumbsUp } from "lucide-react";

const tabs = [
  { id: "my-posts", label: "작성글", shortLabel: "작성", icon: FileText, contentLabel: "작성한 게시글" },
  { id: "my-comments", label: "작성댓글", shortLabel: "댓글", icon: MessageSquare, contentLabel: "작성한 댓글" },
  { id: "liked-posts", label: "추천글", shortLabel: "추천", icon: ThumbsUp, contentLabel: "추천한 게시글" },
  { id: "liked-comments", label: "추천댓글", shortLabel: "추천♡", icon: Heart, contentLabel: "추천한 댓글" },
] as const;

type TabId = typeof tabs[number]["id"];

const isValidTab = (value: string | null): value is TabId => {
  return tabs.some((t) => t.id === value);
};

interface UserActivitySectionProps {
  className?: string;
}

const UserActivitySectionComponent: React.FC<UserActivitySectionProps> = ({ className }) => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const tabParam = searchParams.get("activeTab");

  const [postsPerPage, setPostsPerPage] = useState("10");

  const {
    myPosts,
    myComments,
    likedPosts,
    likedComments,
    isLoading,
    error,
    refetch,
    activeTab,
    setActiveTab,
    pagination,
  } = useUserActivityTabs(Number(postsPerPage));

  // B-310: SPA refetch wrapper (window.location.reload 대신)
  const handleRetry = useCallback(() => {
    void refetch();
  }, [refetch]);

  // B-303: URL `?activeTab=` 가 SSOT. 탭 클릭 시 router.replace 로 URL 갱신,
  // activeTab 상태는 URL 에서 파생 + useUserActivityTabs 에 동기화.
  const urlActiveTab: TabId = useMemo(() => {
    if (isValidTab(tabParam)) return tabParam;
    return "my-posts";
  }, [tabParam]);

  // URL → 내부 상태 동기화 (새로고침/뒤로가기/공유 링크 진입 시)
  useEffect(() => {
    if (urlActiveTab !== activeTab) {
      setActiveTab(urlActiveTab);
    }
  }, [urlActiveTab, activeTab, setActiveTab]);

  // 탭 카운트 뱃지: totalElements 활용 (응답 신설 X)
  const { data: mypageData } = useMyPageInfo(pagination.currentPage, pagination.pageSize);

  const counts: Record<TabId, number> = useMemo(() => {
    const data = mypageData?.data;
    return {
      "my-posts": data?.memberActivityPost?.writePosts?.totalElements ?? 0,
      "my-comments": data?.memberActivityComment?.writeComments?.totalElements ?? 0,
      "liked-posts": data?.memberActivityPost?.likedPosts?.totalElements ?? 0,
      "liked-comments": data?.memberActivityComment?.likedComments?.totalElements ?? 0,
    };
  }, [mypageData]);

  // 탭 클릭 → URL 갱신 (SSOT)
  const handleSelect = useCallback(
    (id: TabId) => {
      if (id === urlActiveTab) return;
      router.replace(`/mypage?activeTab=${id}`, { scroll: false });
    },
    [urlActiveTab, router],
  );

  // 키보드 화살표 네비게이션 (WAI-ARIA Tabs Pattern)
  const tabRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
      let nextIndex: number | null = null;
      if (event.key === "ArrowRight") {
        nextIndex = (index + 1) % tabs.length;
      } else if (event.key === "ArrowLeft") {
        nextIndex = (index - 1 + tabs.length) % tabs.length;
      } else if (event.key === "Home") {
        nextIndex = 0;
      } else if (event.key === "End") {
        nextIndex = tabs.length - 1;
      }

      if (nextIndex !== null) {
        event.preventDefault();
        const next = tabs[nextIndex];
        handleSelect(next.id);
        tabRefs.current[nextIndex]?.focus();
      }
    },
    [handleSelect],
  );

  // invalid tab 파라미터면 my-posts 로 정정
  useEffect(() => {
    if (tabParam !== null && !isValidTab(tabParam)) {
      router.replace("/mypage?activeTab=my-posts", { scroll: false });
    }
  }, [tabParam, router]);

  // 페이지 크기 변경
  const handlePostsPerPageChange = useCallback(
    (value: string) => {
      setPostsPerPage(value);
      pagination.setPageSize(Number(value));
      pagination.setCurrentPage(0);
    },
    [pagination],
  );

  const showPagination = pagination.totalPages > 0;

  // 탭별 데이터/로딩/에러 매핑
  const tabData = useMemo(() => ({
    "my-posts": { items: myPosts, contentType: "posts" as const },
    "my-comments": { items: myComments, contentType: "comments" as const },
    "liked-posts": { items: likedPosts, contentType: "posts" as const },
    "liked-comments": { items: likedComments, contentType: "comments" as const },
  }), [myPosts, myComments, likedPosts, likedComments]);

  return (
    <Card variant="elevated" className={className || ""}>
      <CardHeader>
        <div className="flex items-center justify-between gap-2 flex-wrap">
          <CardTitle>활동 내역</CardTitle>
          <div className="flex items-center gap-2">
            <label
              htmlFor="posts-per-page"
              className="text-sm text-ink-soft dark:text-ink-300 break-keep"
            >
              페이지당
            </label>
            <Select
              id="posts-per-page"
              value={postsPerPage}
              onChange={(e) => handlePostsPerPageChange(e.target.value)}
              sizing="sm"
              className="w-20"
            >
              <option value="10">10</option>
              <option value="15">15</option>
              <option value="30">30</option>
            </Select>
            <span className="text-sm text-ink-soft dark:text-ink-300 break-keep">개씩 보기</span>
          </div>
        </div>
      </CardHeader>
      <CardContent className="p-0">
        <div className="space-y-6 px-6 pb-6">
          {/* 탭 헤더 (sticky) — 라운드 9 FriendTabs 패턴 */}
          <div className="sticky top-0 z-10 -mx-6 px-6 mb-2 bg-paper-50/95 dark:bg-paper-900/95 backdrop-blur-sm border-b border-postal-navy/20">
            <div
              role="tablist"
              aria-label="활동 내역 탭"
              className="flex flex-wrap gap-1 sm:gap-2 overflow-x-auto"
            >
              {tabs.map(({ id, label, shortLabel, icon: Icon }, index) => {
                const isActive = urlActiveTab === id;
                const count = counts[id];
                const showBadge = count > 0;
                return (
                  <button
                    key={id}
                    ref={(el) => {
                      tabRefs.current[index] = el;
                    }}
                    type="button"
                    role="tab"
                    id={`activity-tab-${id}`}
                    aria-selected={isActive}
                    aria-controls={`activity-panel-${id}`}
                    tabIndex={isActive ? 0 : -1}
                    onClick={() => handleSelect(id)}
                    onKeyDown={(e) => handleKeyDown(e, index)}
                    className={`
                      inline-flex items-center gap-1.5 sm:gap-2 px-3 sm:px-4 py-3 font-medium text-sm
                      border-b-2 transition-colors break-keep min-h-[44px]
                      focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-postal-navy focus-visible:ring-offset-1 focus-visible:ring-offset-paper-50
                      ${isActive
                        ? "border-postal-navy text-postal-navy dark:text-ink-900"
                        : "border-transparent text-ink-soft hover:text-postal-navy hover:border-postal-navy/30"
                      }
                    `}
                  >
                    <Icon className="w-4 h-4 shrink-0" aria-hidden="true" />
                    <span className="hidden sm:inline">{label}</span>
                    <span className="sm:hidden">{shortLabel}</span>
                    {showBadge && (
                      <span
                        className={`
                          inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full text-xs font-semibold
                          ${isActive
                            ? "bg-postal-navy text-paper-50"
                            : "bg-postal-navy/15 text-postal-navy dark:bg-postal-navy/30 dark:text-ink-900"
                          }
                        `}
                        aria-label={`${count}건`}
                      >
                        {count}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* 탭 패널 */}
          {tabs.map(({ id, contentLabel }) => {
            const data = tabData[id];
            const isActive = urlActiveTab === id;
            return (
              <div
                key={id}
                role="tabpanel"
                id={`activity-panel-${id}`}
                aria-labelledby={`activity-tab-${id}`}
                hidden={!isActive}
              >
                {isActive && (
                  <Card variant="elevated">
                    <CardHeader>
                      <CardTitle className="flex items-center space-x-2">
                        <span>{contentLabel}</span>
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="p-0">
                      <UserActivityTable
                        items={data.items}
                        contentType={data.contentType}
                        tabType={id}
                        isLoading={isLoading}
                        error={error}
                        onRetry={handleRetry}
                      />
                    </CardContent>
                  </Card>
                )}
              </div>
            );
          })}

          {/* 페이지네이션 */}
          {showPagination && (
            <BoardPagination
              currentPage={pagination.currentPage}
              totalPages={pagination.totalPages}
              setCurrentPage={pagination.setCurrentPage}
            />
          )}
        </div>
      </CardContent>
    </Card>
  );
};

export const UserActivitySection = memo(UserActivitySectionComponent);

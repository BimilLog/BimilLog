"use client";

import React, { memo, useMemo } from "react";
import { Tabs, TabItem } from "flowbite-react";
import { ClipboardList, TrendingUp, Sparkles, Flame } from "lucide-react";
import { NoticeList } from "./notice-list";
import { BoardTable } from "./BoardTable";
import { BoardPagination } from "./board-pagination";
import { LoadMoreButton } from "./LoadMoreButton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import type { SimplePost } from "@/lib/api";
import type { PaginationState } from "@/hooks/common/usePagination";

interface BoardTabsProps {
  activeTab: string;
  onTabChange: (value: string) => void;

  // 전체글 탭 데이터
  posts: SimplePost[];
  isLoading?: boolean;
  error?: Error | null;
  isSearching?: boolean;
  searchTerm?: string;

  // 커서 기반 페이지네이션 (일반 목록)
  hasNextPage?: boolean;
  onLoadMore?: () => void;
  isFetchingNextPage?: boolean;

  // 검색용 offset 페이지네이션
  searchPagination?: PaginationState | null;
  onSearchPageChange?: (page: number) => void;

  // 인기글 탭 데이터
  realtimePosts: SimplePost[];
  weeklyPosts: SimplePost[];
  legendPosts: SimplePost[];
  legendPagination?: PaginationState | null;
  popularLoading?: boolean;
  popularError?: Error | null;

  // 공지사항 데이터
  noticePosts: SimplePost[];
}

const BoardTabsComponent: React.FC<BoardTabsProps> = ({
  activeTab,
  onTabChange,
  posts,
  isLoading = false,
  error = null,
  isSearching = false,
  searchTerm = "",
  hasNextPage = false,
  onLoadMore,
  isFetchingNextPage = false,
  searchPagination,
  onSearchPageChange,
  realtimePosts,
  weeklyPosts,
  legendPosts,
  legendPagination,
  popularLoading = false,
  popularError = null,
  noticePosts,
}) => {
  // 탭 값 매핑
  const getTabValue = (index: number) => {
    switch (index) {
      case 0: return "all";
      case 1: return "realtime";
      case 2: return "popular";
      case 3: return "legend";
      default: return "all";
    }
  };

  // 탭 변경 핸들러
  const handleTabChange = (index: number) => {
    const value = getTabValue(index);
    onTabChange(value);
  };

  // 페이지네이션 표시 조건 메모화
  // 검색 중이거나 레전드 탭일 때만 BoardPagination 표시
  const showPagination = useMemo(() => {
    if (activeTab === "all" && isSearching && searchPagination && searchPagination.totalPages > 0) return true;
    if (activeTab === "legend" && legendPagination && legendPagination.totalPages > 0) return true;
    return false;
  }, [activeTab, isSearching, searchPagination, legendPagination]);

  // 더보기 버튼 표시 조건 (일반 목록일 때만)
  const showLoadMore = useMemo(() => {
    return activeTab === "all" && !isSearching && posts.length > 0;
  }, [activeTab, isSearching, posts.length]);

  // 탭 스타일 커스터마이징 - 가로 배치를 위한 수정
  // 라운드 6 (F-207): paper/ink/postal-navy 토큰 일관 적용 (편지/우편 메타포)
  const tabsTheme = {
    base: "flex flex-col gap-2",
    tablist: {
      base: "flex text-center",
      variant: {
        default: "flex-wrap border-b border-border",
        underline: "-mb-px flex-wrap border-b border-border",
        pills: "flex-wrap space-x-2 text-sm font-medium text-muted-foreground",
        fullWidth: "grid w-full grid-cols-4 divide-x divide-border rounded-lg shadow-sm border border-border"
      },
      tabitem: {
        base: "flex items-center justify-center p-4 text-sm font-medium first:ml-0 focus:outline-none disabled:cursor-not-allowed disabled:text-muted-foreground/50",
        variant: {
          default: {
            base: "rounded-t-lg",
            active: {
              on: "bg-accent text-postal-navy dark:bg-slate-800 dark:text-stamp-red",
              off: "text-muted-foreground hover:bg-accent hover:text-foreground"
            }
          },
          underline: {
            base: "rounded-t-lg",
            active: {
              on: "rounded-t-lg border-b-2 border-postal-navy text-postal-navy dark:border-stamp-red dark:text-stamp-red",
              off: "border-b-2 border-transparent text-muted-foreground hover:border-border hover:text-foreground"
            }
          },
          pills: {
            base: "",
            active: {
              on: "rounded-lg bg-postal-navy text-paper-50",
              off: "rounded-lg text-muted-foreground hover:bg-accent hover:text-foreground"
            }
          },
          fullWidth: {
            base: "flex-1 rounded-none first:rounded-l-lg last:rounded-r-lg border-b-2 border-transparent",
            active: {
              on: "bg-paper-50 text-stamp-red border-b-2 border-stamp-red dark:bg-slate-800 dark:text-stamp-red",
              off: "bg-paper-aged/40 text-muted-foreground hover:bg-paper-aged/70 hover:text-foreground dark:bg-slate-900/60 dark:hover:bg-slate-800 dark:hover:text-foreground"
            }
          }
        },
        icon: "mr-2 h-5 w-5"
      }
    },
    tabpanel: "py-3"
  };

  return (
    <div className="space-y-6">
      {/* 라운드 17 F-17-BUG-11: Flowbite Tabs 는 자체적으로 role=tablist/tab/tabpanel +
          aria-controls/aria-selected/aria-labelledby 를 자동 부여 (검증됨).
          aria-label 을 한국어 + 도메인 명시로 개선해 스크린리더 가독성 향상. */}
      <Tabs
        aria-label="게시판 카테고리 탭 — 전체/실시간/주간/명예의 전당"
        variant="fullWidth"
        onActiveTabChange={handleTabChange}
        theme={tabsTheme}
        className="w-full"
      >
        <TabItem
          active={activeTab === "all"}
          title={
            <span className="whitespace-nowrap break-keep">
              <span className="hidden sm:inline">전체 게시판</span>
              <span className="sm:hidden">전체</span>
            </span>
          }
        >
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <ClipboardList className="w-5 h-5 text-postal-navy" />
                <span>
                  {isSearching && searchTerm ? `'${searchTerm}' 검색 결과` : "전체 게시판"}
                </span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="space-y-4">
                {/* F-BUG-8 (round-6): 검색 모드에서는 NoticeList 시각적 부조화 → 숨김 */}
                {!isSearching && <NoticeList posts={noticePosts} />}
                <BoardTable
                  posts={posts}
                  variant="all"
                  isLoading={isLoading}
                  error={error}
                  isSearching={isSearching}
                  searchTerm={searchTerm}
                />
              </div>
            </CardContent>
          </Card>
        </TabItem>
        <TabItem
          active={activeTab === "realtime"}
          title={
            <span className="whitespace-nowrap break-keep">
              <span className="hidden sm:inline">실시간 인기글</span>
              <span className="sm:hidden">실시간</span>
            </span>
          }
        >
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Flame className="w-5 h-5 text-stamp-red" />
                <span>실시간 인기글</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <BoardTable
                posts={realtimePosts}
                variant="popular"
                isLoading={popularLoading}
                error={popularError}
              />
            </CardContent>
          </Card>
        </TabItem>
        <TabItem
          active={activeTab === "popular"}
          title={
            <span className="whitespace-nowrap break-keep">
              <span className="hidden sm:inline">주간 인기글</span>
              <span className="sm:hidden">주간</span>
            </span>
          }
        >
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <TrendingUp className="w-5 h-5 text-seal-gold" />
                <span>주간 인기글</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <BoardTable
                posts={weeklyPosts}
                variant="popular"
                isLoading={popularLoading}
                error={popularError}
              />
            </CardContent>
          </Card>
        </TabItem>
        <TabItem
          active={activeTab === "legend"}
          title={
            <span className="whitespace-nowrap break-keep">
              <span className="hidden sm:inline">명예의 전당</span>
              <span className="sm:hidden">명예</span>
            </span>
          }
        >
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Sparkles className="w-5 h-5 text-stamp-red" />
                <span>레전드 글</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <BoardTable
                posts={legendPosts}
                variant="legend"
                isLoading={popularLoading}
                error={popularError}
              />
            </CardContent>
          </Card>
        </TabItem>
      </Tabs>

      {/* 커서 기반 더보기 버튼 (일반 목록) */}
      {showLoadMore && onLoadMore && (
        <LoadMoreButton
          onClick={onLoadMore}
          isLoading={isFetchingNextPage}
          hasMore={hasNextPage}
        />
      )}

      {/* offset 기반 페이지네이션 (검색, 레전드) */}
      <div
        className={`transition-opacity duration-200 ${
          showPagination ? 'opacity-100' : 'opacity-0 pointer-events-none invisible'
        }`}
      >
        <BoardPagination
          currentPage={activeTab === "legend" && legendPagination
            ? legendPagination.currentPage
            : (searchPagination?.currentPage ?? 0)}
          totalPages={activeTab === "legend" && legendPagination
            ? legendPagination.totalPages
            : (searchPagination?.totalPages ?? 0)}
          setCurrentPage={activeTab === "legend" && legendPagination
            ? legendPagination.setCurrentPage
            : (onSearchPageChange ?? (() => {}))}
        />
      </div>
    </div>
  );
};

export const BoardTabs = memo(BoardTabsComponent);
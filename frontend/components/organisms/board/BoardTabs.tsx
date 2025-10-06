"use client";

import React, { memo, useMemo } from "react";
import { Tabs, TabItem } from "flowbite-react";
import { HiClipboardList, HiTrendingUp, HiSparkles, HiFire } from "react-icons/hi";
import { NoticeList } from "./notice-list";
import { BoardTable } from "./BoardTable";
import { BoardPagination } from "./board-pagination";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import type { SimplePost } from "@/lib/api";
import type { PaginationState } from "@/hooks/common/usePagination";

interface BoardTabsProps {
  activeTab: string;
  onTabChange: (value: string) => void;

  // 전체글 탭 데이터
  posts: SimplePost[];
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;

  // 인기글 탭 데이터
  realtimePosts: SimplePost[];
  weeklyPosts: SimplePost[];
  legendPosts: SimplePost[];
  legendPagination?: PaginationState | null;
}

const BoardTabsComponent: React.FC<BoardTabsProps> = ({
  activeTab,
  onTabChange,
  posts,
  currentPage,
  totalPages,
  onPageChange,
  realtimePosts,
  weeklyPosts,
  legendPosts,
  legendPagination,
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
  const showPagination = useMemo(() => {
    if (activeTab === "all" && totalPages > 0) return true;
    if (activeTab === "legend" && legendPagination && legendPagination.totalPages > 0) return true;
    return false;
  }, [activeTab, totalPages, legendPagination]);

  // 탭 스타일 커스터마이징 - 가로 배치를 위한 수정
  const tabsTheme = {
    base: "flex flex-col gap-2",
    tablist: {
      base: "flex text-center",
      variant: {
        default: "flex-wrap border-b border-gray-200 dark:border-gray-700",
        underline: "-mb-px flex-wrap border-b border-gray-200 dark:border-gray-700",
        pills: "flex-wrap space-x-2 text-sm font-medium text-gray-500 dark:text-gray-400",
        fullWidth: "grid w-full grid-cols-4 divide-x divide-gray-200 rounded-lg shadow-sm border border-gray-200 dark:divide-gray-700 dark:border-gray-700"
      },
      tabitem: {
        base: "flex items-center justify-center p-4 text-sm font-medium first:ml-0 focus:outline-none disabled:cursor-not-allowed disabled:text-gray-400 disabled:dark:text-gray-500",
        variant: {
          default: {
            base: "rounded-t-lg",
            active: {
              on: "bg-gray-100 text-primary-600 dark:bg-gray-800 dark:text-primary-500",
              off: "text-gray-500 hover:bg-gray-50 hover:text-gray-600 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-gray-300"
            }
          },
          underline: {
            base: "rounded-t-lg",
            active: {
              on: "rounded-t-lg border-b-2 border-primary-600 text-primary-600 dark:border-primary-500 dark:text-primary-500",
              off: "border-b-2 border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-600 dark:text-gray-400 dark:hover:text-gray-300"
            }
          },
          pills: {
            base: "",
            active: {
              on: "rounded-lg bg-primary-600 text-white",
              off: "rounded-lg hover:bg-gray-100 hover:text-gray-900 dark:hover:bg-gray-800 dark:hover:text-white"
            }
          },
          fullWidth: {
            base: "flex-1 rounded-none first:rounded-l-lg last:rounded-r-lg",
            active: {
              on: "bg-white text-purple-600 dark:bg-gray-700 dark:text-purple-400",
              off: "bg-gray-50 text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
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
      <Tabs
        aria-label="Board tabs"
        variant="fullWidth"
        onActiveTabChange={handleTabChange}
        theme={tabsTheme}
        className="w-full"
      >
        <TabItem
          active={activeTab === "all"}
          title="전체 게시판"
        >
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <HiClipboardList className="w-5 h-5 text-blue-500" />
                <span>전체 게시판</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="space-y-4">
                <NoticeList posts={posts} />
                <BoardTable
                  posts={posts}
                  variant="all"
                />
              </div>
            </CardContent>
          </Card>
        </TabItem>
        <TabItem
          active={activeTab === "realtime"}
          title="실시간 인기글"
        >
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <HiFire className="w-5 h-5 text-red-500" />
                <span>실시간 인기글</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <BoardTable
                posts={realtimePosts}
                variant="popular"
              />
            </CardContent>
          </Card>
        </TabItem>
        <TabItem
          active={activeTab === "popular"}
          title="주간 인기글"
        >
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <HiTrendingUp className="w-5 h-5 text-orange-500" />
                <span>주간 인기글</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <BoardTable
                posts={weeklyPosts}
                variant="popular"
              />
            </CardContent>
          </Card>
        </TabItem>
        <TabItem
          active={activeTab === "legend"}
          title="명예의 전당"
        >
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <HiSparkles className="w-5 h-5 text-purple-500" />
                <span>레전드 글</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <BoardTable
                posts={legendPosts}
                variant="legend"
              />
            </CardContent>
          </Card>
        </TabItem>
      </Tabs>

      {showPagination && (
        <BoardPagination
          currentPage={activeTab === "legend" && legendPagination ? legendPagination.currentPage : currentPage}
          totalPages={activeTab === "legend" && legendPagination ? legendPagination.totalPages : totalPages}
          setCurrentPage={activeTab === "legend" && legendPagination ? legendPagination.setCurrentPage : onPageChange}
        />
      )}
    </div>
  );
};

export const BoardTabs = memo(BoardTabsComponent);
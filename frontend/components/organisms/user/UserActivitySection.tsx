"use client";

import React, { useState, useCallback, useMemo, memo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { Tabs, TabItem } from "flowbite-react";
import { UserActivityTable } from "./UserActivityTable";
import { BoardPagination } from "@/components/organisms/board/board-pagination";
import { useUserActivityTabs } from "@/hooks/features/user/useUserActivity";
import { Select } from "flowbite-react";

interface UserActivitySectionProps {
  className?: string;
}

const UserActivitySectionComponent: React.FC<UserActivitySectionProps> = ({ className }) => {
  const [postsPerPage, setPostsPerPage] = useState("10");

  // 사용자 활동 데이터 관리
  const {
    myPosts,
    myComments,
    likedPosts,
    likedComments,
    isLoading,
    error,
    activeTab,
    setActiveTab,
    pagination,
  } = useUserActivityTabs(Number(postsPerPage));

  // 탭 값 매핑
  const getTabValue = (index: number) => {
    switch (index) {
      case 0: return "my-posts";
      case 1: return "my-comments";
      case 2: return "liked-posts";
      case 3: return "liked-comments";
      default: return "my-posts";
    }
  };

  // 탭 변경 핸들러
  const handleTabChange = useCallback((index: number) => {
    const value = getTabValue(index);
    setActiveTab(value as typeof activeTab);
  }, [setActiveTab]);

  // 페이지 크기 변경 시 처리
  const handlePostsPerPageChange = useCallback((value: string) => {
    setPostsPerPage(value);
    pagination.setPageSize(Number(value));
    pagination.setCurrentPage(0);
  }, [pagination]);

  // 페이지네이션 표시 조건
  const showPagination = pagination.totalPages > 0;

  // 현재 탭에 따른 로딩/에러 상태
  const myPostsLoading = activeTab === "my-posts" && isLoading;
  const myCommentsLoading = activeTab === "my-comments" && isLoading;
  const likedPostsLoading = activeTab === "liked-posts" && isLoading;
  const likedCommentsLoading = activeTab === "liked-comments" && isLoading;

  const myPostsError = activeTab === "my-posts" ? error : null;
  const myCommentsError = activeTab === "my-comments" ? error : null;
  const likedPostsError = activeTab === "liked-posts" ? error : null;
  const likedCommentsError = activeTab === "liked-comments" ? error : null;

  // 탭 스타일 커스터마이징
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
    <Card variant="elevated" className={className || ""}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>활동 내역</CardTitle>
          <div className="flex items-center gap-2">
            <label htmlFor="posts-per-page" className="text-sm text-gray-600 dark:text-gray-400">
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
            <span className="text-sm text-gray-600 dark:text-gray-400">개씩 보기</span>
          </div>
        </div>
      </CardHeader>
      <CardContent className="p-0">
        <div className="space-y-6 px-6 pb-6">
          <Tabs
            aria-label="User activity tabs"
            variant="fullWidth"
            onActiveTabChange={handleTabChange}
            theme={tabsTheme}
            className="w-full"
          >
            <TabItem
              active={activeTab === "my-posts"}
              title="작성글"
            >
              <Card variant="elevated">
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <span>작성한 게시글</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                  <UserActivityTable
                    items={myPosts}
                    contentType="posts"
                    tabType="my-posts"
                    isLoading={myPostsLoading}
                    error={myPostsError}
                  />
                </CardContent>
              </Card>
            </TabItem>
            <TabItem
              active={activeTab === "my-comments"}
              title="작성댓글"
            >
              <Card variant="elevated">
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <span>작성한 댓글</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                  <UserActivityTable
                    items={myComments}
                    contentType="comments"
                    tabType="my-comments"
                    isLoading={myCommentsLoading}
                    error={myCommentsError}
                  />
                </CardContent>
              </Card>
            </TabItem>
            <TabItem
              active={activeTab === "liked-posts"}
              title="추천글"
            >
              <Card variant="elevated">
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <span>추천한 게시글</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                  <UserActivityTable
                    items={likedPosts}
                    contentType="posts"
                    tabType="liked-posts"
                    isLoading={likedPostsLoading}
                    error={likedPostsError}
                  />
                </CardContent>
              </Card>
            </TabItem>
            <TabItem
              active={activeTab === "liked-comments"}
              title="추천댓글"
            >
              <Card variant="elevated">
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <span>추천한 댓글</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                  <UserActivityTable
                    items={likedComments}
                    contentType="comments"
                    tabType="liked-comments"
                    isLoading={likedCommentsLoading}
                    error={likedCommentsError}
                  />
                </CardContent>
              </Card>
            </TabItem>
          </Tabs>

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

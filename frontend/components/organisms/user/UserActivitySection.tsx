"use client";

import React, { useCallback, useMemo, memo } from "react";
import { Card, CardContent } from "@/components";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components";
import { Badge } from "@/components";
import { Button } from "@/components";
import { Alert, AlertDescription } from "@/components";
import { LoadingSpinner } from "@/components/atoms";
import { EmptyState } from "@/components/molecules";
import { ActivityCard } from "@/components/molecules";
import { useActivityData } from "@/hooks";
import { userQuery } from "@/lib/api";
import { AlertTriangle, RefreshCw, ChevronLeft, ChevronRight } from "lucide-react";
import { logger } from "@/lib/utils";

const DEFAULT_PAGE = 0;
const DEFAULT_PAGE_SIZE = 10;

interface ActivityTabContentProps {
  fetchData: (page?: number, size?: number) => Promise<{
    content: any[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
  }>;
  contentType: "posts" | "comments" | "liked-posts" | "liked-comments";
}

const ActivityTabContent: React.FC<ActivityTabContentProps> = memo(({
  fetchData,
  contentType,
}) => {
  const {
    items,
    isLoading,
    error,
    currentPage,
    totalPages,
    totalElements,
    isLoadingMore,
    handleLoadMore,
    handlePageChange,
    retry,
  } = useActivityData({ fetchData });

  // 페이지네이션 핸들러 최적화
  const handlePreviousPage = useCallback(() => {
    handlePageChange(currentPage - 1);
  }, [handlePageChange, currentPage]);

  const handleNextPage = useCallback(() => {
    handlePageChange(currentPage + 1);
  }, [handlePageChange, currentPage]);

  // 페이지 번호 생성 메모화
  const pageNumbers = useMemo(() => {
    const pages = [];
    const maxPages = Math.min(5, totalPages);
    for (let i = 0; i < maxPages; i++) {
      const pageNum = Math.max(0, Math.min(currentPage - 2 + i, totalPages - 1));
      if (pageNum >= 0 && pageNum < totalPages) {
        pages.push(pageNum);
      }
    }
    return [...new Set(pages)].sort((a, b) => a - b);
  }, [currentPage, totalPages]);

  // 컨텐츠 타입별 설정 메모화
  const contentConfig = useMemo(() => {
    const isPost = contentType.includes("posts");
    const isLiked = contentType.includes("liked");
    return {
      isPost,
      isLiked,
      badge: isPost 
        ? <Badge className="bg-blue-100 text-blue-700 border-blue-200">게시글</Badge>
        : <Badge className="bg-green-100 text-green-700 border-green-200">댓글</Badge>
    };
  }, [contentType]);

  if (isLoading) {
    return (
      <LoadingSpinner
        variant="gradient" 
        message="데이터를 불러오는 중..."
        className="py-16"
      />
    );
  }

  if (error) {
    return (
      <Alert className="border-red-200 bg-red-50 mt-6">
        <AlertTriangle className="h-4 w-4 text-red-600" />
        <AlertDescription className="text-red-800">
          <div className="flex items-center justify-between">
            <span>{error}</span>
            <Button onClick={retry} variant="outline" size="sm" className="ml-4">
              <RefreshCw className="w-4 h-4 mr-2" />
              다시 시도
            </Button>
          </div>
        </AlertDescription>
      </Alert>
    );
  }

  if (items.length === 0) {
    return <EmptyState type={contentType} />;
  }

  return (
    <div className="space-y-6 mt-6">
      <div className="flex items-center justify-between pb-4 border-b border-gray-200">
        <div className="flex items-center space-x-2">
          <span className="text-lg font-semibold text-gray-800">
            총 {totalElements.toLocaleString()}개
          </span>
          {contentConfig.badge}
        </div>
        {totalPages > 1 && (
          <div className="text-sm text-gray-600">
            페이지 {currentPage + 1} / {totalPages}
          </div>
        )}
      </div>

      <div className="grid gap-6">
        {items.map((item) => (
          <ActivityCard
            key={`${contentType}-${item.id}`}
            item={item}
            type={contentConfig.isPost ? "post" : "comment"}
            isLiked={contentConfig.isLiked}
          />
        ))}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center space-x-2 pt-6 border-t border-gray-200">
          <Button
            variant="outline"
            size="sm"
            onClick={handlePreviousPage}
            disabled={currentPage === 0}
          >
            <ChevronLeft className="w-4 h-4" />
            이전
          </Button>

          <div className="flex items-center space-x-1">
            {pageNumbers.map((pageNum) => (
              <Button
                key={`page-${pageNum}`}
                variant={pageNum === currentPage ? "default" : "outline"}
                size="sm"
                onClick={() => handlePageChange(pageNum)}
                className="w-10"
              >
                {pageNum + 1}
              </Button>
            ))}
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={handleNextPage}
            disabled={currentPage === totalPages - 1}
          >
            다음
            <ChevronRight className="w-4 h-4" />
          </Button>
        </div>
      )}

      {currentPage < totalPages - 1 && (
        <div className="text-center pt-4">
          <Button
            variant="outline"
            onClick={handleLoadMore}
            disabled={isLoadingMore}
            className="w-full max-w-xs"
          >
            {isLoadingMore ? (
              <>
                <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                불러오는 중...
              </>
            ) : (
              <>더보기 ({items.length} / {totalElements})</>
            )}
          </Button>
        </div>
      )}
    </div>
  );
});

ActivityTabContent.displayName = "ActivityTabContent";

interface UserActivitySectionProps {
  className?: string;
}

const UserActivitySectionComponent: React.FC<UserActivitySectionProps> = ({ className }) => {
  // 탭 설정 메모화
  const tabConfig = useMemo(() => ([
    {
      value: "my-posts",
      label: { desktop: "작성한 글", mobile: "글" },
      contentType: "posts" as const
    },
    {
      value: "my-comments",
      label: { desktop: "작성한 댓글", mobile: "댓글" },
      contentType: "comments" as const
    },
    {
      value: "liked-posts",
      label: { desktop: "추천한 글", mobile: "추천글" },
      contentType: "liked-posts" as const
    },
    {
      value: "liked-comments",
      label: { desktop: "추천한 댓글", mobile: "추천댓글" },
      contentType: "liked-comments" as const
    }
  ]), []);

  // API 호출 함수들 메모화
  const fetchMyPosts = useCallback(
    async (page = DEFAULT_PAGE, size = DEFAULT_PAGE_SIZE) => {
      try {
        const response = await userQuery.getUserPosts(page, size);
        if (response.success && response.data) {
          return {
            content: response.data.content || [],
            totalElements: response.data.totalElements || 0,
            totalPages: response.data.totalPages || 0,
            currentPage: response.data.number || 0,
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, currentPage: 0 };
      } catch (error) {
        logger.error("Failed to fetch user posts:", error);
        throw error;
      }
    },
    []
  );

  const fetchMyComments = useCallback(
    async (page = DEFAULT_PAGE, size = DEFAULT_PAGE_SIZE) => {
      try {
        const response = await userQuery.getUserComments(page, size);
        if (response.success && response.data) {
          return {
            content: response.data.content || [],
            totalElements: response.data.totalElements || 0,
            totalPages: response.data.totalPages || 0,
            currentPage: response.data.number || 0,
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, currentPage: 0 };
      } catch (error) {
        logger.error("Failed to fetch user comments:", error);
        throw error;
      }
    },
    []
  );

  const fetchLikedPosts = useCallback(
    async (page = DEFAULT_PAGE, size = DEFAULT_PAGE_SIZE) => {
      try {
        const response = await userQuery.getUserLikedPosts(page, size);
        if (response.success && response.data) {
          return {
            content: response.data.content || [],
            totalElements: response.data.totalElements || 0,
            totalPages: response.data.totalPages || 0,
            currentPage: response.data.number || 0,
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, currentPage: 0 };
      } catch (error) {
        logger.error("Failed to fetch liked posts:", error);
        throw error;
      }
    },
    []
  );

  const fetchLikedComments = useCallback(
    async (page = DEFAULT_PAGE, size = DEFAULT_PAGE_SIZE) => {
      try {
        const response = await userQuery.getUserLikedComments(page, size);
        if (response.success && response.data) {
          return {
            content: response.data.content || [],
            totalElements: response.data.totalElements || 0,
            totalPages: response.data.totalPages || 0,
            currentPage: response.data.number || 0,
          };
        }
        return { content: [], totalElements: 0, totalPages: 0, currentPage: 0 };
      } catch (error) {
        logger.error("Failed to fetch liked comments:", error);
        throw error;
      }
    },
    []
  );

  // fetch 함수 매핑 메모화
  const fetchFunctions = useMemo(() => ({
    "my-posts": fetchMyPosts,
    "my-comments": fetchMyComments,
    "liked-posts": fetchLikedPosts,
    "liked-comments": fetchLikedComments,
  }), [fetchMyPosts, fetchMyComments, fetchLikedPosts, fetchLikedComments]);

  return (
    <Card className={`bg-white/80 backdrop-blur-sm border-0 shadow-lg ${className || ""}`}>
      <CardContent className="p-6">
        <Tabs defaultValue="my-posts" className="w-full">
          <TabsList className="grid w-full grid-cols-4 bg-gray-100 h-12 md:h-10 p-1">
            {tabConfig.map(({ value, label }) => (
              <TabsTrigger
                key={value}
                value={value}
                className="flex items-center justify-center h-full px-2 py-2 text-xs"
              >
                <span className="hidden sm:inline">{label.desktop}</span>
                <span className="sm:hidden">{label.mobile}</span>
              </TabsTrigger>
            ))}
          </TabsList>
          
          {tabConfig.map(({ value, contentType }) => (
            <TabsContent key={value} value={value}>
              <ActivityTabContent 
                fetchData={fetchFunctions[value as keyof typeof fetchFunctions]} 
                contentType={contentType} 
              />
            </TabsContent>
          ))}
        </Tabs>
      </CardContent>
    </Card>
  );
};

export const UserActivitySection = memo(UserActivitySectionComponent);
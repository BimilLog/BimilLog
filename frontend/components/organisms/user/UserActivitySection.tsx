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

  // 페이지네이션 핸들러: 이전/다음 페이지 이동 시 불필요한 리렌더링 방지를 위해 useCallback 사용
  const handlePreviousPage = useCallback(() => {
    handlePageChange(currentPage - 1);
  }, [handlePageChange, currentPage]);

  const handleNextPage = useCallback(() => {
    handlePageChange(currentPage + 1);
  }, [handlePageChange, currentPage]);

  // 페이지 번호 배열 생성: 현재 페이지 중심으로 최대 5개 페이지 표시
  // 예: 현재 페이지 5일 때 [3, 4, 5, 6, 7] 형태로 생성
  const pageNumbers = useMemo(() => {
    const pages = [];
    const maxPages = Math.min(5, totalPages); // 최대 5개 페이지만 표시
    for (let i = 0; i < maxPages; i++) {
      // 현재 페이지를 중심으로 앞뒤 2페이지씩 계산
      const pageNum = Math.max(0, Math.min(currentPage - 2 + i, totalPages - 1));
      if (pageNum >= 0 && pageNum < totalPages) {
        pages.push(pageNum);
      }
    }
    // 중복 제거 후 정렬하여 반환 (Set으로 중복 제거, sort로 오름차순 정렬)
    return [...new Set(pages)].sort((a, b) => a - b);
  }, [currentPage, totalPages]);

  // 컨텐츠 타입별 UI 설정: "posts"/"comments", "liked" 여부에 따라 배지와 플래그 설정
  // 문자열 분석을 통해 게시글/댓글, 좋아요 여부 판단하여 적절한 배지 색상 적용
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
  // 탭 설정 메모화: 4개 탭의 설정을 한 번만 계산하여 리렌더링 최적화
  // desktop/mobile 라벨을 분리하여 반응형 UI 지원
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

  // API 호출 함수들: 각 탭에서 사용할 데이터 fetch 함수를 메모화
  // 공통 응답 형태로 정규화하여 ActivityTabContent에서 일관되게 처리
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

  // 탭별 fetch 함수 매핑: 각 탭의 value와 해당하는 API 함수를 연결
  // ActivityTabContent에서 동적으로 적절한 fetch 함수를 선택하여 사용
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
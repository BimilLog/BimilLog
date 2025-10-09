"use client";

import React, { useCallback, useMemo, memo, useState, useEffect } from "react";
import { Card, CardContent } from "@/components";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components";
import { Badge } from "@/components";
import { Button } from "@/components";
import { Alert, AlertDescription } from "@/components";
import { LoadingSpinner } from "@/components/atoms";
import { EmptyState } from "@/components/molecules";
import { ActivityCard } from "@/components/molecules";
import { useActivityData } from "@/hooks";
import { useDebouncedCallback } from "@/hooks/common/useDebounce";
import { userQuery } from "@/lib/api";
import { AlertTriangle, RefreshCw } from "lucide-react";
import { Pagination, Spinner as FlowbiteSpinner } from "flowbite-react";
import { logger, formatNumber } from "@/lib/utils";
import type { Post, SimplePost } from "@/types/domains/post";
import type { Comment, SimpleComment } from "@/types/domains/comment";

type ActivityItem = Post | SimplePost | Comment | SimpleComment;

const DEFAULT_PAGE = 0;
const DEFAULT_PAGE_SIZE = 10;

interface ActivityTabContentProps {
  fetchData: (page?: number, size?: number) => Promise<{
    content: unknown[];
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
  // 반응형 페이지네이션: 모바일(768px 미만)은 무한 스크롤, 데스크톱은 페이지네이션
  const [isDesktop, setIsDesktop] = useState(false);

  // useActivityData를 먼저 호출하여 currentPage, loadAllPagesForMobile 획득
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
    loadAllPagesForMobile,
  } = useActivityData({ fetchData });

  // debounce를 적용한 resize 핸들러로 성능 최적화
  // 데스크톱→모바일 전환 시 데이터 불연속 방지: 0페이지부터 현재 페이지까지 모두 로드
  const handleResize = useDebouncedCallback(
    () => {
      const newIsDesktop = window.innerWidth >= 768;
      const wasDesktop = isDesktop;

      // 데스크톱→모바일 전환 + 현재 페이지가 0이 아닐 때만 리셋
      if (wasDesktop && !newIsDesktop && currentPage > 0) {
        // 무한 스크롤을 위해 페이지 0부터 currentPage까지 모두 로드
        loadAllPagesForMobile(currentPage);
      }

      setIsDesktop(newIsDesktop);
    },
    200,
    [isDesktop, currentPage, loadAllPagesForMobile]
  );

  useEffect(() => {
    setIsDesktop(window.innerWidth >= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [handleResize]);

  // Flowbite React Pagination 핸들러: 0-based에서 1-based로 변환
  const handleFlowbitePagination = useCallback((page: number) => {
    handlePageChange(page - 1); // Convert 1-based to 0-based
  }, [handlePageChange]);

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
        <AlertTriangle className="h-4 w-4" />
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
    // contentType별로 적절한 CTA 제공
    const emptyStateProps = {
      posts: {
        actionLabel: "글쓰기",
        actionHref: "/board/write"
      },
      comments: {
        actionLabel: "커뮤니티 둘러보기",
        actionHref: "/board"
      },
      "liked-posts": {
        actionLabel: "커뮤니티 둘러보기",
        actionHref: "/board"
      },
      "liked-comments": {
        title: "아직 추천한 댓글이 없어요",
        description: "마음에 드는 댓글에 추천을 눌러보세요!",
        actionLabel: "커뮤니티 둘러보기",
        actionHref: "/board"
      }
    };

    const ctaProps = emptyStateProps[contentType] || {};

    return <EmptyState type={contentType} {...ctaProps} />;
  }

  return (
    <div className="space-y-6 mt-6">
      <div className="flex items-center justify-between pb-4 border-b border-gray-200">
        <div className="flex items-center space-x-2">
          <span className="text-lg font-semibold text-brand-primary">
            총 {formatNumber(totalElements)}개
          </span>
          {contentConfig.badge}
        </div>
        {totalPages > 1 && (
          <div className="text-sm text-brand-muted">
            {isDesktop
              ? `페이지 ${currentPage + 1} / ${totalPages}`
              : `${items.length}개 로드됨`
            }
          </div>
        )}
      </div>

      <div className="grid gap-6">
        {(items as ActivityItem[]).map((item) => (
          <ActivityCard
            key={`${contentType}-${item.id}`}
            item={item}
            type={contentConfig.isPost ? "post" : "comment"}
            isLiked={contentConfig.isLiked}
          />
        ))}
      </div>

      {/* 반응형 페이지네이션: 데스크톱은 Pagination, 모바일은 더보기 버튼 */}
      {totalPages > 1 && isDesktop && (
        <div className="flex items-center justify-center pt-6 border-t border-gray-200">
          <Pagination
            currentPage={currentPage + 1} // Convert 0-based to 1-based
            totalPages={totalPages}
            onPageChange={handleFlowbitePagination}
            showIcons
            className="text-sm"
            theme={{
              pages: {
                base: "xs:mt-0 mt-2 inline-flex items-center -space-x-px",
                showIcon: "inline-flex",
                previous: {
                  base: "ml-0 rounded-l-lg border border-gray-300 bg-white py-2 px-3 leading-tight text-gray-500 hover:bg-gray-100 hover:text-gray-700",
                  icon: "h-4 w-4"
                },
                next: {
                  base: "rounded-r-lg border border-gray-300 bg-white py-2 px-3 leading-tight text-gray-500 hover:bg-gray-100 hover:text-gray-700",
                  icon: "h-4 w-4"
                },
                selector: {
                  base: "w-12 border border-gray-300 bg-white py-2 px-3 leading-tight text-gray-500 hover:bg-gray-100 hover:text-gray-700",
                  active: "bg-brand-primary border-brand-primary text-white hover:bg-brand-primary hover:text-white",
                  disabled: "cursor-not-allowed opacity-50"
                }
              }
            }}
          />
        </div>
      )}

      {/* 모바일 무한 스크롤: 더보기 버튼 */}
      {currentPage < totalPages - 1 && !isDesktop && (
        <div className="text-center pt-4">
          <Button
            variant="outline"
            onClick={handleLoadMore}
            disabled={isLoadingMore}
            className="w-full max-w-xs"
          >
            {isLoadingMore ? (
              <div className="flex items-center justify-center">
                <FlowbiteSpinner color="pink" size="sm" aria-label="불러오는 중..." className="mr-2" />
                <span>불러오는 중...</span>
              </div>
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
    <Card variant="elevated" className={className || ""}>
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
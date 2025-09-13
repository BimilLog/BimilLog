import React, { useCallback } from "react";
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

const ActivityTabContent: React.FC<ActivityTabContentProps> = ({
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

  const isPost = contentType.includes("posts");
  const isLiked = contentType.includes("liked");

  return (
    <div className="space-y-6 mt-6">
      <div className="flex items-center justify-between pb-4 border-b border-gray-200">
        <div className="flex items-center space-x-2">
          <span className="text-lg font-semibold text-gray-800">
            총 {totalElements.toLocaleString()}개
          </span>
          {isPost && (
            <Badge className="bg-blue-100 text-blue-700 border-blue-200">게시글</Badge>
          )}
          {!isPost && (
            <Badge className="bg-green-100 text-green-700 border-green-200">댓글</Badge>
          )}
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
            key={item.id}
            item={item}
            type={isPost ? "post" : "comment"}
            isLiked={isLiked}
          />
        ))}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center space-x-2 pt-6 border-t border-gray-200">
          <Button
            variant="outline"
            size="sm"
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 0}
          >
            <ChevronLeft className="w-4 h-4" />
            이전
          </Button>

          <div className="flex items-center space-x-1">
            {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
              const pageNum = Math.max(0, Math.min(currentPage - 2 + i, totalPages - 1));
              if (pageNum < 0 || pageNum >= totalPages) return null;

              return (
                <Button
                  key={pageNum}
                  variant={pageNum === currentPage ? "default" : "outline"}
                  size="sm"
                  onClick={() => handlePageChange(pageNum)}
                  className="w-10"
                >
                  {pageNum + 1}
                </Button>
              );
            }).filter(Boolean)}
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => handlePageChange(currentPage + 1)}
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
};

interface UserActivitySectionProps {
  className?: string;
}

export const UserActivitySection: React.FC<UserActivitySectionProps> = ({ className }) => {
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

  return (
    <Card className={`bg-white/80 backdrop-blur-sm border-0 shadow-lg ${className || ""}`}>
      <CardContent className="p-6">
        <Tabs defaultValue="my-posts" className="w-full">
          <TabsList className="grid w-full grid-cols-4 bg-gray-100 h-12 md:h-10 p-1">
            <TabsTrigger
              value="my-posts"
              className="flex items-center justify-center h-full px-2 py-2 text-xs"
            >
              <span className="hidden sm:inline">작성한 글</span>
              <span className="sm:hidden">글</span>
            </TabsTrigger>
            <TabsTrigger
              value="my-comments"
              className="flex items-center justify-center h-full px-2 py-2 text-xs"
            >
              <span className="hidden sm:inline">작성한 댓글</span>
              <span className="sm:hidden">댓글</span>
            </TabsTrigger>
            <TabsTrigger
              value="liked-posts"
              className="flex items-center justify-center h-full px-2 py-2 text-xs"
            >
              <span className="hidden sm:inline">추천한 글</span>
              <span className="sm:hidden">추천글</span>
            </TabsTrigger>
            <TabsTrigger
              value="liked-comments"
              className="flex items-center justify-center h-full px-2 py-2 text-xs"
            >
              <span className="hidden sm:inline">추천한 댓글</span>
              <span className="sm:hidden">추천댓글</span>
            </TabsTrigger>
          </TabsList>
          <TabsContent value="my-posts">
            <ActivityTabContent fetchData={fetchMyPosts} contentType="posts" />
          </TabsContent>
          <TabsContent value="my-comments">
            <ActivityTabContent fetchData={fetchMyComments} contentType="comments" />
          </TabsContent>
          <TabsContent value="liked-posts">
            <ActivityTabContent fetchData={fetchLikedPosts} contentType="liked-posts" />
          </TabsContent>
          <TabsContent value="liked-comments">
            <ActivityTabContent fetchData={fetchLikedComments} contentType="liked-comments" />
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
};
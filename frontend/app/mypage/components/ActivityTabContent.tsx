import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { EmptyState } from "@/components";
import Link from "next/link";
import {
  Heart,
  MessageCircle,
  Eye,
  Clock,
  TrendingUp,
  Star,
  Calendar,
  ThumbsUp,
  FileText,
  User,
  Flame,
  Award,
  Pin,
  AlertTriangle,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { SimplePost, SimpleComment } from "@/lib/api";
import { formatKoreanDate } from "@/lib/date-utils";

interface ActivityTabContentProps {
  fetchData: (page?: number, size?: number) => Promise<{
    content: any[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
  }>;
  contentType: "posts" | "comments" | "liked-posts" | "liked-comments";
}

// 상대 시간 계산 함수
const getRelativeTime = (dateString: string): string => {
  const now = new Date();
  const date = new Date(dateString);
  const diffInMs = now.getTime() - date.getTime();
  const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
  const diffInHours = Math.floor(diffInMinutes / 60);
  const diffInDays = Math.floor(diffInHours / 24);

  if (diffInMinutes < 1) return "방금 전";
  if (diffInMinutes < 60) return `${diffInMinutes}분 전`;
  if (diffInHours < 24) return `${diffInHours}시간 전`;
  if (diffInDays < 7) return `${diffInDays}일 전`;
  return formatKoreanDate(dateString);
};

// 인기도 배지 컴포넌트
const PopularityBadge = ({ postCacheFlag }: { postCacheFlag?: string }) => {
  if (!postCacheFlag) return null;

  const badges = {
    REALTIME: {
      text: "실시간 인기",
      icon: <Flame className="w-3 h-3 mr-1" />,
      className: "bg-gradient-to-r from-red-500 to-orange-500 text-white",
    },
    WEEKLY: {
      text: "주간 인기",
      icon: <TrendingUp className="w-3 h-3 mr-1" />,
      className: "bg-gradient-to-r from-blue-500 to-purple-500 text-white",
    },
    LEGEND: {
      text: "레전드",
      icon: <Award className="w-3 h-3 mr-1" />,
      className: "bg-gradient-to-r from-yellow-500 to-orange-500 text-white",
    },
  };

  const badge = badges[postCacheFlag as keyof typeof badges];
  if (!badge) return null;

  return (
    <Badge
      className={`${badge.className} flex items-center text-xs font-medium`}
    >
      {badge.icon}
      {badge.text}
    </Badge>
  );
};

// 게시글 카드 컴포넌트
const PostCard = ({
  post,
  contentType,
}: {
  post: SimplePost;
  contentType: string;
}) => {
  const isLiked = contentType.includes("liked");

  return (
    <Card className="group bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-[1.02]">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3 flex-1">
            <div className="w-10 h-10 bg-gradient-to-r from-pink-500 to-purple-600 rounded-lg flex items-center justify-center">
              <FileText className="w-5 h-5 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center space-x-2 mb-1">
                <User className="w-4 h-4 text-gray-500" />
                <span className="text-base md:text-sm font-medium text-gray-700">
                  {post.userName}
                </span>
                {post.isNotice && (
                  <Badge className="bg-red-100 text-red-700 border-red-200">
                    <Pin className="w-3 h-3 mr-1" />
                    공지
                  </Badge>
                )}
                <PopularityBadge postCacheFlag={post.postCacheFlag} />
              </div>
              <div className="flex items-center space-x-2 text-sm md:text-xs text-gray-500">
                <Clock className="w-4 h-4 md:w-3 md:h-3" />
                <span>{getRelativeTime(post.createdAt)}</span>
              </div>
            </div>
          </div>
          {isLiked && <Heart className="w-5 h-5 text-red-500 fill-current" />}
        </div>
      </CardHeader>

      <CardContent className="pt-0">
        <Link href={`/board/post/${post.id}`}>
          <div className="cursor-pointer group">
            <h3 className="font-bold text-lg text-gray-800 group-hover:text-purple-600 transition-colors mb-3 line-clamp-2">
              {post.title}
            </h3>

            {/* 통계 정보 */}
            <div className="grid grid-cols-2 gap-4 mt-4 p-3 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-2">
                <Eye className="w-4 h-4 text-blue-500" />
                <div className="text-base md:text-sm">
                  <span className="text-gray-600">조회수</span>
                  <span className="font-semibold text-gray-800 ml-1">
                    {post.viewCount.toLocaleString()}
                  </span>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <Heart className="w-4 h-4 text-red-500" />
                <div className="text-base md:text-sm">
                  <span className="text-gray-600">추천</span>
                  <span className="font-semibold text-gray-800 ml-1">
                    {post.likeCount.toLocaleString()}
                  </span>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <MessageCircle className="w-4 h-4 text-green-500" />
                <div className="text-base md:text-sm">
                  <span className="text-gray-600">댓글</span>
                  <span className="font-semibold text-gray-800 ml-1">
                    {post.commentCount.toLocaleString()}
                  </span>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <Calendar className="w-4 h-4 text-purple-500" />
                <div className="text-base md:text-sm">
                  <span className="text-gray-600">작성일</span>
                  <span className="font-semibold text-gray-800 ml-1">
                    {formatKoreanDate(post.createdAt)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </Link>
      </CardContent>
    </Card>
  );
};

// 댓글 카드 컴포넌트
const CommentCard = ({
  comment,
  contentType,
}: {
  comment: SimpleComment;
  contentType: string;
}) => {
  const isLiked = contentType.includes("liked");

  return (
    <Card className="group bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-[1.02]">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3 flex-1">
            <div className="w-10 h-10 bg-gradient-to-r from-green-500 to-teal-600 rounded-lg flex items-center justify-center">
              <MessageCircle className="w-5 h-5 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center space-x-2 mb-1">
                <User className="w-4 h-4 text-gray-500" />
                <span className="text-base md:text-sm font-medium text-gray-700">
                  {comment.userName}
                </span>
              </div>
              <div className="flex items-center space-x-2 text-sm md:text-xs text-gray-500">
                <Clock className="w-4 h-4 md:w-3 md:h-3" />
                <span>{getRelativeTime(comment.createdAt)}</span>
              </div>
            </div>
          </div>
          {isLiked && (
            <ThumbsUp className="w-5 h-5 text-blue-500 fill-current" />
          )}
        </div>
      </CardHeader>

      <CardContent className="pt-0">
        <Link href={`/board/post/${comment.postId}#comment-${comment.id}`}>
          <div className="cursor-pointer group">
            <div className="p-3 bg-gray-50 rounded-lg mb-3">
              <p className="text-gray-800 group-hover:text-purple-600 transition-colors line-clamp-3">
                {comment.content}
              </p>
            </div>

            {/* 통계 정보 */}
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <div className="flex items-center space-x-2">
                  <ThumbsUp className="w-4 h-4 text-blue-500" />
                  <span className="text-base md:text-sm font-semibold text-gray-800">
                    {comment.likeCount.toLocaleString()}
                  </span>
                  <span className="text-sm md:text-xs text-gray-600">추천</span>
                </div>

                {comment.userLike && (
                  <Badge className="bg-blue-100 text-blue-700 border-blue-200 text-sm md:text-xs">
                    내가 추천한 댓글
                  </Badge>
                )}
              </div>

              <div className="text-sm md:text-xs text-gray-500">
                게시글 #{comment.postId}
              </div>
            </div>
          </div>
        </Link>
      </CardContent>
    </Card>
  );
};



export const ActivityTabContent: React.FC<ActivityTabContentProps> = ({
  fetchData,
  contentType,
}) => {
  const [items, setItems] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  const loadData = async (page = 0, append = false) => {
    try {
      if (!append) {
        setIsLoading(true);
      } else {
        setIsLoadingMore(true);
      }
      setError(null);
      
      const result = await fetchData(page, 10);
      
      if (append) {
        setItems(prev => [...prev, ...result.content]);
      } else {
        setItems(result.content);
      }
      
      setCurrentPage(result.currentPage);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (err) {
      console.error("Failed to fetch activity data:", err);
      setError("데이터를 불러오는 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  };

  useEffect(() => {
    loadData(0);
  }, [fetchData]);

  const handleLoadMore = () => {
    if (currentPage < totalPages - 1) {
      loadData(currentPage + 1, true);
    }
  };

  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      loadData(page);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-16">
        <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
          <Star className="w-6 h-6 text-white animate-pulse" />
        </div>
        <p className="text-gray-600">데이터를 불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <Alert className="border-red-200 bg-red-50 mt-6">
        <AlertTriangle className="h-4 w-4 text-red-600" />
        <AlertDescription className="text-red-800">
          <div className="flex items-center justify-between">
            <span>{error}</span>
            <Button
              onClick={() => loadData(currentPage)}
              variant="outline"
              size="sm"
              className="ml-4"
            >
              <RefreshCw className="w-4 h-4 mr-2" />
              다시 시도
            </Button>
          </div>
        </AlertDescription>
      </Alert>
    );
  }

  if (items.length === 0) {
    return <EmptyState type={contentType as "posts" | "comments" | "liked-posts" | "liked-comments"} />;
  }

  return (
    <div className="space-y-6 mt-6">
      {/* 헤더 정보 */}
      <div className="flex items-center justify-between pb-4 border-b border-gray-200">
        <div className="flex items-center space-x-2">
          <span className="text-lg font-semibold text-gray-800">
            총 {totalElements.toLocaleString()}개
          </span>
          {contentType.includes("posts") && (
            <Badge className="bg-blue-100 text-blue-700 border-blue-200">
              게시글
            </Badge>
          )}
          {contentType.includes("comments") && (
            <Badge className="bg-green-100 text-green-700 border-green-200">
              댓글
            </Badge>
          )}
        </div>
        {totalPages > 1 && (
          <div className="text-sm text-gray-600">
            페이지 {currentPage + 1} / {totalPages}
          </div>
        )}
      </div>

      {/* 아이템 목록 */}
      <div className="grid gap-6">
        {items.map((item) => {
          // 게시글인지 댓글인지 판단
          const isPost = "title" in item && "viewCount" in item;

          return isPost ? (
            <PostCard
              key={item.id}
              post={item as SimplePost}
              contentType={contentType}
            />
          ) : (
            <CommentCard
              key={item.id}
              comment={item as SimpleComment}
              contentType={contentType}
            />
          );
        })}
      </div>

      {/* 페이지네이션 */}
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

      {/* 더보기 버튼 (스크롤 모드) */}
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
              <>
                더보기 ({items.length} / {totalElements})
              </>
            )}
          </Button>
        </div>
      )}
    </div>
  );
};

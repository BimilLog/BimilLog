import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
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
} from "lucide-react";
import { SimplePost, SimpleComment } from "@/lib/api";

interface ActivityTabContentProps {
  fetchData: () => Promise<any[]>;
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
  return date.toLocaleDateString("ko-KR");
};

// 인기도 배지 컴포넌트
const PopularityBadge = ({ popularFlag }: { popularFlag?: string }) => {
  if (!popularFlag) return null;

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

  const badge = badges[popularFlag as keyof typeof badges];
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
                {post._notice && (
                  <Badge className="bg-red-100 text-red-700 border-red-200">
                    <Pin className="w-3 h-3 mr-1" />
                    공지
                  </Badge>
                )}
                <PopularityBadge popularFlag={post.popularFlag} />
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
        <Link href={`/board/post/${post.postId}`}>
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
                    {post.views.toLocaleString()}
                  </span>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <Heart className="w-4 h-4 text-red-500" />
                <div className="text-base md:text-sm">
                  <span className="text-gray-600">추천</span>
                  <span className="font-semibold text-gray-800 ml-1">
                    {post.likes.toLocaleString()}
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
                    {new Date(post.createdAt).toLocaleDateString("ko-KR")}
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
                    {comment.likes.toLocaleString()}
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

// 빈 상태 컴포넌트
const EmptyState = ({ contentType }: { contentType: string }) => {
  const emptyStates = {
    posts: {
      icon: <FileText className="w-12 h-12 text-gray-400" />,
      title: "작성한 글이 없습니다",
      description: "첫 번째 글을 작성해보세요!",
    },
    comments: {
      icon: <MessageCircle className="w-12 h-12 text-gray-400" />,
      title: "작성한 댓글이 없습니다",
      description: "다른 사람의 글에 댓글을 달아보세요!",
    },
    "liked-posts": {
      icon: <Heart className="w-12 h-12 text-gray-400" />,
      title: "추천한 글이 없습니다",
      description: "마음에 드는 글에 추천을 눌러보세요!",
    },
    "liked-comments": {
      icon: <ThumbsUp className="w-12 h-12 text-gray-400" />,
      title: "추천한 댓글이 없습니다",
      description: "좋은 댓글에 추천을 눌러보세요!",
    },
  };

  const state = emptyStates[contentType as keyof typeof emptyStates];

  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mb-4">
        {state.icon}
      </div>
      <h3 className="text-lg font-semibold text-gray-800 mb-2">
        {state.title}
      </h3>
      <p className="text-gray-600 max-w-sm">{state.description}</p>
    </div>
  );
};

export const ActivityTabContent: React.FC<ActivityTabContentProps> = ({
  fetchData,
  contentType,
}) => {
  const [items, setItems] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchData()
      .then((data) => setItems(data))
      .catch((err) => console.error("Failed to fetch activity data:", err))
      .finally(() => setIsLoading(false));
  }, [fetchData]);

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

  if (items.length === 0) {
    return <EmptyState contentType={contentType} />;
  }

  return (
    <div className="space-y-6 mt-6">
      {/* 헤더 정보 */}
      <div className="flex items-center justify-between pb-4 border-b border-gray-200">
        <div className="flex items-center space-x-2">
          <span className="text-lg font-semibold text-gray-800">
            총 {items.length.toLocaleString()}개
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

        <div className="text-base md:text-sm text-gray-500">
          최근 활동 순으로 표시
        </div>
      </div>

      {/* 아이템 목록 */}
      <div className="grid gap-6">
        {items.map((item) => {
          // 게시글인지 댓글인지 판단
          const isPost = "title" in item && "views" in item;

          return isPost ? (
            <PostCard
              key={item.postId}
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

      {/* 푸터 정보 */}
      {items.length > 5 && (
        <div className="text-center pt-6 border-t border-gray-200">
          <p className="text-base md:text-sm text-gray-500">
            더 많은 내용을 보시려면 각 항목을 클릭해주세요
          </p>
        </div>
      )}
    </div>
  );
};

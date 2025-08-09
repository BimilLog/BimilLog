import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CardHeader, CardTitle } from "@/components/ui/card";
import {
  Eye,
  ThumbsUp,
  MessageSquare,
  Lock,
  Edit,
  Trash2,
  User,
  ExternalLink,
} from "lucide-react";
import { Post } from "@/lib/api";
import Link from "next/link";
import { KakaoShareButton } from "@/components/atoms/kakao-share-button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/molecules/popover";

interface PostHeaderProps {
  post: Post;
  commentCount: number;
  canModify: () => boolean;
  onDeleteClick: () => void;
}

export const PostHeader: React.FC<PostHeaderProps> = ({
  post,
  commentCount,
  canModify,
  onDeleteClick,
}) => {
  // 시간 포맷팅 함수 (T와 Z 제거, 분까지만 표시)
  const formatDateTime = (dateTimeString: string) => {
    try {
      const date = new Date(dateTimeString);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      const hours = String(date.getHours()).padStart(2, "0");
      const minutes = String(date.getMinutes()).padStart(2, "0");

      return `${year}-${month}-${day} ${hours}:${minutes}`;
    } catch {
      return dateTimeString; // 포맷팅 실패 시 원본 반환
    }
  };

  return (
    <CardHeader className="border-b p-4 md:p-6">
      {/* 제목과 배지 */}
      <div className="mb-4">
        <div className="flex items-center flex-wrap gap-2 mb-3">
          {post.password && <Lock className="w-4 h-4 text-red-500" />}
          {post.notice && (
            <Badge className="bg-red-500 text-white text-xs">공지</Badge>
          )}
          {post.postCacheFlag && (
            <Badge className="bg-orange-500 text-white text-xs">
              {post.postCacheFlag === "REALTIME"
                ? "실시간"
                : post.postCacheFlag === "WEEKLY"
                ? "주간"
                : "레전드"}
            </Badge>
          )}
        </div>
        <CardTitle className="text-xl md:text-2xl font-bold text-gray-800 leading-tight">
          {post.title}
        </CardTitle>
      </div>

      {/* 작성자 정보 - 모바일 최적화 */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex flex-col space-y-2">
          {/* 작성자와 시간 */}
          <div className="flex items-center space-x-3 text-sm text-gray-600">
            <div className="flex items-center space-x-2 min-w-0">
              {post.userName && post.userName !== "익명" ? (
                <Popover>
                  <PopoverTrigger asChild>
                    <button className="truncate max-w-[120px] md:max-w-none hover:text-purple-600 hover:underline transition-colors cursor-pointer inline-flex items-center space-x-1">
                      <User className="w-3 h-3" />
                      <span>{post.userName}</span>
                    </button>
                  </PopoverTrigger>
                  <PopoverContent className="w-56 p-3" align="start">
                    <div className="flex flex-col space-y-2">
                      <div className="flex items-center space-x-2">
                        <User className="w-4 h-4" />
                        <span className="font-medium">{post.userName}</span>
                      </div>
                      <Link
                        href={`/rolling-paper/${encodeURIComponent(
                          post.userName
                        )}`}
                      >
                        <Button size="sm" className="w-full justify-start">
                          <ExternalLink className="w-4 h-4 mr-2" />
                          롤링페이퍼 보기
                        </Button>
                      </Link>
                    </div>
                  </PopoverContent>
                </Popover>
              ) : (
                <span className="truncate max-w-[120px] md:max-w-none text-gray-500">
                  {post.userName || "익명"}
                </span>
              )}
            </div>
            <span className="text-xs text-gray-500 whitespace-nowrap">
              {formatDateTime(post.createdAt)}
            </span>
          </div>

          {/* 통계 정보 */}
          <div className="flex items-center space-x-4 text-sm text-gray-600">
            <div className="flex items-center space-x-1">
              <Eye className="w-4 h-4" />
              <span>{post.views}</span>
            </div>
            <div className="flex items-center space-x-1">
              <ThumbsUp className="w-4 h-4" />
              <span>{post.likes}</span>
            </div>
            <div className="flex items-center space-x-1">
              <MessageSquare className="w-4 h-4" />
              <span>{commentCount}</span>
            </div>
          </div>
        </div>

        {/* 버튼 영역 - 모바일 최적화 */}
        <div className="flex flex-col sm:flex-row gap-2 sm:items-center">
          {/* 카카오톡 공유 버튼 */}
          <KakaoShareButton
            type="post"
            postId={post.postId}
            title={post.title}
            author={post.userName || "익명"}
            content={post.content}
            likes={post.likes}
            variant="outline"
            size="sm"
            className="w-full sm:w-auto"
          />

          {canModify() && (
            <div className="flex gap-2">
              <Link
                href={`/board/post/${post.postId}/edit`}
                className="flex-1 sm:flex-initial"
              >
                <Button
                  size="sm"
                  variant="outline"
                  className="w-full sm:w-auto"
                >
                  <Edit className="w-4 h-4 sm:mr-1" />
                  <span className="hidden sm:inline">수정</span>
                </Button>
              </Link>
              <Button
                size="sm"
                variant="destructive"
                onClick={onDeleteClick}
                className="flex-1 sm:flex-initial"
              >
                <Trash2 className="w-4 h-4 sm:mr-1" />
                <span className="hidden sm:inline">삭제</span>
              </Button>
            </div>
          )}
        </div>
      </div>
    </CardHeader>
  );
};

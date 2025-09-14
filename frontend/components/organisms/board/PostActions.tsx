import { memo } from "react";
import { Edit, Trash2, Heart, MessageCircle, Eye } from "lucide-react";
import { Button } from "@/components";
import type { Post } from "@/lib/api";

interface PostActionsProps {
  post: Post;
  commentCount: number;
  canModify: boolean;
  isAuthenticated: boolean;
  onDeletePost: () => void;
  onLike: () => void;
}

/**
 * 게시글 액션 컴포넌트 (수정/삭제/추천)
 * PostDetailClient에서 분리된 액션 관련 컴포넌트
 */
const PostActions = memo(({
  post,
  commentCount,
  canModify,
  isAuthenticated,
  onDeletePost,
  onLike,
}: PostActionsProps) => {
  return (
    <div className="flex items-center justify-between px-6 pb-6">
      <div className="flex items-center space-x-4">
        {/* 추천 버튼 - 로그인한 사용자만 사용 가능 */}
        <Button
          variant="ghost"
          size="sm"
          onClick={onLike}
          disabled={!isAuthenticated} // 비로그인 사용자는 추천 불가
          className="flex items-center space-x-1 text-pink-600 hover:text-pink-700 hover:bg-pink-50"
        >
          <Heart className="w-4 h-4" />
          <span>{post.likeCount || 0}</span>
        </Button>

        {/* 댓글 수 표시 */}
        <div className="flex items-center space-x-1 text-gray-500">
          <MessageCircle className="w-4 h-4" />
          <span className="text-sm">{commentCount}</span>
        </div>

        {/* 조회수 표시 - 게시글 들어갈 때마다 자동 증가 */}
        <div className="flex items-center space-x-1 text-gray-500">
          <Eye className="w-4 h-4" />
          <span className="text-sm">{post.viewCount || 0}</span>
        </div>
      </div>

      {/* 수정/삭제 버튼 - 작성자 본인이거나 관리자인 경우만 표시 */}
      {canModify && (
        <div className="flex items-center space-x-2">
          <Button
            variant="outline"
            size="sm"
            asChild
            className="flex items-center space-x-1"
          >
            {/* 수정 페이지로 이돔 - 기존 내용을 불러와서 수정 가능 */}
            <a href={`/board/post/${post.id}/edit`}>
              <Edit className="w-4 h-4" />
              <span>수정</span>
            </a>
          </Button>
          
          <Button
            variant="outline"
            size="sm"
            onClick={onDeletePost} // 비밀번호 모달을 열어 비밀번호 확인 후 삭제 진행
            className="flex items-center space-x-1 text-red-600 hover:text-red-700 hover:bg-red-50 border-red-200"
          >
            <Trash2 className="w-4 h-4" />
            <span>삭제</span>
          </Button>
        </div>
      )}
    </div>
  );
});

PostActions.displayName = "PostActions";

export { PostActions };
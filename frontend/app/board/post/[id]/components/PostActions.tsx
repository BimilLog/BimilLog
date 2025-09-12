import React, { memo } from "react";
import { Edit, Trash2, Heart, MessageCircle, Eye } from "lucide-react";
import { Button } from "@/components";
import type { Post, Comment } from "@/lib/api";

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
        {/* 추천 버튼 */}
        <Button
          variant="ghost"
          size="sm"
          onClick={onLike}
          disabled={!isAuthenticated}
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

        {/* 조회수 표시 */}
        <div className="flex items-center space-x-1 text-gray-500">
          <Eye className="w-4 h-4" />
          <span className="text-sm">{post.viewCount || 0}</span>
        </div>
      </div>

      {/* 수정/삭제 버튼 */}
      {canModify && (
        <div className="flex items-center space-x-2">
          <Button
            variant="outline"
            size="sm"
            asChild
            className="flex items-center space-x-1"
          >
            <a href={`/board/post/${post.id}/edit`}>
              <Edit className="w-4 h-4" />
              <span>수정</span>
            </a>
          </Button>
          
          <Button
            variant="outline"
            size="sm"
            onClick={onDeletePost}
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
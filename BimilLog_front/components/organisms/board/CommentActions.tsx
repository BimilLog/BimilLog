"use client";

import React from "react";
import { Button } from "@/components";
import { Button as FlowbiteButton } from "flowbite-react";
import { ThumbsUp, Reply, MoreHorizontal } from "lucide-react";
import { Comment } from "@/lib/api";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/molecules/dropdown-menu";

interface CommentActionsProps {
  comment: Comment;
  isAuthenticated: boolean;
  canModify: boolean;
  isMyComment: boolean;
  onLikeComment: (comment: Comment) => void;
  onReplyTo: (comment: Comment) => void;
  onEditComment: (comment: Comment) => void;
  onDeleteComment: (comment: Comment) => void;
  onReportClick: () => void;
}

export const CommentActions: React.FC<CommentActionsProps> = React.memo(({
  comment,
  isAuthenticated,
  canModify,
  isMyComment,
  onLikeComment,
  onReplyTo,
  onEditComment,
  onDeleteComment,
  onReportClick,
}) => {
  return (
    <div className="mt-3 flex items-center gap-2 flex-wrap">
      {/* 추천 버튼: 비로그인 사용자에게는 비활성화 + 툴팁 표시 */}
      <FlowbiteButton
        size="xs"
        color={comment.userLike ? "blue" : "light"}
        onClick={() => onLikeComment(comment)}
        disabled={!isAuthenticated}
        title={!isAuthenticated ? "로그인 후 추천할 수 있습니다" : undefined}
        className={!isAuthenticated ? "cursor-not-allowed opacity-60" : ""}
      >
        <ThumbsUp className={`w-4 h-4 mr-2 ${comment.userLike ? "fill-current" : ""}`} />
        추천 {comment.likeCount}
      </FlowbiteButton>

      {/* 답글 버튼: 모바일에서도 항상 표시하여 접근성 향상 */}
      <FlowbiteButton
        size="xs"
        className="bg-gradient-to-r from-purple-500 to-pink-500 text-white hover:bg-gradient-to-l"
        onClick={() => onReplyTo(comment)}
      >
        <Reply className="w-4 h-4 mr-2" />
        답글
      </FlowbiteButton>

      {/* 신고 버튼: 다른 사람의 댓글인 경우만 표시 */}
      {!isMyComment && !comment.deleted && (
        <FlowbiteButton
          size="xs"
          color="red"
          onClick={onReportClick}
        >
          신고
        </FlowbiteButton>
      )}

      {/* 수정/삭제 버튼을 위한 드롭다운 */}
      {canModify && !comment.deleted && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              size="sm"
              variant="ghost"
              aria-label="댓글 옵션"
              className="text-xs px-2 py-1 h-7 text-brand-secondary hover:text-brand-primary"
            >
              <MoreHorizontal className="w-3 h-3" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-24">
            <DropdownMenuItem
              onClick={() => onEditComment(comment)}
              className="cursor-pointer"
            >
              수정
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() => onDeleteComment(comment)}
              className="text-red-600 hover:text-red-700 cursor-pointer"
            >
              삭제
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      )}
    </div>
  );
});

CommentActions.displayName = "CommentActions";

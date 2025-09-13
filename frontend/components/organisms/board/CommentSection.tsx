import React, { memo } from "react";
import type { Comment } from "@/lib/api";
import { CommentForm } from "./CommentForm";
import { PopularComments } from "./PopularComments";
import { CommentList } from "./CommentList";

interface CommentSectionProps {
  postId: number;
  comments: (Comment & { replies?: Comment[] })[];
  popularComments: Comment[];
  commentCount: number;
  isAuthenticated: boolean;

  // CommentForm props
  isSubmittingComment: boolean;
  onSubmitComment: (comment: string, password: string) => void;

  // CommentList props
  editingComment: Comment | null;
  editContent: string;
  editPassword: string;
  replyingTo: Comment | null;
  replyContent: string;
  replyPassword: string;
  isSubmittingReply: boolean;

  onEditComment: (comment: Comment) => void;
  onUpdateComment: () => void;
  onCancelEdit: () => void;
  onDeleteComment: (comment: Comment) => void;
  onReplyTo: (comment: Comment) => void;
  onReplySubmit: () => void;
  onCancelReply: () => void;
  onLikeComment: (comment: Comment) => void;

  setEditContent: (content: string) => void;
  setEditPassword: (password: string) => void;
  setReplyContent: (content: string) => void;
  setReplyPassword: (password: string) => void;

  // Utility functions
  isMyComment: (comment: Comment) => boolean;
  canModifyComment: (comment: Comment) => boolean;
  onCommentClick: (commentId: number) => void;
}

/**
 * 댓글 섹션 통합 컴포넌트
 * PostDetailClient에서 분리된 댓글 관련 모든 기능을 통합
 */
const CommentSection = memo(({
  postId,
  comments,
  popularComments,
  commentCount,
  isAuthenticated,

  // CommentForm
  isSubmittingComment,
  onSubmitComment,

  // CommentList
  editingComment,
  editContent,
  editPassword,
  replyingTo,
  replyContent,
  replyPassword,
  isSubmittingReply,

  onEditComment,
  onUpdateComment,
  onCancelEdit,
  onDeleteComment,
  onReplyTo,
  onReplySubmit,
  onCancelReply,
  onLikeComment,

  setEditContent,
  setEditPassword,
  setReplyContent,
  setReplyPassword,

  // Utilities
  isMyComment,
  canModifyComment,
  onCommentClick,
}: CommentSectionProps) => {
  return (
    <div className="space-y-6">
      {/* 댓글 작성 폼 */}
      <CommentForm
        isAuthenticated={isAuthenticated}
        isSubmittingComment={isSubmittingComment}
        onSubmit={onSubmitComment}
      />

      {/* 인기 댓글 */}
      {popularComments.length > 0 && (
        <PopularComments
          comments={popularComments}
          onLikeComment={onLikeComment}
          onReplyTo={onReplyTo}
          onCommentClick={onCommentClick}
        />
      )}

      {/* 댓글 목록 */}
      <CommentList
        comments={comments}
        commentCount={commentCount}
        postId={postId}
        editingComment={editingComment}
        editContent={editContent}
        editPassword={editPassword}
        replyingTo={replyingTo}
        replyContent={replyContent}
        replyPassword={replyPassword}
        isAuthenticated={isAuthenticated}
        isSubmittingReply={isSubmittingReply}
        onEditComment={onEditComment}
        onUpdateComment={onUpdateComment}
        onCancelEdit={onCancelEdit}
        onDeleteComment={onDeleteComment}
        onReplyTo={onReplyTo}
        onReplySubmit={onReplySubmit}
        onCancelReply={onCancelReply}
        setEditContent={setEditContent}
        setEditPassword={setEditPassword}
        setReplyContent={setReplyContent}
        setReplyPassword={setReplyPassword}
        isMyComment={isMyComment}
        onLikeComment={onLikeComment}
        canModifyComment={canModifyComment}
      />
    </div>
  );
});

CommentSection.displayName = "CommentSection";

export { CommentSection };
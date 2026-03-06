import { memo } from "react";
import type { Comment } from "@/lib/api";
import { CommentForm } from "./CommentForm";
import { PopularComments } from "./PopularComments";
import { CommentList } from "./CommentList";
import type { CommentHandlers, CommentEditState, CommentReplyState } from "@/hooks/features/post/useCommentInteraction";

interface CommentSectionProps {
  postId: number;
  comments: (Comment & { replies?: Comment[] })[];
  popularComments: Comment[];
  commentCount: number;
  rootCommentCount: number;
  isAuthenticated: boolean;

  // Pagination props
  hasMoreComments: boolean;
  isLoadingMore: boolean;

  // CommentForm props
  isSubmittingComment: boolean;
  onSubmitComment: (comment: string, password: string) => void;

  // 그룹화된 props
  handlers: CommentHandlers;
  editState: CommentEditState;
  replyState: CommentReplyState;

  // CommentList 개별 props
  isSubmittingReply: boolean;
  isUpdatingComment: boolean;
  onLoadMore: () => void;

  // Utility functions
  onCommentClick: (commentId: number) => void;
}

/**
 * 댓글 섹션 통합 컴포넌트
 * PostDetailClient에서 분리된 댓글 관련 모든 기능을 통합
 * 댓글 작성 폼 + 인기 댓글 + 전체 댓글 목록을 순차적으로 표시
 */
const CommentSection = memo(({
  postId,
  comments,
  popularComments,
  commentCount,
  rootCommentCount,
  isAuthenticated,

  // Pagination
  hasMoreComments,
  isLoadingMore,

  // CommentForm
  isSubmittingComment,
  onSubmitComment,

  // 그룹화된 props
  handlers,
  editState,
  replyState,

  // CommentList 개별 props
  isSubmittingReply,
  isUpdatingComment,
  onLoadMore,

  // Utilities
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

      {/* 인기 댓글 - 좋아요 3개 이상인 댓글들만 별도 표시 */}
      {popularComments.length > 0 && (
        <PopularComments
          comments={popularComments}
          onLikeComment={handlers.onLikeComment}
          onReplyTo={handlers.onReplyTo}
          onCommentClick={onCommentClick} // 인기 댓글 클릭 시 원본 댓글로 스크롤 이동
        />
      )}

      {/* 댓글 목록 - 계층 구조로 표시되는 전체 댓글 */}
      <CommentList
        comments={comments}
        commentCount={commentCount}
        rootCommentCount={rootCommentCount}
        postId={postId}
        isAuthenticated={isAuthenticated}
        isSubmittingReply={isSubmittingReply}
        isUpdatingComment={isUpdatingComment}
        hasMoreComments={hasMoreComments}
        isLoadingMore={isLoadingMore}
        onLoadMore={onLoadMore}
        handlers={handlers}
        editState={editState}
        replyState={replyState}
      />
    </div>
  );
});

CommentSection.displayName = "CommentSection";

export { CommentSection };
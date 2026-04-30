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
  /** 현재 로드된 댓글 총 개수 (재귀, 답글 포함) — CardTitle 보조 정보. */
  commentCount: number;
  /** 루트 댓글 개수 (현재 페이지 기준). */
  rootCommentCount: number;
  /** 서버가 알려준 게시글의 총 댓글 수 (라운드 8 B-8-010). */
  totalCommentCount: number;
  isAuthenticated: boolean;

  // Pagination props
  hasMoreComments: boolean;
  isLoadingMore: boolean;

  // CommentForm props
  isSubmittingComment: boolean;
  onSubmitComment: (
    comment: string,
    password: string,
    callbacks?: { onSuccess?: () => void; onError?: (error: string) => void }
  ) => void;

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
  totalCommentCount,
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
        totalCommentCount={totalCommentCount}
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
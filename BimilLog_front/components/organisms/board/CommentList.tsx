import React from "react";
import { Card, CardContent, CardHeader, CardTitle, Button, Spinner } from "@/components";
import { MessageSquare, ChevronDown } from "lucide-react";
import { Comment } from "@/lib/api";
import { CommentItem } from "./CommentItem";
import type { CommentHandlers, CommentEditState, CommentReplyState } from "@/hooks/features/post/useCommentInteraction";

interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

interface CommentListProps {
  comments: CommentWithReplies[];
  /** 현재 로드된 댓글 수 (재귀 합산). */
  commentCount: number;
  /** 루트 댓글 수 (페이지 기준). */
  rootCommentCount: number;
  /** 라운드 8 B-8-010: 서버가 보낸 게시글 총 댓글 수. */
  totalCommentCount: number;
  postId: number;
  isAuthenticated: boolean;
  isSubmittingReply: boolean;
  isUpdatingComment: boolean;
  hasMoreComments: boolean;
  isLoadingMore: boolean;
  onLoadMore: () => void;
  handlers: CommentHandlers;
  editState: CommentEditState;
  replyState: CommentReplyState;
}

export const CommentList = React.memo<CommentListProps>(({
  comments,
  commentCount,
  rootCommentCount,
  totalCommentCount,
  postId,
  isAuthenticated,
  isSubmittingReply,
  isUpdatingComment,
  hasMoreComments,
  isLoadingMore,
  onLoadMore,
  handlers,
  editState,
  replyState,
}) => {
  const replyCount = Math.max(0, commentCount - rootCommentCount);
  const showLoadedHint = hasMoreComments && totalCommentCount > commentCount;

  return (
    <Card variant="elevated">
      <CardHeader>
        <CardTitle className="flex items-center space-x-2 break-keep">
          <MessageSquare className="w-5 h-5 stroke-blue-600 fill-blue-100" aria-hidden="true" />
          <span>
            {/* 라운드 8 B-8-010: 서버 총합을 헤더에 노출 */}
            댓글 {totalCommentCount}개
            {replyCount > 0 && (
              <span className="text-brand-secondary"> (답글 {replyCount}개)</span>
            )}
            {showLoadedHint && (
              <span className="ml-2 text-xs text-brand-muted" aria-live="polite">
                · 현재 {commentCount}건 보는 중
              </span>
            )}
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {comments.length > 0 ? (
          <>
            <div className="space-y-4">
              {comments.map((comment) => (
                <CommentItem
                  key={comment.id}
                  comment={comment}
                  depth={0}
                  postId={postId}
                  isAuthenticated={isAuthenticated}
                  isSubmittingReply={isSubmittingReply}
                  isUpdatingComment={isUpdatingComment}
                  handlers={handlers}
                  editState={editState}
                  replyState={replyState}
                />
              ))}
            </div>
            {hasMoreComments && (
              <div className="mt-6 flex justify-center">
                <Button
                  onClick={onLoadMore}
                  disabled={isLoadingMore}
                  variant="outline"
                  className="w-full sm:w-auto"
                >
                  {isLoadingMore ? (
                    <>
                      <Spinner size="sm" className="mr-2" />
                      로딩 중...
                    </>
                  ) : (
                    <>
                      <ChevronDown className="w-4 h-4 mr-2" aria-hidden="true" />
                      댓글 더보기
                    </>
                  )}
                </Button>
              </div>
            )}
          </>
        ) : (
          <p className="text-brand-secondary text-center break-keep">
            첫 번째 댓글을 작성해보세요!
          </p>
        )}
      </CardContent>
    </Card>
  );
});

CommentList.displayName = "CommentList";

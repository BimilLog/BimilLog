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
  commentCount: number;
  rootCommentCount: number;
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
  const replyCount = commentCount - rootCommentCount;

  return (
    <Card variant="elevated">
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <MessageSquare className="w-5 h-5 stroke-blue-600 fill-blue-100" />
          <span>
            댓글 {rootCommentCount}개
            {replyCount > 0 && <span className="text-brand-secondary"> (답글 {replyCount}개)</span>}
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
                      <ChevronDown className="w-4 h-4 mr-2" />
                      댓글 더보기
                    </>
                  )}
                </Button>
              </div>
            )}
          </>
        ) : (
          <p className="text-brand-secondary text-center">
            첫 번째 댓글을 작성해보세요!
          </p>
        )}
      </CardContent>
    </Card>
  );
});

CommentList.displayName = "CommentList";

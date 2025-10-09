import React from "react";
import { Card, CardContent, CardHeader, CardTitle, Button, Spinner } from "@/components";
import { MessageSquare, ChevronDown } from "lucide-react";
import { Comment } from "@/lib/api";
import { CommentItem } from "./CommentItem";

interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

interface CommentListProps {
  comments: CommentWithReplies[];
  commentCount: number;
  rootCommentCount: number;
  postId: number;
  editingComment: Comment | null;
  editContent: string;
  editPassword: string;
  replyingTo: Comment | null;
  replyContent: string;
  replyPassword: string;
  isAuthenticated: boolean;
  isSubmittingReply: boolean;
  isUpdatingComment: boolean;
  hasMoreComments: boolean;
  isLoadingMore: boolean;
  onEditComment: (comment: Comment) => void;
  onUpdateComment: () => void;
  onCancelEdit: () => void;
  onDeleteComment: (comment: Comment) => void;
  onReplyTo: (comment: Comment) => void;
  onReplySubmit: () => void;
  onCancelReply: () => void;
  onLoadMore: () => void;
  setEditContent: (content: string) => void;
  setEditPassword: (password: string) => void;
  setReplyContent: (content: string) => void;
  setReplyPassword: (password: string) => void;
  isMyComment: (comment: Comment) => boolean;
  onLikeComment: (comment: Comment) => void;
  canModifyComment: (comment: Comment) => boolean;
}

export const CommentList = React.memo<CommentListProps>(({
  comments,
  commentCount,
  rootCommentCount,
  postId,
  editingComment,
  editContent,
  editPassword,
  replyingTo,
  replyContent,
  replyPassword,
  isAuthenticated,
  isSubmittingReply,
  isUpdatingComment,
  hasMoreComments,
  isLoadingMore,
  onEditComment,
  onUpdateComment,
  onCancelEdit,
  onDeleteComment,
  onReplyTo,
  onReplySubmit,
  onCancelReply,
  onLoadMore,
  setEditContent,
  setEditPassword,
  setReplyContent,
  setReplyPassword,
  isMyComment,
  onLikeComment,
  canModifyComment,
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
                  editingComment={editingComment}
                  editContent={editContent}
                  editPassword={editPassword}
                  replyingTo={replyingTo}
                  replyContent={replyContent}
                  replyPassword={replyPassword}
                  isAuthenticated={isAuthenticated}
                  isSubmittingReply={isSubmittingReply}
                  isUpdatingComment={isUpdatingComment}
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

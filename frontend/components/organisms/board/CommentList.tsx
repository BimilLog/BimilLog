import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { MessageSquare } from "lucide-react";
import { Comment } from "@/lib/api";
import { CommentItem } from "./CommentItem";

interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

interface CommentListProps {
  comments: CommentWithReplies[];
  commentCount: number;
  postId: number;
  editingComment: Comment | null;
  editContent: string;
  editPassword: string;
  replyingTo: Comment | null;
  replyContent: string;
  replyPassword: string;
  isAuthenticated: boolean;
  isSubmittingReply: boolean;
  onEditComment: (comment: Comment) => void;
  onUpdateComment: () => void;
  onCancelEdit: () => void;
  onDeleteComment: (comment: Comment) => void;
  onReplyTo: (comment: Comment) => void;
  onReplySubmit: () => void;
  onCancelReply: () => void;
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
  postId,
  editingComment,
  editContent,
  editPassword,
  replyingTo,
  replyContent,
  replyPassword,
  isAuthenticated,
  isSubmittingReply,
  onEditComment,
  onUpdateComment,
  onCancelEdit,
  onDeleteComment,
  onReplyTo,
  onReplySubmit,
  onCancelReply,
  setEditContent,
  setEditPassword,
  setReplyContent,
  setReplyPassword,
  isMyComment,
  onLikeComment,
  canModifyComment,
}) => {
  return (
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <MessageSquare className="w-5 h-5" />
          <span>댓글 {commentCount}개</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {comments.length > 0 ? (
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
        ) : (
          <p className="text-gray-500 text-center">
            첫 번째 댓글을 작성해보세요!
          </p>
        )}
      </CardContent>
    </Card>
  );
});

CommentList.displayName = "CommentList";

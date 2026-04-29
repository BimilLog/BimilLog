import { memo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { MessageSquare } from "lucide-react";
import { Comment } from "@/lib/api";
import { PopularCommentItem } from "./PopularCommentItem";

interface PopularCommentsProps {
  comments?: Comment[];
  onLikeComment: (comment: Comment) => void;
  onReplyTo: (comment: Comment) => void;
  onCommentClick: (commentId: number) => void;
}

export const PopularComments: React.FC<PopularCommentsProps> = memo(({
  comments,
  onLikeComment,
  onReplyTo,
  onCommentClick,
}) => {
  if (!comments || comments.length === 0) {
    return null;
  }

  return (
    <Card className="bg-paper-aged border-stamp-red/30 shadow-brand-lg mb-6">
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-stamp-red">
          <MessageSquare className="w-5 h-5 stroke-stamp-red fill-paper-100" />
          <span className="text-base sm:text-lg font-display">인기 댓글</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {comments.map((comment) => (
          <PopularCommentItem
            key={comment.id}
            comment={comment}
            onLikeComment={onLikeComment}
            onReplyTo={onReplyTo}
            onCommentClick={onCommentClick}
          />
        ))}
      </CardContent>
    </Card>
  );
});

PopularComments.displayName = "PopularComments";

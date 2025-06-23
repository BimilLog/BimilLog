import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Send } from "lucide-react";

interface CommentFormProps {
  isAuthenticated: boolean;
  newComment: string;
  commentPassword: string;
  isSubmittingComment: boolean;
  onCommentChange: (comment: string) => void;
  onPasswordChange: (password: string) => void;
  onSubmit: () => void;
}

export const CommentForm: React.FC<CommentFormProps> = ({
  isAuthenticated,
  newComment,
  commentPassword,
  isSubmittingComment,
  onCommentChange,
  onPasswordChange,
  onSubmit,
}) => {
  return (
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg mb-8">
      <CardHeader>
        <CardTitle className="text-lg">댓글 작성</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {!isAuthenticated && (
          <div className="mb-4">
            <Input
              type="password"
              placeholder="비밀번호 (숫자만 입력)"
              value={commentPassword}
              onChange={(e) => onPasswordChange(e.target.value)}
            />
          </div>
        )}
        <div className="flex space-x-4">
          <Textarea
            placeholder="댓글을 입력하세요..."
            value={newComment}
            onChange={(e) => onCommentChange(e.target.value)}
          />
          <Button onClick={onSubmit} disabled={isSubmittingComment}>
            <Send className="w-4 h-4" />
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

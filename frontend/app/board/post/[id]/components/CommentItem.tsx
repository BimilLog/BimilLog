import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ThumbsUp, Reply, Flag } from "lucide-react";
import { Comment, userApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { ReportModal } from "@/components/ui/ReportModal";
import { useState } from "react";

interface CommentItemProps {
  comment: Comment & { replies?: Comment[] };
  depth: number;
  editingComment: Comment | null;
  editContent: string;
  editPassword: string;
  replyingTo: Comment | null;
  replyContent: string;
  replyPassword: string;
  isAuthenticated: boolean;
  isSubmittingReply: boolean;
  postId: number;
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

export const CommentItem: React.FC<CommentItemProps> = ({
  comment,
  depth,
  editingComment,
  editContent,
  editPassword,
  replyingTo,
  replyContent,
  replyPassword,
  isAuthenticated,
  isSubmittingReply,
  postId,
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
  const { user } = useAuth();
  const maxDepth = 3; // 최대 들여쓰기 레벨
  const actualDepth = Math.min(depth, maxDepth);
  const marginLeft = actualDepth * 24; // 24px씩 들여쓰기
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);

  const handleReportSubmit = async (reportReason: string) => {
    try {
      const reportData: any = {
        reportType: "COMMENT",
        targetId: comment.id,
        content: reportReason,
      };

      // 회원인 경우에만 userId 추가
      if (user?.userId) {
        reportData.userId = user.userId;
      }

      const response = await userApi.submitSuggestion(reportData);

      if (response.success) {
        alert("신고가 접수되었습니다. 검토 후 적절한 조치를 취하겠습니다.");
      } else {
        alert(response.error || "신고 접수에 실패했습니다. 다시 시도해주세요.");
      }
    } catch (error) {
      console.error("Report failed:", error);
      alert("신고 접수 중 오류가 발생했습니다. 다시 시도해주세요.");
    }
  };

  return (
    <div
      id={`comment-${comment.id}`}
      className={`${
        depth > 0 ? "border-l-2 border-gray-200" : ""
      } transition-colors duration-500`}
      style={{ marginLeft: `${marginLeft}px` }}
    >
      <div className="p-4 bg-gray-50 rounded-lg mb-4 comment-content">
        {editingComment?.id === comment.id ? (
          <div className="p-4 bg-gray-100 rounded-lg">
            <Textarea
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
            />
            {!isMyComment(comment) && (
              <Input
                type="password"
                placeholder="비밀번호"
                className="mt-2"
                value={editPassword}
                onChange={(e) => setEditPassword(e.target.value)}
              />
            )}
            <div className="flex justify-end space-x-2 mt-2">
              <Button onClick={onUpdateComment}>수정완료</Button>
              <Button variant="ghost" onClick={onCancelEdit}>
                취소
              </Button>
            </div>
          </div>
        ) : (
          <div>
            <div className="flex items-start justify-between mb-2">
              <div className="flex items-center space-x-2">
                <Avatar className="w-8 h-8">
                  <AvatarFallback>
                    {comment.userName?.charAt(0) || "?"}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <p className="font-semibold">{comment.userName}</p>
                  <p className="text-xs text-gray-500">
                    {new Date(comment.createdAt).toLocaleString()}
                  </p>
                </div>
              </div>
              <div className="flex space-x-2">
                <Button
                  size="sm"
                  variant={comment.userLike ? "default" : "outline"}
                  onClick={() => onLikeComment(comment)}
                  className={`${
                    comment.userLike
                      ? "bg-blue-500 hover:bg-blue-600 text-white"
                      : "hover:bg-blue-50"
                  }`}
                >
                  <ThumbsUp
                    className={`w-4 h-4 mr-1 ${
                      comment.userLike ? "fill-current" : ""
                    }`}
                  />
                  {comment.likes}
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => onReplyTo(comment)}
                >
                  <Reply className="w-4 h-4 mr-1" />
                  답글
                </Button>
                {!isMyComment(comment) && !comment.deleted && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setIsReportModalOpen(true)}
                    className="hover:bg-red-50 border-red-300 text-red-600 hover:text-red-700"
                  >
                    <Flag className="w-4 h-4 mr-1" />
                    신고
                  </Button>
                )}
                {canModifyComment(comment) && !comment.deleted && (
                  <>
                    <Button size="sm" onClick={() => onEditComment(comment)}>
                      수정
                    </Button>
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => onDeleteComment(comment)}
                    >
                      삭제
                    </Button>
                  </>
                )}
              </div>
            </div>
            <div
              className="prose max-w-none prose-sm"
              dangerouslySetInnerHTML={{ __html: comment.content }}
            />

            {/* 답글 작성 폼 */}
            {replyingTo?.id === comment.id && (
              <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
                <h4 className="text-sm font-semibold mb-3 text-blue-700">
                  {comment.userName}님에게 답글 작성
                </h4>
                {!isAuthenticated && (
                  <div className="mb-3">
                    <Input
                      type="password"
                      placeholder="비밀번호"
                      value={replyPassword}
                      onChange={(e) => setReplyPassword(e.target.value)}
                    />
                  </div>
                )}
                <div className="flex space-x-2">
                  <Textarea
                    placeholder="답글을 입력하세요..."
                    value={replyContent}
                    onChange={(e) => setReplyContent(e.target.value)}
                    className="flex-1"
                  />
                  <div className="flex flex-col space-y-2">
                    <Button
                      size="sm"
                      onClick={onReplySubmit}
                      disabled={isSubmittingReply}
                    >
                      작성
                    </Button>
                    <Button size="sm" variant="ghost" onClick={onCancelReply}>
                      취소
                    </Button>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 대댓글들을 재귀적으로 렌더링 */}
      {comment.replies && comment.replies.length > 0 && (
        <div className="space-y-2">
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              depth={depth + 1}
              editingComment={editingComment}
              editContent={editContent}
              editPassword={editPassword}
              replyingTo={replyingTo}
              replyContent={replyContent}
              replyPassword={replyPassword}
              isAuthenticated={isAuthenticated}
              isSubmittingReply={isSubmittingReply}
              postId={postId}
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
      )}

      {/* 신고 모달 */}
      <ReportModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        onSubmit={handleReportSubmit}
        type="댓글"
      />
    </div>
  );
};

"use client";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { SafeHTML } from "@/components/ui";
import { ThumbsUp, Reply, Flag, MoreHorizontal, User } from "lucide-react";
import { Comment, userApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { ReportModal } from "@/components/ui/ReportModal";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/molecules/dropdown-menu";
import { useState } from "react";
import Link from "next/link";
import { useToast } from "@/hooks/useToast";

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
  const { showSuccess, showError } = useToast();
  const maxDepth = 3; // 최대 들여쓰기 레벨
  const actualDepth = Math.min(depth, maxDepth);
  const marginLeft = actualDepth * 16; // 16px씩 들여쓰기 (모바일 최적화)
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);

  const handleReport = async () => {
    if (!isAuthenticated || !user) {
      showError("로그인 필요", "로그인이 필요한 기능입니다.");
      return;
    }

    try {
      const response = await userApi.submitSuggestion({
        reportType: "COMMENT",
        userId: user.userId,
        targetId: comment.id,
        content: `댓글 신고: ${comment.content.substring(0, 50)}...`,
      });

      if (response.success) {
        showSuccess(
          "신고 접수",
          "신고가 접수되었습니다. 검토 후 적절한 조치를 취하겠습니다."
        );
      } else {
        showError(
          "신고 실패",
          response.error || "신고 접수에 실패했습니다. 다시 시도해주세요."
        );
      }
    } catch (error) {
      console.error("Report failed:", error);
      showError(
        "신고 실패",
        "신고 접수 중 오류가 발생했습니다. 다시 시도해주세요."
      );
    }
  };

  return (
    <div
      id={`comment-${comment.id}`}
      className={`${
        depth > 0 ? "border-l-2 border-gray-200 pl-2" : ""
      } transition-colors duration-500`}
      style={{ marginLeft: `${marginLeft}px` }}
    >
      <div className="p-3 sm:p-4 bg-gray-50 rounded-lg mb-3 comment-content">
        {editingComment?.id === comment.id ? (
          <div className="p-3 sm:p-4 bg-gray-100 rounded-lg">
            <Textarea
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
              className="min-h-[80px]"
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
            <div className="flex flex-col sm:flex-row gap-2 sm:justify-end mt-3">
              <Button
                onClick={onUpdateComment}
                size="sm"
                className="w-full sm:w-auto"
              >
                수정완료
              </Button>
              <Button
                variant="ghost"
                onClick={onCancelEdit}
                size="sm"
                className="w-full sm:w-auto"
              >
                취소
              </Button>
            </div>
          </div>
        ) : (
          <div>
            {/* 헤더: 닉네임, 날짜, 액션 버튼들 */}
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2 min-w-0 flex-1">
                <Link
                  href={`/rolling-paper/${encodeURIComponent(
                    comment.userName
                  )}`}
                  className="font-semibold text-sm sm:text-base hover:text-purple-600 hover:underline transition-colors inline-flex items-center space-x-1 truncate"
                  title={`${comment.userName}님의 롤링페이퍼 보기`}
                >
                  <User className="w-3 h-3 flex-shrink-0" />
                  <span className="truncate">{comment.userName}</span>
                </Link>
                <span className="text-xs text-gray-500 whitespace-nowrap">
                  {new Date(comment.createdAt).toLocaleDateString()}
                </span>
              </div>

              {/* 모바일: 드롭다운 메뉴, 데스크톱: 버튼들 */}
              <div className="flex items-center gap-1">
                {/* 추천 버튼은 항상 표시 */}
                <Button
                  size="sm"
                  variant={comment.userLike ? "default" : "outline"}
                  onClick={() => onLikeComment(comment)}
                  className={`text-xs px-2 py-1 h-7 ${
                    comment.userLike
                      ? "bg-blue-500 hover:bg-blue-600 text-white"
                      : "hover:bg-blue-50"
                  }`}
                >
                  <ThumbsUp
                    className={`w-3 h-3 mr-1 ${
                      comment.userLike ? "fill-current" : ""
                    }`}
                  />
                  {comment.likes}
                </Button>

                {/* 답글 버튼 (모바일에서도 항상 표시) */}
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => onReplyTo(comment)}
                  className="text-xs px-2 py-1 h-7"
                >
                  <Reply className="w-3 h-3 mr-1" />
                  <span className="hidden sm:inline">답글</span>
                </Button>

                {/* 추가 액션들 (드롭다운) */}
                {(!isMyComment(comment) || canModifyComment(comment)) &&
                  !comment.deleted && (
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-xs px-2 py-1 h-7 text-gray-500 hover:text-gray-700"
                        >
                          <MoreHorizontal className="w-3 h-3" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-24">
                        {!isMyComment(comment) && !comment.deleted && (
                          <DropdownMenuItem
                            onClick={handleReport}
                            className="text-red-600 hover:text-red-700 cursor-pointer"
                          >
                            <Flag className="w-3 h-3 mr-2" />
                            신고
                          </DropdownMenuItem>
                        )}
                        {canModifyComment(comment) && !comment.deleted && (
                          <>
                            <DropdownMenuItem
                              onClick={() => onEditComment(comment)}
                              className="cursor-pointer"
                            >
                              수정
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={() => onDeleteComment(comment)}
                              className="text-red-600 hover:text-red-700 cursor-pointer"
                            >
                              삭제
                            </DropdownMenuItem>
                          </>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  )}
              </div>
            </div>

            {/* 댓글 내용 */}
            <SafeHTML
              html={comment.content}
              className="prose max-w-none prose-sm text-sm sm:text-base leading-relaxed"
            />

            {/* 답글 작성 폼 */}
            {replyingTo?.id === comment.id && (
              <div className="mt-4 p-3 sm:p-4 bg-blue-50 rounded-lg border border-blue-200">
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
                <div className="space-y-3">
                  <Textarea
                    placeholder="답글을 입력하세요..."
                    value={replyContent}
                    onChange={(e) => setReplyContent(e.target.value)}
                    className="min-h-[80px] resize-none"
                  />
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={onReplySubmit}
                      disabled={isSubmittingReply}
                      className="flex-1 sm:flex-none"
                    >
                      작성
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={onCancelReply}
                      className="flex-1 sm:flex-none"
                    >
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
        onSubmit={handleReport}
        type="댓글"
      />
    </div>
  );
};

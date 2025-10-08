"use client";

import React from "react";
import dynamic from "next/dynamic";
import { Button, Input, Textarea, SafeHTML, Spinner, TimeBadge } from "@/components";
import { Button as FlowbiteButton } from "flowbite-react";
import { ThumbsUp, Reply, MoreHorizontal, User, ExternalLink, CornerDownRight } from "lucide-react";
import { Comment, userCommand } from "@/lib/api";
import { useAuth } from "@/hooks";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/molecules/dropdown-menu";
import { useState } from "react";
import Link from "next/link";
import { useToast } from "@/hooks";
import { Popover } from "flowbite-react";

const ReportModal = dynamic(
  () => import("@/components/organisms/common/ReportModal").then(mod => ({ default: mod.ReportModal })),
  {
    ssr: false,
    loading: () => (
      <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center">
        <div className="bg-white rounded-lg p-6 flex flex-col items-center gap-3">
          <Spinner size="md" />
          <p className="text-sm text-brand-secondary">신고 모달 로딩 중...</p>
        </div>
      </div>
    ),
  }
);

interface CommentItemProps {
  comment: Comment & { replies?: Comment[] };
  depth: number;
  parentUserName?: string; // 부모 댓글 작성자명 추가
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

export const CommentItem: React.FC<CommentItemProps> = React.memo(({
  comment,
  depth,
  parentUserName,
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
  const { showFeedback, showError } = useToast();

  // 댓글 계층구조 처리: 최대 3단계까지만 지원하여 모바일에서도 읽기 편하도록 제한
  const maxDepth = 3; // 최대 들여쓰기 레벨
  const actualDepth = Math.min(depth, maxDepth); // depth가 3을 초과하면 3으로 제한
  const marginLeft = actualDepth * 16; // 16px씩 들여쓰기 (모바일 최적화)
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);

  // 댓글 신고 처리 함수: 비로그인 사용자도 신고 가능
  // v2 API를 사용하여 신고 타입과 대상 ID, 사유를 전송
  const handleReport = async (reason: string) => {
    try {
      // v2 신고 API 사용 - 익명 사용자도 신고 가능
      const response = await userCommand.submitReport({
        reportType: "COMMENT",
        targetId: comment.id,
        content: reason,
        reporterId: isAuthenticated && user?.memberId ? user.memberId : null,
        reporterName: isAuthenticated && user?.memberName ? user.memberName : "익명",
      });

      if (response.success) {
        showFeedback(
          "댓글 신고가 접수되었습니다",
          "검토 후 적절한 조치를 취하겠습니다. 신고해 주셔서 감사합니다.",
          {
            label: "확인",
            onClick: () => setIsReportModalOpen(false)
          }
        );
        setIsReportModalOpen(false);
      } else {
        showError(
          "신고 실패",
          response.error || "신고 접수에 실패했습니다. 다시 시도해주세요."
        );
      }
    } catch {
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
        depth > 0 ? "border-l-2 border-purple-300 pl-2" : ""
      } transition-colors duration-500`}
      style={{ marginLeft: `${marginLeft}px` }}
      aria-label={depth > 0 ? "답글" : "댓글"}
    >
      <div className={`p-3 sm:p-4 ${depth > 0 ? "bg-gray-100" : "bg-gray-50"} rounded-lg mb-3 comment-content`}>
        {/* 댓글 수정 모드: 현재 수정 중인 댓글과 일치할 때 수정 폼 표시 */}
        {editingComment?.id === comment.id ? (
          <div className="p-3 sm:p-4 bg-gray-100 rounded-lg">
            <Textarea
              value={editContent}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setEditContent(e.target.value)}
              className="min-h-[80px]"
            />
            {/* 익명 댓글 수정 시에만 비밀번호 입력 필요 */}
            {!isMyComment(comment) && (
              <Input
                type="password"
                placeholder="비밀번호 (1000~9999)"
                className="mt-2"
                value={editPassword}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditPassword(e.target.value)}
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
                {/* 대댓글인 경우 아이콘 표시 */}
                {depth > 0 && (
                  <CornerDownRight className="w-4 h-4 text-purple-500 flex-shrink-0" />
                )}
                {comment.memberName && comment.memberName !== "익명" ? (
                  <Popover
                    trigger="click"
                    placement="bottom"
                    content={
                      <div className="p-3 w-56">
                        <div className="flex flex-col space-y-2">
                          <div className="flex items-center space-x-2">
                            <User className="w-4 h-4" />
                            <span className="font-medium">{comment.memberName}</span>
                          </div>
                          <Link
                            href={`/rolling-paper/${encodeURIComponent(
                              comment.memberName
                            )}`}
                          >
                            <Button size="sm" className="w-full justify-start">
                              <ExternalLink className="w-4 h-4 mr-2" />
                              롤링페이퍼 보기
                            </Button>
                          </Link>
                        </div>
                      </div>
                    }
                  >
                    <button className="font-semibold text-sm sm:text-base hover:text-purple-600 hover:underline transition-colors cursor-pointer inline-flex items-center space-x-1 truncate">
                      <User className="w-3 h-3 flex-shrink-0" />
                      <span className="truncate">{comment.memberName}</span>
                    </button>
                  </Popover>
                ) : (
                  <span className="font-semibold text-sm sm:text-base inline-flex items-center space-x-1 truncate text-brand-secondary">
                    <User className="w-3 h-3 flex-shrink-0 stroke-slate-600 fill-slate-100" />
                    <span className="truncate">{comment.memberName || "익명"}</span>
                  </span>
                )}
                {comment.popular && (
                  <span className="bg-gradient-to-r from-pink-500 to-purple-600 text-white text-xs px-2 py-1 rounded-full font-semibold flex-shrink-0">
                    인기
                  </span>
                )}
                <TimeBadge dateString={comment.createdAt} size="xs" />
              </div>

              {/* 액션 버튼들: 모바일에서는 핵심 기능만 노출, 나머지는 드롭다운으로 처리 */}
              <div className="flex items-center gap-1">
                {/* 추천 버튼: 로그인/비로그인 상관없이 항상 표시 */}
                <FlowbiteButton
                  size="xs"
                  color={comment.userLike ? "blue" : "light"}
                  onClick={() => onLikeComment(comment)}
                >
                  <ThumbsUp className={`w-4 h-4 mr-2 ${comment.userLike ? "fill-current" : ""}`} />
                  추천 {comment.likeCount}
                </FlowbiteButton>

                {/* 답글 버튼: 모바일에서도 항상 표시하여 접근성 향상 */}
                <FlowbiteButton
                  size="xs"
                  className="bg-gradient-to-r from-purple-500 to-pink-500 text-white hover:bg-gradient-to-l"
                  onClick={() => onReplyTo(comment)}
                >
                  <Reply className="w-4 h-4 mr-2" />
                  답글
                </FlowbiteButton>

                {/* 신고 버튼: 다른 사람의 댓글인 경우만 표시 */}
                {!isMyComment(comment) && !comment.deleted && (
                  <FlowbiteButton
                    size="xs"
                    color="red"
                    onClick={() => setIsReportModalOpen(true)}
                  >
                    신고
                  </FlowbiteButton>
                )}

                {/* 수정/삭제 버튼을 위한 드롭다운 */}
                {canModifyComment(comment) && !comment.deleted && (
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-xs px-2 py-1 h-7 text-brand-secondary hover:text-brand-primary"
                      >
                        <MoreHorizontal className="w-3 h-3" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-24">
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
                    </DropdownMenuContent>
                  </DropdownMenu>
                )}
              </div>
            </div>

            {/* 부모 댓글 작성자 표시 (대댓글인 경우) */}
            {depth > 0 && parentUserName && (
              <div className="text-sm text-purple-600 mb-2">
                @{parentUserName}
              </div>
            )}

            {/* 댓글 내용 */}
            <SafeHTML
              html={comment.content}
              className="prose max-w-none prose-sm text-sm sm:text-base leading-relaxed"
            />

            {/* 답글 작성 폼: 해당 댓글에 답글을 작성 중일 때만 표시 */}
            {replyingTo?.id === comment.id && (
              <div className="mt-4 p-3 sm:p-4 bg-blue-50 rounded-lg border border-blue-200">
                <h4 className="text-sm font-semibold mb-3 text-blue-700">
                  {comment.memberName}님에게 답글 작성
                </h4>
                {/* 비로그인 사용자는 비밀번호 입력 필요 */}
                {!isAuthenticated && (
                  <div className="mb-3">
                    <Input
                      type="password"
                      placeholder="비밀번호 (1000~9999)"
                      value={replyPassword}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setReplyPassword(e.target.value)}
                    />
                  </div>
                )}
                <div className="space-y-3">
                  <Textarea
                    placeholder="답글을 입력하세요..."
                    value={replyContent}
                    onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setReplyContent(e.target.value)}
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

      {/* 대댓글 재귀 렌더링: 답글이 있으면 동일한 CommentItem으로 중첩 표시 */}
      {comment.replies && comment.replies.length > 0 && (
        <div className="space-y-2">
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              depth={depth + 1}
              parentUserName={comment.memberName || "익명"} // 부모 댓글 작성자명 전달
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
}, (prevProps, nextProps) => {
  // React.memo 최적화: 댓글 컴포넌트의 불필요한 리렌더링 방지
  // 주요 상태 변화만 감지하여 성능 최적화

  // Comment 객체의 핵심 필드들만 비교
  if (prevProps.comment.id !== nextProps.comment.id) return false;
  if (prevProps.comment.content !== nextProps.comment.content) return false;
  if (prevProps.comment.likeCount !== nextProps.comment.likeCount) return false;
  if (prevProps.comment.userLike !== nextProps.comment.userLike) return false;
  if (prevProps.comment.deleted !== nextProps.comment.deleted) return false;

  // 댓글 수정/답글 상태 비교
  if (prevProps.editingComment?.id !== nextProps.editingComment?.id) return false;
  if (prevProps.replyingTo?.id !== nextProps.replyingTo?.id) return false;

  // 기본 props 비교
  if (prevProps.depth !== nextProps.depth) return false;
  if (prevProps.editContent !== nextProps.editContent) return false;
  if (prevProps.editPassword !== nextProps.editPassword) return false;
  if (prevProps.replyContent !== nextProps.replyContent) return false;
  if (prevProps.replyPassword !== nextProps.replyPassword) return false;
  if (prevProps.isAuthenticated !== nextProps.isAuthenticated) return false;
  if (prevProps.isSubmittingReply !== nextProps.isSubmittingReply) return false;

  // 답글 개수 비교
  if ((prevProps.comment.replies?.length || 0) !== (nextProps.comment.replies?.length || 0)) return false;

  return true;
});

CommentItem.displayName = "CommentItem";

"use client";

import React, { useState, useMemo } from "react";
import { Button, SafeHTML, TimeBadge } from "@/components";
import { LazyReportModal } from "@/lib/utils/lazy-components";
import { User, CornerDownRight, ChevronDown, ChevronUp } from "lucide-react";
import { Comment } from "@/lib/api";
import { submitReportAction } from "@/lib/actions/user";
import { useAuthStore } from "@/stores/auth.store";
import { useToastStore } from "@/stores/toast.store";
import { UserActionPopover } from "@/components/molecules/UserActionPopover";
import { CommentEditForm } from "./CommentEditForm";
import { CommentReplyForm } from "./CommentReplyForm";
import { CommentActions } from "./CommentActions";
import type { CommentHandlers, CommentEditState, CommentReplyState } from "@/hooks/features/post/useCommentInteraction";

interface CommentItemProps {
  comment: Comment & { replies?: Comment[] };
  depth: number;
  parentUserName?: string;
  isAuthenticated: boolean;
  isSubmittingReply: boolean;
  isUpdatingComment: boolean;
  postId: number;
  handlers: CommentHandlers;
  editState: CommentEditState;
  replyState: CommentReplyState;
}

export const CommentItem: React.FC<CommentItemProps> = React.memo(({
  comment,
  depth,
  parentUserName,
  isAuthenticated,
  isSubmittingReply,
  isUpdatingComment,
  postId,
  handlers,
  editState,
  replyState,
}) => {
  // 그룹화된 객체에서 구조 분해
  const {
    onEditComment, onUpdateComment, onCancelEdit, onDeleteComment,
    onReplyTo, onReplySubmit, onCancelReply, onLikeComment,
    isMyComment, canModifyComment,
  } = handlers;
  const {
    editingComment, editContent, editPassword,
    setEditContent, setEditPassword,
  } = editState;
  const {
    replyingTo, replyContent, replyPassword,
    setReplyContent, setReplyPassword,
  } = replyState;
  // 신고 시에만 사용하므로 구독 대신 이벤트 시점에 직접 접근 (memo 우회 방지)

  // 댓글 계층구조 처리: 최대 3단계까지만 지원하여 모바일에서도 읽기 편하도록 제한
  const maxDepth = 3; // 최대 들여쓰기 레벨
  const actualDepth = Math.min(depth, maxDepth); // depth가 3을 초과하면 3으로 제한
  const marginLeft = actualDepth * 16; // 16px씩 들여쓰기 (모바일 최적화)
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);

  // 답글 접기/펼치기 상태: 답글 2개 이하는 기본 펼침, 3개 이상은 접힘
  const [isRepliesExpanded, setIsRepliesExpanded] = useState(() => {
    if (!comment.replies || comment.replies.length === 0) return false;
    return comment.replies.length <= 2;
  });

  // 인기 답글이 있으면 자동으로 펼침
  React.useEffect(() => {
    if (comment.replies && comment.replies.some(reply => reply.popular)) {
      setIsRepliesExpanded(true);
    }
  }, [comment.replies]);

  // HTML 태그를 제거하고 순수 텍스트 길이 계산 (수정 모드)
  const editPlainTextLength = useMemo(() => {
    if (typeof window === "undefined") return 0;
    const div = document.createElement("div");
    div.innerHTML = editContent;
    return (div.textContent || div.innerText || "").length;
  }, [editContent]);

  // HTML 태그를 제거하고 순수 텍스트 길이 계산 (답글 모드)
  const replyPlainTextLength = useMemo(() => {
    if (typeof window === "undefined") return 0;
    const div = document.createElement("div");
    div.innerHTML = replyContent;
    return (div.textContent || div.innerText || "").length;
  }, [replyContent]);

  // 댓글 신고 처리 함수: 비로그인 사용자도 신고 가능
  // v2 API를 사용하여 신고 타입과 대상 ID, 사유를 전송
  const handleReport = async (reason: string) => {
    const user = useAuthStore.getState().user;
    const { showFeedback, showError } = useToastStore.getState();
    try {
      const response = await submitReportAction({
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
        depth > 0 ? "border-l-2 border-purple-300 dark:border-purple-600 pl-2" : ""
      } transition-colors duration-500`}
      style={{ marginLeft: `${marginLeft}px` }}
      aria-label={depth > 0 ? "답글" : "댓글"}
    >
      <div className={`p-3 sm:p-4 ${depth > 0 ? "bg-gray-100 dark:bg-gray-800/50" : "bg-gray-50 dark:bg-gray-800"} rounded-lg mb-3 comment-content`}>
        {/* 댓글 수정 모드: 현재 수정 중인 댓글과 일치할 때 수정 폼 표시 */}
        {editingComment?.id === comment.id ? (
          <CommentEditForm
            editContent={editContent}
            editPassword={editPassword}
            isAnonymous={comment.memberName === "익명" || comment.memberName === null}
            isUpdatingComment={isUpdatingComment}
            editPlainTextLength={editPlainTextLength}
            onUpdateComment={onUpdateComment}
            onCancelEdit={onCancelEdit}
            setEditContent={setEditContent}
            setEditPassword={setEditPassword}
          />
        ) : (
          <div>
            {/* [프로필] 닉네임 · 날짜 (헤더: 액션 버튼 제거) */}
            <div className="flex items-center gap-2 mb-3">
              <div className="flex items-center gap-2 min-w-0">
                {/* 대댓글인 경우 아이콘 표시 */}
                {depth > 0 && (
                  <CornerDownRight className="w-4 h-4 text-purple-500 flex-shrink-0" />
                )}
                {comment.memberName && comment.memberName !== "익명" ? (
                  <UserActionPopover
                    memberName={comment.memberName}
                    memberId={comment.memberId}
                    trigger={
                      <button className="font-semibold text-sm sm:text-base hover:text-purple-600 hover:underline transition-colors cursor-pointer inline-flex items-center space-x-1 truncate">
                        <User className="w-3 h-3 flex-shrink-0" />
                        <span className="truncate">{comment.memberName}</span>
                      </button>
                    }
                    placement="bottom"
                  />
                ) : (
                  <span className="font-semibold text-sm sm:text-base inline-flex items-center space-x-1 truncate text-brand-secondary">
                    <User className="w-3 h-3 flex-shrink-0 stroke-slate-600 fill-slate-100" />
                    <span className="truncate">{comment.memberName || "익명"}</span>
                  </span>
                )}
                <TimeBadge dateString={comment.createdAt} size="xs" />
              </div>
            </div>

            {/* 부모 댓글 작성자 표시 (대댓글인 경우) */}
            {depth > 0 && parentUserName && (
              <div className="text-sm text-purple-600 dark:text-purple-400 mb-2">
                @{parentUserName}
              </div>
            )}

            {/* 댓글 내용 */}
            <SafeHTML
              html={comment.content}
              className="prose max-w-none prose-sm text-sm sm:text-base leading-relaxed"
            />

            {/* 추천 답글 신고 (액션 버튼을 댓글 내용 아래로 이동) */}
            <CommentActions
              comment={comment}
              isAuthenticated={isAuthenticated}
              canModify={canModifyComment(comment)}
              isMyComment={isMyComment(comment)}
              onLikeComment={onLikeComment}
              onReplyTo={onReplyTo}
              onEditComment={onEditComment}
              onDeleteComment={onDeleteComment}
              onReportClick={() => setIsReportModalOpen(true)}
            />

            {/* 답글 작성 폼: 해당 댓글에 답글을 작성 중일 때만 표시 */}
            {replyingTo?.id === comment.id && (
              <CommentReplyForm
                targetUserName={comment.memberName || "익명"}
                replyContent={replyContent}
                replyPassword={replyPassword}
                isAuthenticated={isAuthenticated}
                isSubmittingReply={isSubmittingReply}
                replyPlainTextLength={replyPlainTextLength}
                onReplySubmit={onReplySubmit}
                onCancelReply={onCancelReply}
                setReplyContent={setReplyContent}
                setReplyPassword={setReplyPassword}
              />
            )}
          </div>
        )}
      </div>

      {/* 대댓글 섹션: 접기/펼치기 기능 포함 */}
      {comment.replies && comment.replies.length > 0 && (
        <div className="mt-3 space-y-2">
          {/* 미리보기: 접혀있을 때 최대 2개까지만 표시 */}
          {!isRepliesExpanded && comment.replies.slice(0, 2).map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              depth={depth + 1}
              parentUserName={comment.memberName || "익명"}
              isAuthenticated={isAuthenticated}
              isSubmittingReply={isSubmittingReply}
              isUpdatingComment={isUpdatingComment}
              postId={postId}
              handlers={handlers}
              editState={editState}
              replyState={replyState}
            />
          ))}

          {/* 답글 토글 버튼: 3개 이상일 때만 표시 */}
          {comment.replies.length > 2 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsRepliesExpanded(!isRepliesExpanded)}
              className="w-full text-purple-600 dark:text-purple-400 hover:text-purple-700 dark:hover:text-purple-300 hover:bg-purple-50 dark:hover:bg-purple-900/30 flex items-center justify-center gap-2"
            >
              {isRepliesExpanded ? (
                <>
                  <ChevronUp className="w-4 h-4" />
                  답글 숨기기
                </>
              ) : (
                <>
                  <ChevronDown className="w-4 h-4" />
                  답글 {comment.replies.length - 2}개 더보기
                </>
              )}
            </Button>
          )}

          {/* 전체 답글: 펼쳐있을 때 모든 답글 표시 */}
          {isRepliesExpanded && comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              depth={depth + 1}
              parentUserName={comment.memberName || "익명"}
              isAuthenticated={isAuthenticated}
              isSubmittingReply={isSubmittingReply}
              isUpdatingComment={isUpdatingComment}
              postId={postId}
              handlers={handlers}
              editState={editState}
              replyState={replyState}
            />
          ))}
        </div>
      )}

      {/* 신고 모달 */}
      <LazyReportModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        onSubmit={handleReport}
        type="댓글"
      />
    </div>
  );
}, (prevProps, nextProps) => {
  // React.memo 최적화: 댓글 컴포넌트의 불필요한 리렌더링 방지
  // 그룹화된 객체는 useMemo로 참조 안정성이 보장되므로 얕은 비교로 충분

  // Comment 객체의 핵심 필드들만 비교
  if (prevProps.comment.id !== nextProps.comment.id) return false;
  if (prevProps.comment.content !== nextProps.comment.content) return false;
  if (prevProps.comment.likeCount !== nextProps.comment.likeCount) return false;
  if (prevProps.comment.userLike !== nextProps.comment.userLike) return false;
  if (prevProps.comment.deleted !== nextProps.comment.deleted) return false;

  // 기본 props 비교
  if (prevProps.depth !== nextProps.depth) return false;
  if (prevProps.isAuthenticated !== nextProps.isAuthenticated) return false;
  if (prevProps.isSubmittingReply !== nextProps.isSubmittingReply) return false;
  if (prevProps.isUpdatingComment !== nextProps.isUpdatingComment) return false;

  // 그룹화된 객체 참조 비교 (useMemo로 안정성 보장)
  if (prevProps.handlers !== nextProps.handlers) return false;
  if (prevProps.editState !== nextProps.editState) return false;
  if (prevProps.replyState !== nextProps.replyState) return false;

  // 답글 개수 비교
  if ((prevProps.comment.replies?.length || 0) !== (nextProps.comment.replies?.length || 0)) return false;

  return true;
});

CommentItem.displayName = "CommentItem";

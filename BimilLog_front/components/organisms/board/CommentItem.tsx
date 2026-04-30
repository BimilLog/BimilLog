"use client";

import React, { useState, useMemo } from "react";
import { Button, SafeHTML, TimeBadge } from "@/components";
import { LazyReportModal } from "@/lib/utils/lazy-components";
import { User, CornerDownRight, ChevronDown, ChevronUp, MailX, Sparkles } from "lucide-react";
import { Comment } from "@/lib/api";
import { submitReportAction } from "@/lib/actions/user";
import { useAuthStore } from "@/stores/auth.store";
import { useToastStore } from "@/stores/toast.store";
import { UserActionPopover } from "@/components/molecules/UserActionPopover";
import { CommentEditForm } from "./CommentEditForm";
import { CommentReplyForm } from "./CommentReplyForm";
import { CommentActions } from "./CommentActions";
import type { CommentHandlers, CommentEditState, CommentReplyState } from "@/hooks/features/post/useCommentInteraction";

// 라운드 8 B-8-016: depth(0~3)에 따른 들여쓰기 클래스 매핑.
// inline style 의 px 고정 값을 Tailwind 클래스로 교체하여 다크/JIT/rem 스케일 대응.
// 모바일에서 답글 깊이 누적 시 본문 영역이 좁아지지 않도록 sm 이상에서 더 큰 간격을 적용.
const DEPTH_INDENT_CLASS: Record<number, string> = {
  0: "ml-0",
  1: "ml-2 sm:ml-4",
  2: "ml-4 sm:ml-8",
  3: "ml-6 sm:ml-12",
};

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
  const actualDepth = Math.min(depth, maxDepth);
  const indentClass = DEPTH_INDENT_CLASS[actualDepth] ?? "ml-0";
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

  // 라운드 8 B-8-002: 삭제된 댓글 여부.
  const isDeleted = comment.deleted === true;
  // 라운드 8 B-8-009: 본인 댓글 시각 마커 (탈퇴/익명 false positive 방지)
  const isAuthorMine = !isDeleted && isMyComment(comment);
  // 라운드 8 B-8 응답 활용: 탈퇴한 사용자 (memberId === null && memberName 부재)
  const isWithdrawnAuthor = !isDeleted && comment.memberId == null && (!comment.memberName || comment.memberName === "익명");

  return (
    <article
      id={`comment-${comment.id}`}
      data-comment-id={comment.id}
      data-deleted={isDeleted ? "true" : "false"}
      className={`${
        depth > 0 ? "border-l-2 border-stamp-red/40 dark:border-stamp-red/60 pl-2" : ""
      } ${indentClass} transition-colors duration-500`}
      aria-label={depth > 0 ? "답글" : "댓글"}
    >
      <div
        className={`p-3 sm:p-4 break-keep rounded-lg mb-3 comment-content ${
          isDeleted
            ? "bg-paper-aged/60 dark:bg-postal-navy/10 border border-dashed border-postal-navy/30 dark:border-postal-navy/40"
            : depth > 0
            ? "bg-paper-aged dark:bg-postal-navy/15"
            : "bg-paper-50 dark:bg-postal-navy/25"
        } ${isAuthorMine ? "ring-1 ring-stamp-red/30 dark:ring-stamp-red/40" : ""}`}
      >
        {/* 라운드 8 B-8-002: 삭제된 댓글 마스킹 — 본문/액션은 숨기고 자식 트리는 유지 */}
        {isDeleted ? (
          <div className="flex items-center gap-2 text-ink-soft dark:text-paper-200/70">
            <MailX className="w-4 h-4 stroke-ink-soft flex-shrink-0" aria-hidden="true" />
            <p className="italic text-sm sm:text-base break-keep">
              삭제된 댓글입니다.
            </p>
          </div>
        ) : editingComment?.id === comment.id ? (
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
            <div className="flex items-center gap-2 mb-3 flex-wrap">
              <div className="flex items-center gap-2 min-w-0">
                {/* 대댓글인 경우 아이콘 표시 */}
                {depth > 0 && (
                  <CornerDownRight className="w-4 h-4 text-postal-navy dark:text-paper-100 flex-shrink-0" aria-hidden="true" />
                )}
                {isWithdrawnAuthor ? (
                  <span className="font-semibold text-sm sm:text-base inline-flex items-center space-x-1 truncate text-ink-soft dark:text-paper-200/70 italic">
                    <User className="w-3 h-3 flex-shrink-0 stroke-ink-soft" aria-hidden="true" />
                    <span className="truncate">탈퇴한 사용자</span>
                  </span>
                ) : comment.memberName && comment.memberName !== "익명" ? (
                  <UserActionPopover
                    memberName={comment.memberName}
                    memberId={comment.memberId}
                    trigger={
                      <button className="font-semibold text-sm sm:text-base hover:text-postal-navy dark:hover:text-paper-100 hover:underline transition-colors cursor-pointer inline-flex items-center space-x-1 truncate text-ink dark:text-paper-50">
                        <User className="w-3 h-3 flex-shrink-0" aria-hidden="true" />
                        <span className="truncate">{comment.memberName}</span>
                      </button>
                    }
                    placement="bottom"
                  />
                ) : (
                  <span className="font-semibold text-sm sm:text-base inline-flex items-center space-x-1 truncate text-brand-secondary">
                    <User className="w-3 h-3 flex-shrink-0 stroke-slate-600 fill-slate-100" aria-hidden="true" />
                    <span className="truncate">{comment.memberName || "익명"}</span>
                  </span>
                )}
                <TimeBadge dateString={comment.createdAt} size="xs" />
              </div>
              {/* 라운드 8: 인기 댓글 뱃지 (백엔드 popular 필드 활용) */}
              {comment.popular && (
                <span className="inline-flex items-center gap-1 rounded-full bg-seal-gold/20 px-2 py-0.5 text-[11px] font-semibold text-stamp-red ring-1 ring-seal-gold/50 dark:bg-seal-gold/15 dark:text-seal-gold">
                  <Sparkles className="w-3 h-3" aria-hidden="true" />
                  인기 댓글
                </span>
              )}
              {/* 라운드 8 B-8-009: 본인 댓글 마커 */}
              {isAuthorMine && (
                <span className="inline-flex items-center rounded-full bg-stamp-red/10 px-2 py-0.5 text-[11px] font-semibold text-stamp-red ring-1 ring-stamp-red/40 dark:bg-stamp-red/20">
                  나
                </span>
              )}
            </div>

            {/* 부모 댓글 작성자 표시 (대댓글인 경우) */}
            {depth > 0 && parentUserName && (
              <div className="text-sm text-postal-navy dark:text-paper-100 mb-2 break-keep">
                @{parentUserName}
              </div>
            )}

            {/* 댓글 내용 (prose dark 토큰 추가 — 라운드 8) */}
            <SafeHTML
              html={comment.content}
              className="prose dark:prose-invert max-w-none prose-sm text-sm sm:text-base leading-relaxed break-keep text-ink dark:text-paper-50"
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

          {/* 답글 토글 버튼: 3개 이상일 때만 표시.
              라운드 8 B-8-003: 한국어 카피/클래스명 의존을 제거하기 위해
              data-replies-toggle / data-expanded 속성을 부여한다. */}
          {comment.replies.length > 2 && (
            <Button
              variant="ghost"
              size="sm"
              data-replies-toggle="true"
              data-expanded={isRepliesExpanded ? "true" : "false"}
              aria-expanded={isRepliesExpanded}
              onClick={() => setIsRepliesExpanded(!isRepliesExpanded)}
              className="w-full text-postal-navy dark:text-paper-100 hover:text-stamp-red dark:hover:text-stamp-red hover:bg-stamp-red/10 dark:hover:bg-stamp-red/20 flex items-center justify-center gap-2"
            >
              {isRepliesExpanded ? (
                <>
                  <ChevronUp className="w-4 h-4" aria-hidden="true" />
                  답글 숨기기
                </>
              ) : (
                <>
                  <ChevronDown className="w-4 h-4" aria-hidden="true" />
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
    </article>
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
  // 라운드 8: popular(인기 승격) / parentId / memberName(닉네임 변경) 비교 누락 보강
  if (prevProps.comment.popular !== nextProps.comment.popular) return false;
  if (prevProps.comment.parentId !== nextProps.comment.parentId) return false;
  if (prevProps.comment.memberName !== nextProps.comment.memberName) return false;
  if (prevProps.comment.memberId !== nextProps.comment.memberId) return false;

  // 기본 props 비교
  if (prevProps.depth !== nextProps.depth) return false;
  if (prevProps.parentUserName !== nextProps.parentUserName) return false;
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

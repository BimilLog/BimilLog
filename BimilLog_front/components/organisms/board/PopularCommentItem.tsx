"use client";

import { SafeHTML, TimeBadge, Button } from "@/components";
import { ThumbsUp, MessageSquare, Flag, User, ExternalLink } from "lucide-react";
import { Comment } from "@/lib/api";
import { Button as FlowbiteButton } from "flowbite-react";
import { useAuth } from "@/hooks";
import React from "react";
import { useToast } from "@/hooks";
import { Popover } from "flowbite-react";
import Link from "next/link";
import { submitReportAction } from "@/lib/actions/user";

interface PopularCommentItemProps {
  comment: Comment;
  onLikeComment: (comment: Comment) => void;
  onReplyTo: (comment: Comment) => void;
  onCommentClick: (commentId: number) => void;
}

export const PopularCommentItem = React.memo<PopularCommentItemProps>(({
  comment,
  onLikeComment,
  onReplyTo,
  onCommentClick,
}) => {
  const { user, isAuthenticated } = useAuth();
  const { showError, showFeedback, showWarning } = useToast();

  const isMyComment = (comment: Comment) => {
    return user?.memberId === comment.memberId;
  };

  const handleReport = async () => {
    if (!isAuthenticated || !user) {
      showWarning("로그인 필요", "로그인이 필요한 기능입니다.");
      return;
    }

    try {
      const response = await submitReportAction({
        reportType: "COMMENT",
        targetId: comment.id,
        content: `댓글 신고: ${comment.content.substring(0, 50)}...`,
        reporterId: user?.memberId ?? null,
        reporterName: user?.memberName ?? "익명",
      });

      if (response.success) {
        showFeedback(
          "신고가 접수되었습니다",
          "검토 후 적절한 조치를 취하겠습니다. 신고해 주셔서 감사합니다.",
          {
            label: "확인",
            onClick: () => {}
          }
        );
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
      className="p-3 sm:p-4 rounded-lg border border-blue-100 bg-white/70 backdrop-blur-sm transition-all duration-200 cursor-pointer hover:bg-white/90 hover:shadow-brand-md dark:border-blue-900/40 dark:bg-slate-900/70 dark:hover:bg-slate-900/90"
      onClick={() => onCommentClick(comment.id)}
    >
      {/* 헤더: 닉네임, 날짜, 액션 버튼들 */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2 min-w-0 flex-1">
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
              <button className="inline-flex items-center space-x-1 truncate text-sm font-semibold text-blue-800 transition-colors hover:text-purple-600 hover:underline sm:text-base dark:text-blue-300 dark:hover:text-purple-300">
                <User className="w-3 h-3 flex-shrink-0" />
                <span className="truncate">{comment.memberName}</span>
              </button>
            </Popover>
          ) : (
            <span className="inline-flex items-center space-x-1 truncate text-sm font-semibold text-brand-secondary sm:text-base dark:text-gray-300">
              <User className="w-3 h-3 flex-shrink-0 stroke-slate-600 fill-slate-100" />
              <span className="truncate">{comment.memberName || "익명"}</span>
            </span>
          )}
          <TimeBadge dateString={comment.createdAt} size="xs" />
        </div>


      </div>

      {/* 댓글 내용 */}
      <SafeHTML
        html={comment.content}
        className="prose max-w-none prose-sm text-sm leading-relaxed text-brand-primary sm:text-base dark:text-gray-100"
      />

      {/* 액션 버튼들 */}
              <div className="flex items-center gap-1">
                {/* 추천 버튼 */}
                <FlowbiteButton
                  size="xs"
                  color={comment.userLike ? "blue" : "light"}
                  onClick={(e) => {
                    e.stopPropagation();
                    onLikeComment(comment);
                  }}
                  disabled={!isAuthenticated}
                  title={!isAuthenticated ? "로그인 후 추천할 수 있습니다" : undefined}
                  className={!isAuthenticated ? "cursor-not-allowed opacity-60" : ""}
                >
                  <ThumbsUp className={`w-4 h-4 mr-2 ${comment.userLike ? "fill-current" : ""}`} />
                  추천 {comment.likeCount}
                </FlowbiteButton>

                {/* 답글 버튼 */}
                <FlowbiteButton
                  size="xs"
                  className="bg-gradient-to-r from-purple-500 to-pink-500 text-white hover:bg-gradient-to-l"
                  onClick={(e) => {
                    e.stopPropagation();
                    onReplyTo(comment);
                  }}
                >
                  <MessageSquare className="w-4 h-4 mr-2" />
                  답글
                </FlowbiteButton>

                {/* 신고 버튼 (본인 댓글이 아닌 경우만) */}
                {!isMyComment(comment) && !comment.deleted && (
                  <FlowbiteButton
                    size="xs"
                    color="red"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleReport();
                    }}
                  >
                    <Flag className="w-4 h-4 mr-2" />
                    신고
                  </FlowbiteButton>
                )}
              </div>

      {/* 클릭 안내 */}
      <div className="mt-3 border-t border-blue-100 pt-2 dark:border-blue-900/40">
        <p className="flex items-center gap-1 text-xs font-medium text-blue-600 dark:text-blue-300">
          원본 댓글로 이동하기
        </p>
      </div>
    </div>
  );
}, (prevProps, nextProps) => {
  // Comment 객체의 핵심 필드만 비교하여 불필요한 리렌더링 방지
  return (
    prevProps.comment.id === nextProps.comment.id &&
    prevProps.comment.content === nextProps.comment.content &&
    prevProps.comment.likeCount === nextProps.comment.likeCount &&
    prevProps.comment.userLike === nextProps.comment.userLike &&
    prevProps.comment.deleted === nextProps.comment.deleted
  );
});

PopularCommentItem.displayName = "PopularCommentItem";

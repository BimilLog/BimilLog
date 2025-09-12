"use client";

import { Card, CardContent, CardHeader, CardTitle, Button, SafeHTML, ReportModal } from "@/components";
import { ThumbsUp, MessageSquare, Flag, MoreHorizontal } from "lucide-react";
import { Comment, userApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/molecules/dropdown-menu";
import { useState } from "react";
import { useToast } from "@/hooks/useToast";

interface PopularCommentsProps {
  comments?: Comment[];
  onLikeComment: (comment: Comment) => void;
  onReplyTo: (comment: Comment) => void;
  onCommentClick: (commentId: number) => void;
}

export const PopularComments: React.FC<PopularCommentsProps> = ({
  comments,
  onLikeComment,
  onReplyTo,
  onCommentClick,
}) => {
  const { user, isAuthenticated } = useAuth();
  const [reportingCommentId, setReportingCommentId] = useState<number | null>(
    null
  );
  const { showSuccess, showError } = useToast();

  const isMyComment = (comment: Comment) => {
    return user?.userId === comment.userId;
  };

  const handleReport = async (comment: Comment) => {
    if (!isAuthenticated || !user) {
      showError("로그인 필요", "로그인이 필요한 기능입니다.");
      return;
    }

    try {
      const response = await userApi.submitReport({
        reportType: "COMMENT",
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
      showError(
        "신고 실패",
        "신고 접수 중 오류가 발생했습니다. 다시 시도해주세요."
      );
    }
  };

  if (!comments || comments.length === 0) {
    return null;
  }

  return (
    <>
      <Card className="bg-gradient-to-r from-blue-50 to-purple-50 border-blue-200 shadow-lg mb-6">
        <CardHeader className="pb-3">
          <CardTitle className="flex items-center gap-2 text-blue-700">
            <MessageSquare className="w-5 h-5" />
            <span className="text-base sm:text-lg">인기 댓글</span>
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {comments.map((comment) => (
            <div
              key={comment.id}
              className="p-3 sm:p-4 bg-white/70 backdrop-blur-sm rounded-lg border border-blue-100 shadow-sm cursor-pointer hover:bg-white/90 transition-all duration-200 hover:shadow-md"
              onClick={() => onCommentClick(comment.id)}
            >
              {/* 헤더: 닉네임, 날짜, 액션 버튼들 */}
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2 min-w-0 flex-1">
                  <p className="font-semibold text-sm sm:text-base text-blue-800 truncate">
                    {comment.userName}
                  </p>
                  <span className="bg-gradient-to-r from-pink-500 to-purple-600 text-white text-xs px-2 py-1 rounded-full font-semibold flex-shrink-0">
                    인기
                  </span>
                  <span className="text-xs text-gray-500 whitespace-nowrap">
                    {new Date(comment.createdAt).toLocaleDateString()}
                  </span>
                </div>

                {/* 액션 버튼들 */}
                <div className="flex items-center gap-1">
                  {/* 추천 버튼 */}
                  <Button
                    size="sm"
                    variant={comment.userLike ? "default" : "outline"}
                    onClick={() => onLikeComment(comment)}
                    className={`text-xs px-2 py-1 h-7 ${
                      comment.userLike
                        ? "bg-blue-500 hover:bg-blue-600 text-white"
                        : "hover:bg-blue-50 border-blue-300 text-blue-600"
                    }`}
                  >
                    <ThumbsUp
                      className={`w-3 h-3 mr-1 ${
                        comment.userLike ? "fill-current" : ""
                      }`}
                    />
                    {comment.likeCount}
                  </Button>

                  {/* 답글 버튼 */}
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => onReplyTo(comment)}
                    className="text-xs px-2 py-1 h-7 border-blue-300 text-blue-600 hover:bg-blue-50"
                  >
                    <MessageSquare className="w-3 h-3 mr-1" />
                    <span className="hidden sm:inline">답글</span>
                  </Button>

                  {/* 신고 버튼 (본인 댓글이 아닌 경우만) */}
                  {!isMyComment(comment) && !comment.deleted && (
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
                        <DropdownMenuItem
                          onClick={() => handleReport(comment)}
                          className="text-red-600 hover:text-red-700 cursor-pointer"
                        >
                          <Flag className="w-3 h-3 mr-2" />
                          신고
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  )}
                </div>
              </div>

              {/* 댓글 내용 */}
              <SafeHTML
                html={comment.content}
                className="prose max-w-none prose-sm text-sm sm:text-base leading-relaxed text-gray-700"
              />

              {/* 클릭 안내 */}
              <div className="mt-3 pt-2 border-t border-blue-100">
                <p className="text-xs text-blue-600 font-medium flex items-center gap-1">
                  원본 댓글로 이동하기
                </p>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* 신고 모달 */}
      <ReportModal
        isOpen={reportingCommentId !== null}
        onClose={() => setReportingCommentId(null)}
        onSubmit={() => {}}
        type="댓글"
      />
    </>
  );
};

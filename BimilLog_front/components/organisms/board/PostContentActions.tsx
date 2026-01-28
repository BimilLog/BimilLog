"use client";

import { Button, Tooltip } from "flowbite-react";
import { ThumbsUp, Flag } from "lucide-react";
import { Post } from "@/lib/api";
import { useAuth, useToast } from "@/hooks";
import { useState } from "react";
import { LazyReportModal } from "@/lib/utils/lazy-components";
import { submitReportAction } from "@/lib/actions/user";

interface PostContentActionsProps {
  post: Post;
  isAuthenticated: boolean;
  onLike: () => void;
}

export const PostContentActions: React.FC<PostContentActionsProps> = ({
  post,
  isAuthenticated,
  onLike,
}) => {
  const { user } = useAuth();
  const isOwnPost = user?.memberId === post.memberId;
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const { showFeedback, showError } = useToast();

  const handleReport = async (reason: string) => {
    try {
      // v2 신고 API 사용 - 익명 사용자도 신고 가능
      const response = await submitReportAction({
        reportType: "POST",
        targetId: post.id, // v2에서는 post.id 사용
        content: reason,
        reporterId: isAuthenticated && user?.memberId ? user.memberId : null,
        reporterName: isAuthenticated && user?.memberName ? user.memberName : "익명",
      });

      if (response.success) {
        showFeedback(
          "신고가 접수되었습니다",
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
    <>
      {/* 추천 및 신고 버튼 */}
      <div className="flex items-center justify-center gap-4 mt-8 pt-6 border-t">
        {!isAuthenticated ? (
          <Tooltip content="로그인이 필요합니다" placement="top">
            <Button
              onClick={onLike}
              color="light"
              disabled
              className="transition-colors duration-200 cursor-not-allowed"
            >
              <ThumbsUp className="w-4 h-4 mr-2 stroke-blue-500 fill-blue-100" />
              추천 {post.likeCount}
            </Button>
          </Tooltip>
        ) : (
          <Button
            onClick={onLike}
            color={post.liked ? "blue" : "light"}
            className={`transition-colors duration-200 ${post.liked ? 'ring-0 border-0' : ''}`}
          >
            <ThumbsUp
              className={`w-4 h-4 mr-2 ${post.liked ? "stroke-blue-500 fill-blue-500" : "stroke-blue-500 fill-blue-100"}`}
            />
            추천 {post.likeCount}
          </Button>
        )}

        {/* 신고 버튼 (자신의 글이 아닌 경우에만 표시) */}
        {!isOwnPost && (
          <Button
            onClick={() => setIsReportModalOpen(true)}
            color="red"
            className="transition-colors duration-200"
          >
            <Flag className="w-4 h-4 mr-2 stroke-red-500 fill-red-100" />
            신고
          </Button>
        )}
      </div>

      {/* 신고 모달 */}
      <LazyReportModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        onSubmit={handleReport}
        type="게시글"
      />
    </>
  );
};

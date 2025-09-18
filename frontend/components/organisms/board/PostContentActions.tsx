"use client";

import dynamic from "next/dynamic";
import { Button } from "flowbite-react";
import { Spinner } from "@/components";
import { ThumbsUp, Flag } from "lucide-react";
import { Post, userCommand } from "@/lib/api";
import { useAuth, useToast } from "@/hooks";
import { useState } from "react";

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
  const isOwnPost = user?.userId === post.userId;
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const { showFeedback, showError } = useToast();

  const handleReport = async (reason: string) => {
    try {
      // v2 신고 API 사용 - 익명 사용자도 신고 가능
      const response = await userCommand.submitReport({
        reportType: "POST",
        targetId: post.id, // v2에서는 post.id 사용
        content: reason,
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
        <Button
          onClick={onLike}
          color={post.isLiked ? "blue" : "light"}
          disabled={!isAuthenticated}
          className="transition-colors duration-200"
        >
          <ThumbsUp
            className={`w-4 h-4 mr-2 ${post.isLiked ? "fill-current" : ""}`}
          />
          추천 {post.likeCount}
        </Button>

        {/* 신고 버튼 (자신의 글이 아닌 경우에만 표시) */}
        {!isOwnPost && (
          <Button
            onClick={() => setIsReportModalOpen(true)}
            color="red"
            className="transition-colors duration-200"
          >
            <Flag className="w-4 h-4 mr-2" />
            신고
          </Button>
        )}
      </div>

      {/* 신고 모달 */}
      <ReportModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        onSubmit={handleReport}
        type="게시글"
      />
    </>
  );
};
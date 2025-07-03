import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import { SafeHTML } from "@/components/ui";
import { ThumbsUp, Flag } from "lucide-react";
import { Post, userApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { ReportModal } from "@/components/ui/ReportModal";
import { useState } from "react";
import { useToast } from "@/hooks/useToast";

interface PostContentProps {
  post: Post;
  isAuthenticated: boolean;
  onLike: () => void;
}

export const PostContent: React.FC<PostContentProps> = ({
  post,
  isAuthenticated,
  onLike,
}) => {
  const { user } = useAuth();
  const isOwnPost = user?.userId === post.userId;
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const { showSuccess, showError } = useToast();

  const handleReport = async () => {
    if (!isAuthenticated || !user) {
      showError("로그인 필요", "로그인이 필요한 기능입니다.");
      return;
    }

    try {
      const response = await userApi.submitSuggestion({
        reportType: "POST",
        userId: user.userId,
        targetId: post.postId,
        content: `게시글 신고: ${post.title}`,
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
    <CardContent className="p-6">
      <div className="prose max-w-none">
        <SafeHTML
          html={post.content}
          className="text-gray-800 leading-relaxed"
        />
      </div>

      {/* 추천 및 신고 버튼 */}
      <div className="flex items-center justify-center gap-4 mt-8 pt-6 border-t">
        <Button
          onClick={onLike}
          variant={post.userLike ? "default" : "outline"}
          className={`${
            post.userLike
              ? "bg-blue-500 hover:bg-blue-600 text-white"
              : "hover:bg-blue-50 border-blue-300 text-blue-600"
          } transition-colors duration-200`}
          disabled={!isAuthenticated}
        >
          <ThumbsUp
            className={`w-4 h-4 mr-2 ${post.userLike ? "fill-current" : ""}`}
          />
          추천 {post.likes}
        </Button>

        {/* 신고 버튼 (자신의 글이 아닌 경우에만 표시) */}
        {!isOwnPost && (
          <Button
            onClick={() => setIsReportModalOpen(true)}
            variant="outline"
            className="hover:bg-red-50 border-red-300 text-red-600 hover:text-red-700 transition-colors duration-200"
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
    </CardContent>
  );
};

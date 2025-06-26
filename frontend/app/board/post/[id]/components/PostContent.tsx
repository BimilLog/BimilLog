import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import { SafeHTML } from "@/components/ui";
import { ThumbsUp, Flag } from "lucide-react";
import { Post, userApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { ReportModal } from "@/components/ui/ReportModal";
import { useState } from "react";

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

  const handleReportSubmit = async (reportReason: string) => {
    try {
      const reportData: {
        reportType: "POST";
        targetId: number;
        content: string;
        userId?: number;
      } = {
        reportType: "POST",
        targetId: post.postId,
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
        onSubmit={handleReportSubmit}
        type="게시글"
      />
    </CardContent>
  );
};

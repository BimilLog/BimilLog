import { Button, CardContent, SafeHTML, ReportModal } from "@/components";
import { ThumbsUp, Flag } from "lucide-react";
import { Post, userApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
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

  const handleReport = async (reason: string) => {
    try {
      // v2 신고 API 사용 - 익명 사용자도 신고 가능
      const response = await userApi.submitReport({
        reportType: "POST",
        targetId: post.id, // v2에서는 post.id 사용
        content: reason,
      });

      if (response.success) {
        showSuccess(
          "신고 접수",
          "신고가 접수되었습니다. 검토 후 적절한 조치를 취하겠습니다."
        );
        setIsReportModalOpen(false);
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

  return (
    <CardContent className="p-6">
      <div className="prose max-w-none">
        <SafeHTML
          html={post.content}
          className="text-gray-800 leading-relaxed whitespace-pre-wrap"
        />
      </div>

      {/* 추천 및 신고 버튼 */}
      <div className="flex items-center justify-center gap-4 mt-8 pt-6 border-t">
        <Button
          onClick={onLike}
          variant={post.isLiked ? "default" : "outline"}
          className={`${
            post.isLiked
              ? "bg-blue-500 hover:bg-blue-600 text-white"
              : "hover:bg-blue-50 border-blue-300 text-blue-600"
          } transition-colors duration-200`}
          disabled={!isAuthenticated}
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

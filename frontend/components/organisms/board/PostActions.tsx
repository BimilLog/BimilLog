import { memo } from "react";
import { Edit, Trash2, Megaphone, Loader2 } from "lucide-react";
import { Button } from "@/components";
import { Tooltip } from "flowbite-react";
import type { Post } from "@/lib/api";
import { useAuth } from "@/hooks";
import { useToggleNotice } from "@/hooks/api/usePostMutations";

interface PostActionsProps {
  post: Post;
  canModify: boolean;
  onDeletePost: () => void;
}

/**
 * 게시글 액션 컴포넌트 (수정/삭제/공지사항 토글)
 * PostDetailClient에서 분리된 액션 관련 컴포넌트
 */
const PostActions = memo(({
  post,
  canModify,
  onDeletePost,
}: PostActionsProps) => {
  const { user } = useAuth();
  const { mutate: toggleNotice, isPending: isTogglingNotice } = useToggleNotice();
  const isAdmin = user?.role === 'ADMIN';

  const handleToggleNotice = () => {
    if (isTogglingNotice) return; // 이중 클릭 방지
    toggleNotice(post.id);
  };

  return (
    <div className="flex items-center justify-end px-6 pb-6">
      <div className="flex items-center space-x-2">
        {/* 공지사항 토글 버튼 - 관리자 전용 */}
        {isAdmin && (
          <Tooltip
            content={post.isNotice ? "공지사항을 일반 게시글로 변경합니다" : "게시글을 공지사항으로 등록합니다"}
            placement="top"
          >
            <Button
              variant={post.isNotice ? "default" : "outline"}
              size="sm"
              onClick={handleToggleNotice}
              disabled={isTogglingNotice}
              className="flex items-center space-x-1"
              aria-label={post.isNotice ? "공지사항 해제" : "공지사항으로 설정"}
              role="button"
            >
              {isTogglingNotice ? (
                <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
              ) : (
                <Megaphone className="w-4 h-4" aria-hidden="true" />
              )}
              <span>{post.isNotice ? "공지 해제" : "공지 설정"}</span>
            </Button>
          </Tooltip>
        )}

        {/* 수정/삭제 버튼 - 작성자 본인이거나 관리자인 경우만 표시 */}
        {canModify && (
          <>
            <Button
              variant="outline"
              size="sm"
              asChild
              className="flex items-center space-x-1"
            >
              {/* 수정 페이지로 이동 - 기존 내용을 불러와서 수정 가능 */}
              <a href={`/board/post/${post.id}/edit`}>
                <Edit className="w-4 h-4" />
                <span>수정</span>
              </a>
            </Button>

            <Button
              variant="outline"
              size="sm"
              onClick={onDeletePost} // 비밀번호 모달을 열어 비밀번호 확인 후 삭제 진행
              className="flex items-center space-x-1 text-red-600 hover:text-red-700 hover:bg-red-50 border-red-200"
            >
              <Trash2 className="w-4 h-4" />
              <span>삭제</span>
            </Button>
          </>
        )}
      </div>
    </div>
  );
});

PostActions.displayName = "PostActions";

export { PostActions };
import { memo } from "react";
import { Edit, Trash2, Megaphone, Loader2 } from "lucide-react";
import { Button } from "@/components";
import { Tooltip } from "flowbite-react";
import type { Post } from "@/lib/api";
import { useAuth } from "@/hooks";
import { useToggleNoticeAction } from "@/hooks/actions/usePostActions";

interface PostActionsProps {
  post: Post;
  canModify: boolean;
  onDeletePost: () => void;
}

/**
 * 게시글 액션 컴포넌트 (수정/삭제/공지사항 토글)
 * PostDetailClient에서 분리된 액션 관련 컴포넌트
 * 공지 상태는 FeaturedPost 테이블로 관리되어 Post.isNotice 필드가 제거됨
 * 관리자가 토글 시 서버에서 현재 상태를 확인하고 토글함
 */
const PostActions = memo(({
  post,
  canModify,
  onDeletePost,
}: PostActionsProps) => {
  const { user } = useAuth();
  const { toggleNotice, isPending: isTogglingNotice } = useToggleNoticeAction();
  const isAdmin = user?.role === 'ADMIN';

  const handleToggleNotice = () => {
    if (isTogglingNotice) return; // 이중 클릭 방지
    toggleNotice(post.id, post.notice);
  };

  return (
    <div className="flex items-center justify-end px-6 pb-6">
      <div className="flex items-center space-x-2">
        {/* 공지사항 토글 버튼 - 관리자 전용 */}
        {/* 서버에서 현재 공지 상태를 확인하고 토글 (FeaturedPost 테이블 기반) */}
        {isAdmin && (
          <Tooltip
            content="공지사항 설정/해제를 토글합니다"
            placement="top"
          >
            <Button
              variant="outline"
              size="sm"
              onClick={handleToggleNotice}
              disabled={isTogglingNotice}
              className="flex items-center space-x-1"
              aria-label="공지사항 토글"
              role="button"
            >
              {isTogglingNotice ? (
                <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
              ) : (
                <Megaphone className="w-4 h-4" aria-hidden="true" />
              )}
              <span>공지 토글</span>
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

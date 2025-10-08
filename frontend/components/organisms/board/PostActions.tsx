import { memo } from "react";
import { Edit, Trash2, Pin, PinOff } from "lucide-react";
import { Button } from "@/components";
import { useAdmin } from "@/hooks";
import { useToggleNotice } from "@/hooks/api/usePostMutations";
import type { Post } from "@/lib/api";

interface PostActionsProps {
  post: Post;
  canModify: boolean;
  onDeletePost: () => void;
}

/**
 * 게시글 액션 컴포넌트 (수정/삭제/공지토글)
 * PostDetailClient에서 분리된 액션 관련 컴포넌트
 */
const PostActions = memo(({
  post,
  canModify,
  onDeletePost,
}: PostActionsProps) => {
  const { isAdmin } = useAdmin();
  const { mutate: toggleNotice, isPending: isTogglingNotice } = useToggleNotice();

  const handleToggleNotice = () => {
    if (isTogglingNotice) return;
    toggleNotice(post.id);
  };

  const isNotice = post.postCacheFlag === 'NOTICE';

  return (
    <div className="flex items-center justify-between px-6 pb-6">
      {/* 관리자 전용: 공지사항 토글 버튼 */}
      {isAdmin && (
        <Button
          variant="outline"
          size="sm"
          onClick={handleToggleNotice}
          disabled={isTogglingNotice}
          className={`flex items-center space-x-1 ${
            isNotice
              ? 'text-purple-600 hover:text-purple-700 hover:bg-purple-50 border-purple-200'
              : 'text-gray-600 hover:text-gray-700 hover:bg-gray-50'
          }`}
        >
          {isNotice ? <PinOff className="w-4 h-4" /> : <Pin className="w-4 h-4" />}
          <span>{isNotice ? '공지 해제' : '공지 설정'}</span>
        </Button>
      )}

      {/* 수정/삭제 버튼 - 작성자 본인이거나 관리자인 경우만 표시 */}
      {canModify && (
        <div className="flex items-center space-x-2">
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
        </div>
      )}
    </div>
  );
});

PostActions.displayName = "PostActions";

export { PostActions };
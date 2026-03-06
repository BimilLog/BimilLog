"use client";
import { useCallback } from "react";

import { Card } from "@/components";
import { AuthHeader } from "@/components/organisms/common";
import {
  ResponsiveAdFitBanner,
  AdFitBanner,
  AD_SIZES,
  getAdUnit,
  Breadcrumb,
} from "@/components";
import { Post } from "@/lib/api";

// 분리된 컴포넌트들 import
import { PostHeader } from "./PostHeader";
import { PostContent } from "./PostContent";
import { PostActions } from "./PostActions";
import { CommentSection } from "./CommentSection";
import { PasswordModal } from "./PasswordModal";
import { PostDetailSkeleton } from "./PostDetailSkeleton";
import { DeleteConfirmModal } from "@/components/molecules/modals/DeleteConfirmModal";

// 분리된 훅들 import
import { usePostDetail, useCommentInteraction } from "@/hooks/features";
import { useReadingProgress } from "@/hooks/features/useReadingProgress";
import { useAuth } from "@/hooks";

// Server Action hooks (브라우저에서 백엔드 직접 호출 방지)
import { useLikePostAction, useDeletePostAction } from "@/hooks/actions/usePostActions";
import {
  useLikeCommentAction,
  useCreateCommentAction,
  useUpdateCommentAction,
  useDeleteCommentAction,
} from "@/hooks/actions/useCommentActions";

interface Props {
  initialPost: Post;
  postId: string;
}

export default function PostDetailClient({ initialPost, postId }: Props) {
  // 인증 상태 가져오기
  const { isAuthenticated } = useAuth();

  // 읽기 진행률 트래킹
  const { progress } = useReadingProgress({
    postId: parseInt(postId),
    autoTrack: true,
  });

  // 게시글 상세 데이터 관리
  const {
    post,
    comments,
    popularComments,
    loading,
    hasMoreComments,
    isLoadingMore,
    showPasswordModal,
    modalPassword,
    passwordModalTitle,
    deleteMode,
    targetComment,
    loadMoreComments,
    getTotalCommentCount,
    getRootCommentCount,
    canModify,
    isMyComment,
    canModifyComment,
    openPasswordModal,
    resetPasswordModal,
    setModalPassword,
  } = usePostDetail(postId, initialPost);

  // Server Action hooks (브라우저에서 백엔드 직접 호출 방지)
  const { likePost } = useLikePostAction();
  const { deletePost, isPending: isDeletingPost } = useDeletePostAction();
  const { createComment, isPending: isCreatingComment } = useCreateCommentAction();
  const { updateComment, isPending: isUpdatingComment } = useUpdateCommentAction();
  const { deleteComment, isPending: isDeletingComment } = useDeleteCommentAction();
  const { likeComment } = useLikeCommentAction(Number(postId));

  // 댓글 편집/답글/삭제 상태 및 핸들러
  const {
    commentHandlers, editState, replyState,
    showDeleteModal, showCommentDeleteModal, targetDeleteComment,
    passwordError,
    handleCommentSubmitForSection,
    handleLikePost, handleDeletePostClick, handleConfirmDelete,
    handleConfirmCommentDelete,
    handlePasswordSubmit,
    setShowDeleteModal, setShowCommentDeleteModal,
    setTargetDeleteComment, setPasswordError,
  } = useCommentInteraction({
    postId,
    post,
    isAuthenticated,
    canModify,
    isMyComment,
    canModifyComment,
    openPasswordModal,
    resetPasswordModal,
    modalPassword,
    deleteMode,
    targetComment,
    createComment,
    updateComment,
    deleteComment,
    deletePost,
    likePost,
    likeComment,
  });

  // 인기 댓글 → 원본 댓글 스크롤 이동 핸들러
  const handleCommentClick = useCallback((commentId: number) => {
    const element = document.getElementById(`comment-${commentId}`);
    if (element) {
      // 부모 댓글 자동 펼치기 (대댓글인 경우)
      const clickedComment = comments.find(c => c.id === commentId);
      if (clickedComment?.parentId) {
        const toggleButton = document.querySelector(`#comment-${clickedComment.parentId} button[class*="답글"]`);
        if (toggleButton && toggleButton.textContent?.includes('더보기')) {
          (toggleButton as HTMLButtonElement).click();
        }
      }

      element.scrollIntoView({ behavior: 'smooth', block: 'center' });

      const commentContent = element.querySelector('.comment-content');
      if (commentContent) {
        commentContent.classList.add('bg-yellow-100', 'ring-2', 'ring-yellow-400');
        setTimeout(() => {
          commentContent.classList.remove('bg-yellow-100', 'ring-2', 'ring-yellow-400');
        }, 2500);
      }
    }
  }, [comments]);

  // 로딩 상태
  if (loading) {
    return <PostDetailSkeleton />;
  }

  // 게시글이 없는 경우
  if (!post) {
    return (
      <div className="min-h-screen bg-brand-gradient flex items-center justify-center">
        <p>게시글을 찾을 수 없습니다.</p>
      </div>
    );
  }

  const commentCount = getTotalCommentCount(comments);
  const rootCommentCount = getRootCommentCount(comments);

  return (
    <div className="min-h-screen bg-brand-gradient">
      {/* 읽기 진행률 바 */}
      {progress > 0 && (
        <div className="fixed top-0 left-0 right-0 z-[60] h-1 bg-gray-200">
          <div
            className="h-full bg-gradient-to-r from-purple-500 to-pink-500 transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      )}

      <AuthHeader />

      {/* Top Banner Advertisement */}
      <div className="container mx-auto px-4 py-2">
        <div className="flex justify-center">
          <ResponsiveAdFitBanner
            position="게시글 상세 상단"
            className="max-w-full"
          />
        </div>
      </div>

      <div className="container mx-auto px-4 py-8">
        <div className="mb-4">
          <Breadcrumb
            items={[
              { title: "홈", href: "/" },
              { title: "커뮤니티", href: "/board" },
              {
                title: post.title,
                href: `/board/post/${post.id}`,
              },
            ]}
          />
        </div>

        {/* 게시글 카드 */}
        <Card variant="elevated" className="mb-8">
          <PostHeader
            post={post}
            commentCount={commentCount}
          />
          <PostContent
            post={post}
            isAuthenticated={isAuthenticated}
            onLike={handleLikePost}
          />
          <PostActions
            post={post}
            canModify={canModify()}
            onDeletePost={handleDeletePostClick}
          />
        </Card>

        {/* 댓글 섹션 */}
        <CommentSection
          postId={post.id}
          comments={comments}
          popularComments={popularComments}
          commentCount={commentCount}
          rootCommentCount={rootCommentCount}
          isAuthenticated={isAuthenticated}

          hasMoreComments={hasMoreComments}
          isLoadingMore={isLoadingMore}

          isSubmittingComment={isCreatingComment}
          onSubmitComment={handleCommentSubmitForSection}

          handlers={commentHandlers}
          editState={editState}
          replyState={replyState}

          isSubmittingReply={isCreatingComment}
          isUpdatingComment={isUpdatingComment}
          onLoadMore={loadMoreComments}

          onCommentClick={handleCommentClick}
        />

        {/* Mobile Advertisement */}
        <div className="mt-8 mb-6">
          <div className="flex justify-center px-2">
            {getAdUnit("MOBILE_BANNER") && (
              <AdFitBanner
                adUnit={getAdUnit("MOBILE_BANNER")!}
                width={AD_SIZES.BANNER_320x50.width}
                height={AD_SIZES.BANNER_320x50.height}
              />
            )}
          </div>
        </div>

        {/* 비밀번호 모달 */}
        <PasswordModal
          isOpen={showPasswordModal}
          password={modalPassword}
          onPasswordChange={setModalPassword}
          onConfirm={handlePasswordSubmit}
          onCancel={() => {
            resetPasswordModal();
            setPasswordError("");
          }}
          title={passwordModalTitle}
          description={
            deleteMode === "comment" && targetComment?.replies && targetComment.replies.length > 0
              ? `이 댓글에는 ${targetComment.replies.length}개의 답글이 있습니다. 삭제하면 '삭제된 댓글입니다'로 표시됩니다.`
              : undefined
          }
          error={passwordError}
          isLoading={deleteMode === "post" ? isDeletingPost : isDeletingComment}
        />

        {/* 게시글 삭제 확인 모달 */}
        <DeleteConfirmModal
          isOpen={showDeleteModal}
          onClose={() => setShowDeleteModal(false)}
          onConfirm={handleConfirmDelete}
          title="게시글을 삭제하시겠습니까?"
          message="이 작업은 되돌릴 수 없습니다. 게시글과 모든 댓글이 삭제됩니다."
          confirmText="삭제"
          cancelText="취소"
          isLoading={isDeletingPost}
        />

        {/* 댓글 삭제 확인 모달 */}
        <DeleteConfirmModal
          isOpen={showCommentDeleteModal}
          onClose={() => {
            setShowCommentDeleteModal(false);
            setTargetDeleteComment(null);
          }}
          onConfirm={handleConfirmCommentDelete}
          title="댓글을 삭제하시겠습니까?"
          message={
            targetDeleteComment?.replies && targetDeleteComment.replies.length > 0
              ? `이 댓글에는 ${targetDeleteComment.replies.length}개의 답글이 있습니다. 삭제하면 '삭제된 댓글입니다'로 표시됩니다.`
              : "이 작업은 되돌릴 수 없습니다. 댓글이 완전히 삭제됩니다."
          }
          confirmText="삭제"
          cancelText="취소"
          isLoading={isDeletingComment}
        />
      </div>
    </div>
  );
}

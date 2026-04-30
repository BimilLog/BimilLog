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
  // 라운드 8 B-8-006: isPending 노출하여 더블 클릭/스피너 처리.
  const { likePost, isPending: isLikingPost } = useLikePostAction();
  const { deletePost, isPending: isDeletingPost } = useDeletePostAction();
  const { createComment, isPending: isCreatingComment } = useCreateCommentAction();
  const { updateComment, isPending: isUpdatingComment } = useUpdateCommentAction();
  const { deleteComment, isPending: isDeletingComment } = useDeleteCommentAction();
  // 라운드 8 B-8-001: 댓글 좋아요는 옵티미스틱 토글로 즉시 반영되므로
  //   isPending 시각화는 본 라운드 스코프에서는 불필요. 향후 disabled 가 필요하면 재추가.
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
    ConfirmModalComponent,
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
  // 라운드 8 B-8-003: 한국어 카피/클래스명 의존 제거 — data-replies-toggle / data-expanded 사용.
  // 라운드 8: 강조 색상은 paper/seal-gold 토큰으로 통일 + 스크린리더에 announce.
  const handleCommentClick = useCallback((commentId: number) => {
    const element = document.getElementById(`comment-${commentId}`);
    if (!element) return;

    // 부모 댓글 자동 펼치기 (대댓글인 경우): data-* 속성 기반 안전 조회
    const clickedComment = comments.find(c => c.id === commentId);
    if (clickedComment?.parentId) {
      const parentEl = document.getElementById(`comment-${clickedComment.parentId}`);
      const toggleButton = parentEl?.querySelector<HTMLButtonElement>(
        'button[data-replies-toggle="true"]'
      );
      if (toggleButton && toggleButton.dataset.expanded === "false") {
        toggleButton.click();
      }
    }

    element.scrollIntoView({ behavior: 'smooth', block: 'center' });

    const commentContent = element.querySelector('.comment-content');
    if (commentContent) {
      commentContent.classList.add('ring-2', 'ring-seal-gold', 'bg-seal-gold/15');
      // 스크린리더에 결과 안내
      element.setAttribute('tabindex', '-1');
      (element as HTMLElement).focus({ preventScroll: true });
      setTimeout(() => {
        commentContent.classList.remove('ring-2', 'ring-seal-gold', 'bg-seal-gold/15');
        element.removeAttribute('tabindex');
      }, 2500);
    }
  }, [comments]);

  // 로딩 상태
  if (loading) {
    return <PostDetailSkeleton />;
  }

  // 게시글이 없는 경우
  if (!post) {
    return (
      <div className="min-h-screen bg-paper flex items-center justify-center">
        <p className="text-ink">게시글을 찾을 수 없습니다.</p>
      </div>
    );
  }

  // 라운드 8 B-8-010: PostHeader 는 서버 총합(post.commentCount) 사용.
  //   CommentList 는 현재 로드된 댓글 기준 카운트 노출.
  const loadedCommentCount = getTotalCommentCount(comments);
  const rootCommentCount = getRootCommentCount(comments);
  const totalCommentCount = post?.commentCount ?? loadedCommentCount;

  return (
    <div className="min-h-screen bg-paper">
      {/* 읽기 진행률 바 — 라운드 8: progressbar role + 장식적 내부 div 는 aria-hidden */}
      {progress > 0 && (
        <div
          className="fixed top-0 left-0 right-0 z-[60] h-1 bg-paper-200 dark:bg-postal-navy/40"
          role="progressbar"
          aria-valuenow={Math.round(progress)}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label="읽기 진행률"
        >
          <div
            aria-hidden="true"
            className="h-full bg-stamp-red transition-all duration-300"
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
            commentCount={totalCommentCount}
          />
          <PostContent
            post={post}
            isAuthenticated={isAuthenticated}
            onLike={handleLikePost}
            isLiking={isLikingPost}
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
          commentCount={loadedCommentCount}
          rootCommentCount={rootCommentCount}
          totalCommentCount={totalCommentCount}
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

        {/* 라운드 8 B-8-008: 댓글 수정 취소용 인앱 ConfirmModal */}
        <ConfirmModalComponent />
      </div>
    </div>
  );
}

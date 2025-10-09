"use client";

import { useState } from "react";
import { Card } from "@/components";
import { AuthHeader } from "@/components/organisms/common";
import {
  ResponsiveAdFitBanner,
  AdFitBanner,
  AD_SIZES,
  getAdUnit,
  Breadcrumb,
} from "@/components";
import { Post, Comment } from "@/lib/api";

// 분리된 컴포넌트들 import
import { PostHeader } from "./PostHeader";
import { PostContent } from "./PostContent";
import { PostActions } from "./PostActions";
import { CommentSection } from "./CommentSection";
import { PasswordModal } from "./PasswordModal";
import { PostDetailSkeleton } from "./PostDetailSkeleton";
import { DeleteConfirmModal } from "@/components/molecules/modals/DeleteConfirmModal";

// 분리된 훅들 import
import { usePostDetail } from "@/hooks/features";
import { useReadingProgress } from "@/hooks/features/useReadingProgress";
import { useAuth, useToast } from "@/hooks";

// TanStack Query mutation hooks
import { useLikePost, useDeletePost } from "@/hooks/api/usePostMutations";
import {
  useCreateComment,
  useUpdateComment,
  useDeleteComment,
  useLikeCommentOptimized,
} from "@/hooks/api/useCommentMutations";

interface Props {
  initialPost: Post;
  postId: string;
}

export default function PostDetailClient({ initialPost, postId }: Props) {
  // 인증 상태 가져오기
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();

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

  // TanStack Query mutation hooks
  const { mutate: likePost } = useLikePost();
  const { mutate: deletePost, isPending: isDeletingPost } = useDeletePost();
  const { mutate: createComment, isPending: isCreatingComment } = useCreateComment();
  const { mutate: updateComment, isPending: isUpdatingComment } = useUpdateComment();
  const { mutate: deleteComment, isPending: isDeletingComment } = useDeleteComment();
  const { mutate: likeComment } = useLikeCommentOptimized(Number(postId));

  // 댓글 편집 및 답글 상태 관리
  const [editingComment, setEditingComment] = useState<Comment | null>(null);
  const [editContent, setEditContent] = useState("");
  const [editPassword, setEditPassword] = useState("");
  const [replyingTo, setReplyingTo] = useState<Comment | null>(null);
  const [replyContent, setReplyContent] = useState("");
  const [replyPassword, setReplyPassword] = useState("");

  // 삭제 확인 모달 상태 관리
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showCommentDeleteModal, setShowCommentDeleteModal] = useState(false);
  const [targetDeleteComment, setTargetDeleteComment] = useState<Comment | null>(null);

  // 비밀번호 모달 에러 상태
  const [passwordError, setPasswordError] = useState("");

  // 댓글 작성 핸들러
  const handleCommentSubmitForSection = (content: string, password: string) => {
    createComment(
      {
        postId: Number(postId),
        content,
        password: password ? Number(password) : undefined,
      },
      {
        onSuccess: () => {
          // 성공 시 추가 작업 (TanStack Query가 자동으로 캐시 무효화)
        },
      }
    );
  };

  // 댓글 수정 상태 관리 - 수정 모드 진입 시 기존 내용을 대입
  const handleEditComment = (comment: Comment) => {
    setEditingComment(comment);
    setEditContent(comment.content); // 기존 내용을 편집 필드에 설정
    setEditPassword(""); // 비밀번호는 매번 새로 입력
  };

  // 댓글 수정 완료 후 상태 초기화
  const handleUpdateComment = async () => {
    if (!editingComment) return;

    // 내용 검증
    const trimmedContent = editContent.trim();
    if (!trimmedContent) {
      showToast({ type: 'error', message: '댓글 내용을 입력해주세요.' });
      return;
    }
    if (trimmedContent.length > 255) {
      showToast({ type: 'error', message: '댓글은 최대 255자까지 입력 가능합니다.' });
      return;
    }

    // 익명 댓글 수정 시 비밀번호 검증
    const isAnonymous = editingComment.memberName === "익명" || editingComment.memberName === null;
    if (isAnonymous) {
      if (!editPassword) {
        showToast({ type: 'error', message: '비밀번호를 입력해주세요.' });
        return;
      }
      const passwordNum = Number(editPassword);
      if (isNaN(passwordNum) || passwordNum < 1000 || passwordNum > 9999) {
        showToast({ type: 'error', message: '비밀번호는 4자리 숫자여야 합니다.' });
        return;
      }
    }

    updateComment(
      {
        commentId: editingComment.id,
        postId: Number(postId),
        content: trimmedContent,
        password: editPassword ? Number(editPassword) : undefined,
      },
      {
        onSuccess: () => {
          // 수정 완료 후 상태 초기화
          setEditingComment(null);
          setEditContent("");
          setEditPassword("");
        },
        onError: () => {
          // 에러 발생 시에도 상태 초기화하지 않음 (사용자가 다시 시도할 수 있도록)
          // 토스트 메시지는 mutation hook에서 처리됨
        },
      }
    );
  };

  const handleCancelEdit = () => {
    // 내용이 변경되었으면 확인
    if (editingComment && editContent !== editingComment.content) {
      const confirmed = window.confirm("수정 중인 내용이 있습니다. 취소하시겠습니까?");
      if (!confirmed) {
        return;
      }
    }
    setEditingComment(null);
    setEditContent("");
    setEditPassword("");
  };

  // 댓글 답글 상태 관리 - 특정 댓글에 답글 작성 모드
  const handleReplyTo = (comment: Comment) => {
    setReplyingTo(comment); // 답글 대상 설정
    setReplyContent("");   // 답글 내용 초기화
    setReplyPassword(""); // 답글 비밀번호 초기화
  };

  const handleCancelReply = () => {
    setReplyingTo(null);
    setReplyContent("");
    setReplyPassword("");
  };

  // 답글 작성 완료 - parentId로 replyingTo.id 전달
  const handleSubmitReply = async () => {
    if (!replyingTo) return;

    createComment(
      {
        postId: Number(postId),
        content: replyContent,
        parentId: replyingTo.id,
        password: replyPassword ? Number(replyPassword) : undefined,
      },
      {
        onSuccess: () => {
          // 답글 작성 완료 후 답글 상태 초기화
          setReplyingTo(null);
          setReplyContent("");
          setReplyPassword("");
        },
      }
    );
  };

  const handleUpdateEditContent = (content: string) => {
    setEditContent(content);
  };

  const handleUpdateEditPassword = (password: string) => {
    setEditPassword(password);
  };

  const handleUpdateReplyContent = (content: string) => {
    setReplyContent(content);
  };

  const handleUpdateReplyPassword = (password: string) => {
    setReplyPassword(password);
  };

  // 게시글 좋아요 핸들러
  const handleLikePost = () => {
    if (!isAuthenticated) {
      showToast({ type: "warning", message: "로그인이 필요합니다." });
      return;
    }
    likePost(Number(postId));
  };

  // 게시글 삭제 클릭 핸들러
  const handleDeletePostClick = () => {
    if (!canModify()) {
      showToast({ type: "error", message: "삭제 권한이 없습니다." });
      return;
    }

    if (post?.memberName === "익명" || post?.memberName === null) {
      // 익명 게시글의 경우 비밀번호 모달
      openPasswordModal("게시글 삭제", "post");
    } else {
      // 로그인 사용자의 경우 삭제 확인 모달
      setShowDeleteModal(true);
    }
  };

  // 삭제 확인 후 실제 삭제 실행
  const handleConfirmDelete = async () => {
    deletePost({ postId: Number(postId) }, {
      onSuccess: () => {
        setShowDeleteModal(false);
      },
      onError: () => {
        // 에러 발생 시 모달 닫기 (토스트는 mutation hook에서 처리)
        setShowDeleteModal(false);
      },
    });
  };

  // 댓글 삭제 핸들러
  const handleDeleteComment = (comment: Comment) => {
    if (!canModifyComment(comment)) {
      showToast({ type: "error", message: "삭제 권한이 없습니다." });
      return;
    }

    if (comment.memberName === "익명" || comment.memberName === null) {
      // 익명 댓글의 경우 비밀번호 모달
      openPasswordModal("댓글 삭제", "comment", comment);
    } else {
      // 로그인 사용자 댓글: 확인 모달 표시
      setTargetDeleteComment(comment);
      setShowCommentDeleteModal(true);
    }
  };

  // 댓글 삭제 확인 후 실제 삭제 실행
  const handleConfirmCommentDelete = async () => {
    if (!targetDeleteComment) return;

    deleteComment({
      commentId: targetDeleteComment.id,
      postId: Number(postId),
    }, {
      onSuccess: () => {
        setShowCommentDeleteModal(false);
        setTargetDeleteComment(null);
      },
      onError: () => {
        // 에러 발생 시 모달 닫기 (토스트는 mutation hook에서 처리)
        setShowCommentDeleteModal(false);
        setTargetDeleteComment(null);
      },
    });
  };

  // 댓글 좋아요 핸들러
  const handleLikeComment = (comment: Comment) => {
    if (!isAuthenticated) {
      showToast({ type: "warning", message: "로그인이 필요합니다." });
      return;
    }
    likeComment(comment.id);
  };

  // 비밀번호 모달 제출 - 게시글/댓글 삭제 모드에 따라 분기 처리
  const handlePasswordSubmit = async () => {
    // 에러 초기화
    setPasswordError("");

    if (deleteMode === "post") {
      deletePost({
        postId: Number(postId),
        password: modalPassword ? Number(modalPassword) : undefined,
      }, {
        onSuccess: () => {
          // 삭제 성공 시 모달 상태 초기화
          resetPasswordModal();
          setPasswordError("");
        },
        onError: (error: unknown) => {
          // 에러 발생 시 에러 메시지 표시 (모달은 닫지 않음)
          const errorMessage =
            (error && typeof error === 'object' && 'error' in error && typeof error.error === 'string')
              ? error.error
              : (error instanceof Error)
                ? error.message
                : "비밀번호가 올바르지 않습니다.";
          setPasswordError(errorMessage);
        },
      });
    } else if (deleteMode === "comment" && targetComment) {
      deleteComment({
        commentId: targetComment.id,
        postId: Number(postId),
        password: modalPassword ? Number(modalPassword) : undefined,
      }, {
        onSuccess: () => {
          // 삭제 성공 시 모달 상태 초기화
          resetPasswordModal();
          setPasswordError("");
        },
        onError: (error: unknown) => {
          // 에러 발생 시 에러 메시지 표시 (모달은 닫지 않음)
          const errorMessage =
            (error && typeof error === 'object' && 'error' in error && typeof error.error === 'string')
              ? error.error
              : (error instanceof Error)
                ? error.message
                : "비밀번호가 올바르지 않습니다.";
          setPasswordError(errorMessage);
        },
      });
    }
  };

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

          editingComment={editingComment}
          editContent={editContent}
          editPassword={editPassword}
          replyingTo={replyingTo}
          replyContent={replyContent}
          replyPassword={replyPassword}
          isSubmittingReply={isCreatingComment}
          isUpdatingComment={isUpdatingComment}

          onEditComment={handleEditComment}
          onUpdateComment={handleUpdateComment}
          onCancelEdit={handleCancelEdit}
          onDeleteComment={handleDeleteComment}
          onReplyTo={handleReplyTo}
          onReplySubmit={handleSubmitReply}
          onCancelReply={handleCancelReply}
          onLikeComment={handleLikeComment}
          onLoadMore={loadMoreComments}

          setEditContent={handleUpdateEditContent}
          setEditPassword={handleUpdateEditPassword}
          setReplyContent={handleUpdateReplyContent}
          setReplyPassword={handleUpdateReplyPassword}

          isMyComment={isMyComment}
          canModifyComment={canModifyComment}
          onCommentClick={(commentId) => {
            // 인기 댓글에서 원본 댓글로 이동 - 댓글 ID로 DOM 요소를 찾아 스크롤 이동
            const element = document.getElementById(`comment-${commentId}`);
            if (element) {
              // 부모 댓글 자동 펼치기 (대댓글인 경우)
              const clickedComment = comments.find(c => c.id === commentId);
              if (clickedComment?.parentId) {
                // 답글인 경우, 부모 댓글 내부의 "답글 더보기" 버튼 클릭 (접혀있는 경우)
                const toggleButton = document.querySelector(`#comment-${clickedComment.parentId} button[class*="답글"]`);
                if (toggleButton && toggleButton.textContent?.includes('더보기')) {
                  (toggleButton as HTMLButtonElement).click();
                }
              }

              // 스크롤 이동
              element.scrollIntoView({ behavior: 'smooth', block: 'center' });

              // 하이라이트 효과 추가
              const commentContent = element.querySelector('.comment-content');
              if (commentContent) {
                commentContent.classList.add('bg-yellow-100', 'ring-2', 'ring-yellow-400');

                // 2.5초 후 하이라이트 제거
                setTimeout(() => {
                  commentContent.classList.remove('bg-yellow-100', 'ring-2', 'ring-yellow-400');
                }, 2500);
              }
            }
          }}
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
          message="이 작업은 되돌릴 수 없습니다."
          confirmText="삭제"
          cancelText="취소"
          isLoading={isDeletingComment}
        />
      </div>
    </div>
  );
}

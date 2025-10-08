"use client";

import { useState } from "react";
import { Card } from "@/components";
import { Spinner as FlowbiteSpinner } from "flowbite-react";
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
    canModify,
    isMyComment,
    canModifyComment,
    openPasswordModal,
    resetPasswordModal,
    setModalPassword,
  } = usePostDetail(postId, initialPost);

  // TanStack Query mutation hooks
  const { mutate: likePost } = useLikePost();
  const { mutate: deletePost } = useDeletePost();
  const { mutate: createComment, isPending: isCreatingComment } = useCreateComment();
  const { mutate: updateComment } = useUpdateComment();
  const { mutate: deleteComment } = useDeleteComment();
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

    updateComment(
      {
        commentId: editingComment.id,
        content: editContent,
        password: editPassword ? Number(editPassword) : undefined,
      },
      {
        onSuccess: () => {
          // 수정 완료 후 상태 초기화
          setEditingComment(null);
          setEditContent("");
          setEditPassword("");
        },
      }
    );
  };

  const handleCancelEdit = () => {
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
    setShowDeleteModal(false);
    deletePost({ postId: Number(postId) });
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
      // 로그인 사용자 댓글은 바로 삭제
      deleteComment({
        commentId: comment.id,
      });
    }
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
    if (deleteMode === "post") {
      deletePost({
        postId: Number(postId),
        password: modalPassword ? Number(modalPassword) : undefined,
      }, {
        onSuccess: () => {
          // 삭제 성공 시 모달 상태 초기화
          resetPasswordModal();
        },
      });
    } else if (deleteMode === "comment" && targetComment) {
      deleteComment({
        commentId: targetComment.id,
        password: modalPassword ? Number(modalPassword) : undefined,
      }, {
        onSuccess: () => {
          // 삭제 성공 시 모달 상태 초기화
          resetPasswordModal();
        },
      });
    }
  };

  // 로딩 상태
  if (loading) {
    return (
      <div className="min-h-screen bg-brand-gradient flex items-center justify-center">
        <FlowbiteSpinner color="pink" size="xl" aria-label="Loading..." />
      </div>
    );
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
            canModify={canModify}
            onDeleteClick={handleDeletePostClick}
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
              element.scrollIntoView({ behavior: 'smooth', block: 'center' });
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
          onCancel={resetPasswordModal}
          title={passwordModalTitle}
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
        />
      </div>
    </div>
  );
}

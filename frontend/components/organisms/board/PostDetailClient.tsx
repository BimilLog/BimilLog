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
import { usePostDetail, usePostActions, useCommentActions } from "@/hooks/features";
import { useReadingProgress } from "@/hooks/features/useReadingProgress";
import { useAuth } from "@/hooks";

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
    showPasswordModal,
    modalPassword,
    passwordModalTitle,
    deleteMode,
    targetComment,
    fetchPost,
    fetchComments,
    getTotalCommentCount,
    canModify,
    isMyComment,
    canModifyComment,
    setShowPasswordModal,
    setModalPassword,
    setPasswordModalTitle,
    setDeleteMode,
    setTargetComment,
  } = usePostDetail(postId, initialPost);

  // 댓글 액션 관리
  const commentActions = useCommentActions(postId, fetchComments);

  // 댓글 편집 및 답글 상태 관리
  const [editingComment, setEditingComment] = useState<Comment | null>(null);
  const [editContent, setEditContent] = useState("");
  const [editPassword, setEditPassword] = useState("");
  const [replyingTo, setReplyingTo] = useState<Comment | null>(null);
  const [replyContent, setReplyContent] = useState("");
  const [replyPassword, setReplyPassword] = useState("");

  // 삭제 확인 모달 상태 관리
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  // CommentSection이 기대하는 시그니처로 래핑 및 핸들러 함수들
  // 새 댓글 작성 시 parentId가 undefined인 공통 핸들러
  const handleCommentSubmitForSection = (comment: string, password: string) => {
    commentActions.handleCommentSubmit(comment, undefined, password);
  };

  // 댓글 수정 상태 관리 - 수정 모드 진입 시 기존 내용을 대입
  const handleEditComment = (comment: Comment) => {
    setEditingComment(comment);
    setEditContent(comment.content); // 기존 내용을 편집 필드에 설정
    setEditPassword(""); // 비밀번호는 매번 새로 입력
  };

  // 댓글 수정 완료 후 상태 초기화
  const handleUpdateComment = async () => {
    if (editingComment) {
      await commentActions.handleCommentEdit(editingComment.id, editContent, editPassword);
      // 수정 완료 후 모든 수정 상태 초기화
      setEditingComment(null);
      setEditContent("");
      setEditPassword("");
    }
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
    if (replyingTo) {
      await commentActions.handleCommentSubmit(replyContent, replyingTo.id, replyPassword);
      // 답글 작성 완료 후 답글 상태 초기화
      setReplyingTo(null);
      setReplyContent("");
      setReplyPassword("");
    }
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

  // 게시글 액션 관리
  const postActions = usePostActions(
    postId,
    post,
    canModify,
    setShowPasswordModal,
    setPasswordModalTitle,
    setDeleteMode,
    setModalPassword,
    fetchPost
  );

  // 게시글 삭제 클릭 핸들러 - 로그인 사용자는 확인 모달, 익명은 비밀번호 모달
  const handleDeleteClick = () => {
    if (post?.userName === "익명" || post?.userName === null) {
      // 익명 게시글의 경우 비밀번호 모달
      postActions.handleDeleteClick();
    } else {
      // 로그인 사용자의 경우 삭제 확인 모달
      setShowDeleteModal(true);
    }
  };

  // 삭제 확인 후 실제 삭제 실행
  const handleConfirmDelete = async () => {
    setShowDeleteModal(false);
    await postActions.handleDelete();
  };

  // 비밀번호 모달 제출 - 게시글/댓글 삭제 모드에 따라 분기 처리
  const handlePasswordSubmit = async () => {
    if (deleteMode === "post") {
      await postActions.handleDelete(modalPassword);
    } else if (deleteMode === "comment" && targetComment) {
      // 댓글 삭제 시 targetComment로 대상 확인
      await commentActions.handleCommentDelete(targetComment.id, modalPassword);
    }

    // 삭제 작업 완료 후 모든 모달 상태 초기화
    setShowPasswordModal(false);
    setModalPassword("");
    setDeleteMode(null);
    setTargetComment(null);
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
            onDeleteClick={postActions.handleDeleteClick}
          />
          <PostContent
            post={post}
            isAuthenticated={isAuthenticated}
            onLike={postActions.handleLike}
          />
          <PostActions
            post={post}
            canModify={canModify()}
            onDeletePost={handleDeleteClick}
          />
        </Card>

        {/* 댓글 섹션 */}
        <CommentSection
          postId={post.id}
          comments={comments}
          popularComments={popularComments}
          commentCount={commentCount}
          isAuthenticated={isAuthenticated}

          isSubmittingComment={commentActions.isSubmitting}
          onSubmitComment={handleCommentSubmitForSection}

          editingComment={editingComment}
          editContent={editContent}
          editPassword={editPassword}
          replyingTo={replyingTo}
          replyContent={replyContent}
          replyPassword={replyPassword}
          isSubmittingReply={commentActions.isSubmitting}

          onEditComment={handleEditComment}
          onUpdateComment={handleUpdateComment}
          onCancelEdit={handleCancelEdit}
          onDeleteComment={(comment) => commentActions.handleCommentDelete(comment.id)}
          onReplyTo={handleReplyTo}
          onReplySubmit={handleSubmitReply}
          onCancelReply={handleCancelReply}
          onLikeComment={(comment) => commentActions.handleCommentLike(comment.id)}

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
          onCancel={() => {
            setShowPasswordModal(false);
            setModalPassword("");
            setDeleteMode(null);
            setTargetComment(null);
          }}
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

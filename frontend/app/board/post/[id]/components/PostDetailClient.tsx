"use client";

import { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { ArrowLeft, Loader2 } from "lucide-react";
import Link from "next/link";
import { AuthHeader } from "@/components/organisms/auth-header";
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

// 분리된 훅들 import
import { usePostDetail } from "../hooks/usePostDetail";
import { useCommentActions } from "../hooks/useCommentActions";
import { usePostActions } from "../hooks/usePostActions";
import { useAuth } from "@/hooks/useAuth";

interface Props {
  initialPost: Post;
  postId: string;
}

export default function PostDetailClient({ initialPost, postId }: Props) {
  // 인증 상태 가져오기
  const { isAuthenticated } = useAuth();
  
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

  // 게시글 액션 관리
  const postActions = usePostActions(
    post,
    (title, mode, target) => {
      setPasswordModalTitle(title);
      setDeleteMode(mode);
      setTargetComment(target || null);
      setShowPasswordModal(true);
    },
    async () => {
      await Promise.all([fetchPost(), fetchComments()]);
    }
  );

  // 비밀번호 모달 제출
  const handlePasswordSubmit = async () => {
    await postActions.handlePasswordSubmit(
      modalPassword,
      deleteMode!,
      targetComment || undefined
    );

    // Reset state after action
    setShowPasswordModal(false);
    setModalPassword("");
    setDeleteMode(null);
    setTargetComment(null);
  };

  // 로딩 상태
  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-100 via-purple-50 to-indigo-100 flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  // 게시글이 없는 경우
  if (!post) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-100 via-purple-50 to-indigo-100 flex items-center justify-center">
        <p>게시글을 찾을 수 없습니다.</p>
      </div>
    );
  }

  const commentCount = getTotalCommentCount(comments);

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-100 via-purple-50 to-indigo-100">
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
        {/* 뒤로가기 버튼 */}
        <div className="mb-6">
          <Link href="/board">
            <ArrowLeft className="w-5 h-5 inline mr-2" />
            목록으로 돌아가기
          </Link>
        </div>

        {/* 게시글 카드 */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg mb-8">
          <PostHeader
            post={post}
            commentCount={commentCount}
            canModify={canModify}
            onDeleteClick={postActions.handleDeletePost}
          />
          <PostContent
            post={post}
            isAuthenticated={isAuthenticated}
            onLike={postActions.handleLike}
          />
          <PostActions
            post={post}
            commentCount={commentCount}
            canModify={canModify}
            isAuthenticated={isAuthenticated}
            onDeletePost={postActions.handleDeletePost}
            onLike={postActions.handleLike}
          />
        </Card>

        {/* 댓글 섹션 */}
        <CommentSection
          postId={post.id}
          comments={comments}
          popularComments={popularComments}
          commentCount={commentCount}
          isAuthenticated={isAuthenticated}
          
          newComment={commentActions.newComment}
          commentPassword={commentActions.commentPassword}
          isSubmittingComment={commentActions.isSubmittingComment}
          onCommentChange={commentActions.setNewComment}
          onPasswordChange={commentActions.setCommentPassword}
          onSubmitComment={commentActions.handleCommentSubmit}
          
          editingComment={commentActions.editingComment}
          editContent={commentActions.editContent}
          editPassword={commentActions.editPassword}
          replyingTo={commentActions.replyingTo}
          replyContent={commentActions.replyContent}
          replyPassword={commentActions.replyPassword}
          isSubmittingReply={commentActions.isSubmittingReply}
          
          onEditComment={commentActions.handleEditComment}
          onUpdateComment={commentActions.handleUpdateComment}
          onCancelEdit={commentActions.handleCancelEdit}
          onDeleteComment={postActions.handleDeleteComment}
          onReplyTo={commentActions.setReplyingTo}
          onReplySubmit={commentActions.handleReplySubmit}
          onCancelReply={commentActions.handleCancelReply}
          onLikeComment={commentActions.handleLikeComment}
          
          setEditContent={commentActions.setEditContent}
          setEditPassword={commentActions.setEditPassword}
          setReplyContent={commentActions.setReplyContent}
          setReplyPassword={commentActions.setReplyPassword}
          
          isMyComment={isMyComment}
          canModifyComment={canModifyComment}
          onCommentClick={commentActions.scrollToComment}
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
      </div>
    </div>
  );
}

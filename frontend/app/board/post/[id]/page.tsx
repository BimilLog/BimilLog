"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { boardApi, commentApi, type Post, type Comment } from "@/lib/api";
import { Card } from "@/components/ui/card";
import { ArrowLeft, Loader2 } from "lucide-react";
import Link from "next/link";

// 분리된 컴포넌트들 import
import { PostHeader } from "./components/PostHeader";
import { PostContent } from "./components/PostContent";
import { CommentForm } from "./components/CommentForm";
import { PopularComments } from "./components/PopularComments";
import { CommentList } from "./components/CommentList";
import { PasswordModal } from "./components/PasswordModal";

interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

export default function PostDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const [id, setId] = useState<string | null>(null);

  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<CommentWithReplies[]>([]);
  const [popularComments, setPopularComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [modalPassword, setModalPassword] = useState("");
  const [passwordModalTitle, setPasswordModalTitle] = useState("");
  const [pendingAction, setPendingAction] = useState<() => void>(() => {});

  // 댓글 관련 상태
  const [newComment, setNewComment] = useState("");
  const [commentPassword, setCommentPassword] = useState("");
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);

  // 답글 관련 상태
  const [replyingTo, setReplyingTo] = useState<Comment | null>(null);
  const [replyContent, setReplyContent] = useState("");
  const [replyPassword, setReplyPassword] = useState("");
  const [isSubmittingReply, setIsSubmittingReply] = useState(false);

  // 댓글 수정 관련 상태
  const [editingComment, setEditingComment] = useState<Comment | null>(null);
  const [editContent, setEditContent] = useState("");
  const [editPassword, setEditPassword] = useState("");

  // params에서 id 추출
  useEffect(() => {
    if (params.id) {
      setId(params.id as string);
    }
  }, [params]);

  useEffect(() => {
    if (id) {
      fetchPost();
      fetchComments();
    }
  }, [id]);

  const fetchPost = async () => {
    try {
      const response = await boardApi.getPost(Number(id));
      if (response.success && response.data) {
        setPost(response.data);
      } else {
        throw new Error("게시글을 불러올 수 없습니다");
      }
    } catch (error) {
      console.error("게시글 조회 실패:", error);
      router.push("/board");
    } finally {
      setLoading(false);
    }
  };

  const getTotalCommentCount = (
    comments: (Comment & { replies?: Comment[] })[]
  ): number => {
    let count = 0;
    comments.forEach((comment) => {
      count += 1; // 현재 댓글
      if (comment.replies) {
        count += getTotalCommentCount(comment.replies); // 재귀적으로 답글 개수 계산
      }
    });
    return count;
  };

  const buildCommentTree = (comments: Comment[]): Comment[] => {
    const commentMap = new Map<number, Comment & { replies: Comment[] }>();
    const rootComments: Comment[] = [];

    // 먼저 모든 댓글을 Map에 저장하고 replies 배열 초기화
    comments.forEach((comment) => {
      commentMap.set(comment.id, { ...comment, replies: [] });
    });

    // 부모-자식 관계 설정
    comments.forEach((comment) => {
      if (comment.parentId && commentMap.has(comment.parentId)) {
        const parent = commentMap.get(comment.parentId)!;
        const child = commentMap.get(comment.id)!;
        parent.replies.push(child);
      } else {
        // 부모가 없으면 루트 댓글
        rootComments.push(commentMap.get(comment.id)!);
      }
    });

    return rootComments;
  };

  const fetchComments = async () => {
    try {
      const [commentsResponse, popularResponse] = await Promise.all([
        commentApi.getComments(Number(id), 0),
        commentApi.getPopularComments(Number(id)),
      ]);

      if (commentsResponse.success && commentsResponse.data) {
        const treeComments = buildCommentTree(commentsResponse.data.content);
        setComments(treeComments);
      }

      if (popularResponse.success && popularResponse.data) {
        setPopularComments(popularResponse.data);
      }
    } catch (error) {
      console.error("댓글 조회 실패:", error);
    }
  };

  const handlePasswordSubmit = async () => {
    if (modalPassword) {
      pendingAction();
      setShowPasswordModal(false);
      setModalPassword("");
    }
  };

  const handleLike = async () => {
    if (!post) return;
    try {
      await boardApi.likePost(post.postId);
      fetchPost();
    } catch (error) {
      console.error("추천 실패:", error);
    }
  };

  const handleCommentSubmit = async () => {
    if (!newComment.trim()) return;
    if (!isAuthenticated && !commentPassword.trim()) return;

    setIsSubmittingComment(true);
    try {
      await commentApi.createComment({
        postId: Number(id),
        userName: isAuthenticated ? user?.userName || "" : "비회원",
        content: newComment,
        password: isAuthenticated ? undefined : Number(commentPassword),
      });

      setNewComment("");
      setCommentPassword("");
      await fetchComments();
    } catch (error) {
      console.error("댓글 작성 실패:", error);
    } finally {
      setIsSubmittingComment(false);
    }
  };

  const handleReplySubmit = async () => {
    if (!replyContent.trim() || !replyingTo) return;
    if (!isAuthenticated && !replyPassword.trim()) return;

    setIsSubmittingReply(true);
    try {
      await commentApi.createComment({
        postId: Number(id),
        userName: isAuthenticated ? user?.userName || "" : "비회원",
        content: replyContent,
        parentId: replyingTo.id,
        password: isAuthenticated ? undefined : Number(replyPassword),
      });

      setReplyContent("");
      setReplyPassword("");
      setReplyingTo(null);
      await fetchComments();
    } catch (error) {
      console.error("답글 작성 실패:", error);
    } finally {
      setIsSubmittingReply(false);
    }
  };

  const handleCancelReply = () => {
    setReplyingTo(null);
    setReplyContent("");
    setReplyPassword("");
  };

  const handleLikeComment = async (comment: Comment) => {
    try {
      await commentApi.likeComment(comment.id);
      await fetchComments();
    } catch (error) {
      console.error("댓글 추천 실패:", error);
    }
  };

  const scrollToComment = (commentId: number) => {
    const element = document.getElementById(`comment-${commentId}`);
    if (element) {
      element.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });

      // 하이라이트 효과
      const commentContent = element.querySelector(".comment-content");
      if (commentContent) {
        commentContent.classList.add("bg-yellow-200");
        setTimeout(() => {
          commentContent.classList.remove("bg-yellow-200");
        }, 2000);
      }
    }
  };

  const handleDeletePost = async () => {
    if (!post) return;

    const deleteAction = async () => {
      try {
        if (post.userName === "비회원") {
          await boardApi.deletePost(post.postId, modalPassword);
        } else {
          await boardApi.deletePost(post.postId);
        }
        router.push("/board");
      } catch (error) {
        console.error("게시글 삭제 실패:", error);
      }
    };

    if (post.userName === "비회원") {
      setPasswordModalTitle("게시글 삭제");
      setPendingAction(() => deleteAction);
      setShowPasswordModal(true);
    } else {
      await deleteAction();
    }
  };

  const canModify = () => {
    if (!post) return false;
    if (post.userName === "비회원") {
      return !isAuthenticated;
    }
    return isAuthenticated && user?.userName === post.userName;
  };

  const isMyComment = (comment: Comment) => {
    return isAuthenticated && user?.userName === comment.userName;
  };

  const canModifyComment = (comment: Comment) => {
    if (comment.userName === "비회원") {
      return !isAuthenticated;
    }
    return isAuthenticated && user?.userName === comment.userName;
  };

  const handleEditComment = (comment: Comment) => {
    setEditingComment(comment);
    setEditContent(comment.content);
    setEditPassword("");
  };

  const handleCancelEdit = () => {
    setEditingComment(null);
    setEditContent("");
    setEditPassword("");
  };

  const handleUpdateComment = async () => {
    if (!editingComment || !editContent.trim()) return;

    try {
      if (isMyComment(editingComment)) {
        await commentApi.updateComment(editingComment.id, {
          content: editContent,
        });
      } else {
        if (!editPassword.trim()) return;
        await commentApi.updateComment(editingComment.id, {
          content: editContent,
          password: editPassword,
        });
      }

      setEditingComment(null);
      setEditContent("");
      setEditPassword("");
      await fetchComments();
    } catch (error) {
      console.error("댓글 수정 실패:", error);
    }
  };

  const handleDeleteComment = (comment: Comment) => {
    const deleteAction = (password?: string) => {
      confirmDeleteComment(comment, password);
    };

    if (comment.userName === "비회원") {
      setPasswordModalTitle("댓글 삭제");
      setPendingAction(() => deleteAction(modalPassword));
      setShowPasswordModal(true);
    } else {
      deleteAction();
    }
  };

  const confirmDeleteComment = async (comment: Comment, password?: string) => {
    try {
      if (comment.userName === "비회원") {
        await commentApi.deleteComment(comment.id, password);
      } else {
        await commentApi.deleteComment(comment.id);
      }
      await fetchComments();
    } catch (error) {
      console.error("댓글 삭제 실패:", error);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-100 via-purple-50 to-indigo-100 flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

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
      <div className="container mx-auto px-4 py-8">
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
            onDeleteClick={handleDeletePost}
          />
          <PostContent
            post={post}
            isAuthenticated={isAuthenticated}
            onLike={handleLike}
          />
        </Card>

        {/* 댓글 작성 폼 */}
        <CommentForm
          isAuthenticated={isAuthenticated}
          newComment={newComment}
          commentPassword={commentPassword}
          isSubmittingComment={isSubmittingComment}
          onCommentChange={setNewComment}
          onPasswordChange={setCommentPassword}
          onSubmit={handleCommentSubmit}
        />

        {/* 인기 댓글 */}
        <PopularComments
          popularComments={popularComments}
          onCommentClick={scrollToComment}
        />

        {/* 댓글 목록 */}
        <CommentList
          comments={comments}
          commentCount={commentCount}
          editingComment={editingComment}
          editContent={editContent}
          editPassword={editPassword}
          replyingTo={replyingTo}
          replyContent={replyContent}
          replyPassword={replyPassword}
          isAuthenticated={isAuthenticated}
          isSubmittingReply={isSubmittingReply}
          onEditComment={handleEditComment}
          onUpdateComment={handleUpdateComment}
          onCancelEdit={handleCancelEdit}
          onDeleteComment={handleDeleteComment}
          onReplyTo={setReplyingTo}
          onReplySubmit={handleReplySubmit}
          onCancelReply={handleCancelReply}
          setEditContent={setEditContent}
          setEditPassword={setEditPassword}
          setReplyContent={setReplyContent}
          setReplyPassword={setReplyPassword}
          isMyComment={isMyComment}
          onLikeComment={handleLikeComment}
          canModifyComment={canModifyComment}
        />

        {/* 비밀번호 모달 */}
        <PasswordModal
          isOpen={showPasswordModal}
          password={modalPassword}
          onPasswordChange={setModalPassword}
          onConfirm={handlePasswordSubmit}
          onCancel={() => {
            setShowPasswordModal(false);
            setModalPassword("");
          }}
          title={passwordModalTitle}
        />
      </div>
    </div>
  );
}

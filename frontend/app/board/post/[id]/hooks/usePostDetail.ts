import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { boardApi, commentApi, type Post, type Comment } from "@/lib/api";

interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

export const usePostDetail = (id: string | null) => {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  
  // Post 상태
  const [post, setPost] = useState<Post | null>(null);
  const [loading, setLoading] = useState(true);

  // Comment 상태
  const [comments, setComments] = useState<CommentWithReplies[]>([]);
  const [popularComments, setPopularComments] = useState<Comment[]>([]);

  // Modal 상태
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [modalPassword, setModalPassword] = useState("");
  const [passwordModalTitle, setPasswordModalTitle] = useState("");
  const [deleteMode, setDeleteMode] = useState<"post" | "comment" | null>(null);
  const [targetComment, setTargetComment] = useState<Comment | null>(null);

  // Data fetching
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

  const buildCommentTree = (comments: Comment[]): Comment[] => {
    const commentMap = new Map<number, Comment & { replies: Comment[] }>();
    const rootComments: Comment[] = [];

    comments.forEach((comment) => {
      commentMap.set(comment.id, { ...comment, replies: [] });
    });

    comments.forEach((comment) => {
      if (comment.parentId && commentMap.has(comment.parentId)) {
        const parent = commentMap.get(comment.parentId)!;
        const child = commentMap.get(comment.id)!;
        parent.replies.push(child);
      } else {
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

  const getTotalCommentCount = (
    comments: (Comment & { replies?: Comment[] })[]
  ): number => {
    let count = 0;
    comments.forEach((comment) => {
      count += 1;
      if (comment.replies) {
        count += getTotalCommentCount(comment.replies);
      }
    });
    return count;
  };

  // Permission checks
  const canModify = () => {
    if (!post || loading) return false;
    if (post.userName === "익명" || post.userName === null) {
      return !isAuthenticated;
    }
    return isAuthenticated && user?.userName === post.userName;
  };

  const isMyComment = (comment: Comment) => {
    return isAuthenticated && user?.userName === comment.userName;
  };

  const canModifyComment = (comment: Comment) => {
    if (comment.userName === "익명" || comment.userName === null) {
      return !isAuthenticated;
    }
    return isAuthenticated && user?.userName === comment.userName;
  };

  // Effects
  useEffect(() => {
    if (id) {
      fetchPost();
      fetchComments();
    }
  }, [id]);

  return {
    // Data
    post,
    comments,
    popularComments,
    loading,
    
    // Modal state
    showPasswordModal,
    modalPassword,
    passwordModalTitle,
    deleteMode,
    targetComment,
    
    // Actions
    fetchPost,
    fetchComments,
    getTotalCommentCount,
    canModify,
    isMyComment,
    canModifyComment,
    
    // Modal actions
    setShowPasswordModal,
    setModalPassword,
    setPasswordModalTitle,
    setDeleteMode,
    setTargetComment,
  };
}; 
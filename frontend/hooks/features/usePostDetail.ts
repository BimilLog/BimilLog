import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks";
import { postQuery, commentQuery, type Post, type Comment } from "@/lib/api";

interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

export const usePostDetail = (id: string | null, initialPost?: Post) => {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();

  // Post 상태
  const [post, setPost] = useState<Post | null>(initialPost || null);
  const [loading, setLoading] = useState(!initialPost);

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
  const fetchPost = useCallback(async () => {
    if (!id) return;

    try {
      const response = await postQuery.getById(Number(id));
      if (response.success && response.data) {
        setPost(response.data);
      } else {
        throw new Error("게시글을 불러올 수 없습니다");
      }
    } catch (error) {
      router.push("/board");
    } finally {
      setLoading(false);
    }
  }, [id, router]);

  const buildCommentTree = useCallback((comments: Comment[]): CommentWithReplies[] => {
    const commentMap = new Map<number, CommentWithReplies>();
    const rootComments: CommentWithReplies[] = [];

    comments.forEach((comment) => {
      commentMap.set(comment.id, { ...comment, replies: [] });
    });

    comments.forEach((comment) => {
      if (comment.parentId && commentMap.has(comment.parentId)) {
        const parent = commentMap.get(comment.parentId)!;
        const child = commentMap.get(comment.id)!;
        parent.replies!.push(child);
      } else {
        rootComments.push(commentMap.get(comment.id)!);
      }
    });

    return rootComments;
  }, []);

  const fetchComments = useCallback(async () => {
    if (!id) return;

    try {
      const [commentsResponse, popularResponse] = await Promise.all([
        commentQuery.getByPostId(Number(id), 0),
        commentQuery.getPopular(Number(id)),
      ]);

      if (commentsResponse.success && commentsResponse.data) {
        const treeComments = buildCommentTree(commentsResponse.data.content);
        setComments(treeComments);
      } else {
        setComments([]);
      }

      if (popularResponse.success && popularResponse.data) {
        setPopularComments(popularResponse.data);
      } else {
        setPopularComments([]);
      }
    } catch (error) {
      setComments([]);
      setPopularComments([]);
    }
  }, [id, buildCommentTree]);

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
    if (!id) return;
    // 서버에서 initialPost가 내려온 경우 조회수는 이미 SSR 단계에서 증가하지 않았고,
    // 브라우저 첫 렌더 시 한 번만 증가시키기 위해 상세 재조회는 initialPost 없을 때만 수행
    if (!initialPost) {
      fetchPost();
    }
    fetchComments();
  }, [id, initialPost, fetchPost, fetchComments]);

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
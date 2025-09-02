import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { boardApi, commentApi, type Post, type Comment } from "@/lib/api";

export const usePostActions = (
  post: Post | null,
  onPasswordModalOpen: (title: string, mode: "post" | "comment", target?: Comment) => void,
  onRefresh: () => Promise<void>
) => {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();

  // 게시글 추천
  const handleLike = async () => {
    if (!post) return;
    try {
      await boardApi.likePost(post.id);
      await onRefresh();
    } catch (error) {
      console.error("추천 실패:", error);
    }
  };

  // 게시글 삭제 확인
  const confirmDeletePost = async (password?: string) => {
    if (!post) return;
    try {
      const response =
        post.userName === "익명" || post.userName === null
          ? await boardApi.deletePost(
              post.id,
              post.userId,
              password,
              post.content,
              post.title
            )
          : await boardApi.deletePost(post.id, post.userId);

      if (response.success) {
        router.push("/board");
      } else {
        if (
          response.error &&
          response.error.includes("게시글 비밀번호가 일치하지 않습니다")
        ) {
          alert("비밀번호가 일치하지 않습니다.");
        } else {
          alert(response.error || "게시글 삭제에 실패했습니다.");
        }
      }
    } catch (error) {
      console.error("게시글 삭제 실패:", error);
      if (error instanceof Error && error.message.includes("403")) {
        alert("비밀번호가 일치하지 않습니다.");
      } else {
        alert("게시글 삭제 중 오류가 발생했습니다.");
      }
    }
  };

  // 게시글 삭제
  const handleDeletePost = async () => {
    if (!post) return;

    if (post.userName === "익명" || post.userName === null) {
      onPasswordModalOpen("게시글 삭제", "post");
    } else {
      await confirmDeletePost();
    }
  };

  // 댓글 삭제 확인
  const confirmDeleteComment = async (comment: Comment, password?: string) => {
    try {
      const response =
        comment.userName === "익명" || comment.userName === null
          ? await commentApi.deleteComment(
              comment.id,
              user?.userId,
              Number(password),
              comment.content
            )
          : await commentApi.deleteComment(
              comment.id,
              user?.userId,
              undefined,
              comment.content
            );

      if (response.success) {
        await onRefresh();
      } else {
        if (
          response.error &&
          response.error.includes("댓글 비밀번호가 일치하지 않습니다")
        ) {
          alert("비밀번호가 일치하지 않습니다.");
        } else {
          alert(response.error || "댓글 삭제에 실패했습니다.");
        }
      }
    } catch (error) {
      console.error("댓글 삭제 실패:", error);
      if (error instanceof Error && error.message.includes("403")) {
        alert("비밀번호가 일치하지 않습니다.");
      } else {
        alert("댓글 삭제 중 오류가 발생했습니다.");
      }
    }
  };

  // 댓글 삭제
  const handleDeleteComment = (comment: Comment) => {
    if (comment.userName === "익명" || comment.userName === null) {
      onPasswordModalOpen("댓글 삭제", "comment", comment);
    } else {
      confirmDeleteComment(comment);
    }
  };

  // 비밀번호 제출 처리
  const handlePasswordSubmit = async (
    password: string,
    mode: "post" | "comment",
    targetComment?: Comment
  ) => {
    if (!password.trim()) {
      alert("비밀번호를 입력해주세요.");
      return;
    }

    if (mode === "post") {
      await confirmDeletePost(password);
    } else if (mode === "comment" && targetComment) {
      await confirmDeleteComment(targetComment, password);
    }
  };

  return {
    handleLike,
    handleDeletePost,
    handleDeleteComment,
    handlePasswordSubmit,
    confirmDeletePost,
    confirmDeleteComment,
  };
}; 
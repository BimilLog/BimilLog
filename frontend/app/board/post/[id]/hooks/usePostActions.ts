import { useRouter } from "next/navigation";
import { useAuth, useToast } from "@/hooks";
import { postCommand, commentCommand, type Post, type Comment } from "@/lib/api";

export const usePostActions = (
  post: Post | null,
  onPasswordModalOpen: (title: string, mode: "post" | "comment", target?: Comment) => void,
  onRefresh: () => Promise<void>
) => {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const { showSuccess, showError, showWarning } = useToast();

  // 게시글 추천
  const handleLike = async () => {
    if (!post) return;
    try {
      await postCommand.like(post.id);
      await onRefresh();
    } catch (error) {
    }
  };

  // 게시글 삭제 확인
  const confirmDeletePost = async (password?: string) => {
    if (!post) return;
    try {
      // API는 파라미터를 받지만 실제로는 DELETE 메서드만 사용 (백엔드에서 처리)
      // 익명 게시글의 경우 password를 헤더나 바디에 포함하여 전송해야 할 수 있음
      const response = await postCommand.delete(post.id);

      if (response.success) {
        showSuccess("삭제 완료", "게시글이 성공적으로 삭제되었습니다.");
        router.push("/board");
      } else {
        if (
          response.error &&
          response.error.includes("게시글 비밀번호가 일치하지 않습니다")
        ) {
          showError("삭제 실패", "비밀번호가 일치하지 않습니다.");
        } else {
          showError("삭제 실패", response.error || "게시글 삭제에 실패했습니다.");
        }
      }
    } catch (error) {
      if (error instanceof Error && error.message.includes("403")) {
        showError("삭제 실패", "비밀번호가 일치하지 않습니다.");
      } else {
        showError("삭제 실패", "게시글 삭제 중 오류가 발생했습니다.");
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
      // password가 있으면 전달, 없으면 undefined로 통일
      const response = await commentCommand.delete(
        comment.id,
        password ? Number(password) : undefined
      );

      if (response.success) {
        showSuccess("삭제 완료", "댓글이 성공적으로 삭제되었습니다.");
        await onRefresh();
      } else {
        if (
          response.error &&
          response.error.includes("댓글 비밀번호가 일치하지 않습니다")
        ) {
          showError("삭제 실패", "비밀번호가 일치하지 않습니다.");
        } else {
          showError("삭제 실패", response.error || "댓글 삭제에 실패했습니다.");
        }
      }
    } catch (error) {
      if (error instanceof Error && error.message.includes("403")) {
        showError("삭제 실패", "비밀번호가 일치하지 않습니다.");
      } else {
        showError("삭제 실패", "댓글 삭제 중 오류가 발생했습니다.");
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
      showWarning("입력 확인", "비밀번호를 입력해주세요.");
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
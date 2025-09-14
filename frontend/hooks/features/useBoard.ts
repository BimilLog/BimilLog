"use client";

import { useState } from "react";
import { useAuth, useToast } from "@/hooks";

// ============ BOARD QUERY HOOKS ============
export {
  useBoardPosts,
  useBoardSearch,
  useBoardPopularPosts,
  useBoardLegendPosts,
  useBoardNoticePosts,
} from '@/hooks/api/useBoardQueries';

// ============ BOARD MUTATION HOOKS ============
export {
  useCreateBoardPost,
  useUpdateBoardPost,
  useDeleteBoardPost,
} from '@/hooks/api/useBoardMutations';

// Import for local usage
import { useCreateBoardPost } from '@/hooks/api/useBoardMutations';

// ============ BOARD WRITE FORM HOOK ============

/**
 * 게시글 작성 폼을 위한 통합 훅
 * TanStack Query mutation과 로컬 폼 상태를 결합
 */
export const useWriteForm = () => {
  const { user, isAuthenticated } = useAuth();
  const { showWarning } = useToast();
  const createPostMutation = useCreateBoardPost();

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [password, setPassword] = useState("");
  const [isPreview, setIsPreview] = useState(false);

  // 폼 유효성 검사
  const validateForm = () => {
    if (!title.trim() || !content.trim()) {
      showWarning("입력 확인", "제목과 내용을 모두 입력해주세요.");
      return false;
    }
    return true;
  };

  // 폼 제출 핸들러
  const handleSubmit = async () => {
    if (!validateForm()) return;

    createPostMutation.mutate({
      title,
      content,
      password,
    });
  };

  const isFormValid = Boolean(title.trim() && content.trim());
  const isSubmitting = createPostMutation.isPending;

  const resetForm = () => {
    setTitle("");
    setContent("");
    setPassword("");
    setIsPreview(false);
  };

  return {
    // Form fields
    title,
    setTitle,
    content,
    setContent,
    password,
    setPassword,
    isPreview,
    setIsPreview,

    // Form actions
    handleSubmit,
    isSubmitting,
    validateForm,
    isFormValid,
    resetForm,

    // User info
    user,
    isAuthenticated,
  };
};
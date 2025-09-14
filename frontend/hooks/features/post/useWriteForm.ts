"use client";

import { useState } from "react";
import { useAuth, useToast } from "@/hooks";
import { useCreateBoardPost } from '@/hooks/api/useBoardMutations';

/**
 * 게시글 작성 폼을 위한 통합 훅
 * TanStack Query mutation과 로컬 폼 상태를 결합
 */
export function useWriteForm() {
  const { user, isAuthenticated } = useAuth();
  const { showWarning } = useToast();
  // TanStack Query의 mutation 훅 - 게시글 생성 API 호출 및 캐시 관리
  const createPostMutation = useCreateBoardPost();

  // 로컬 폼 상태 관리
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [password, setPassword] = useState(""); // 비회원 게시글용 비밀번호
  const [isPreview, setIsPreview] = useState(false);

  // 폼 유효성 검사: 제목과 내용의 공백 제거 후 빈 값 체크
  const validateForm = () => {
    if (!title.trim() || !content.trim()) {
      showWarning("입력 확인", "제목과 내용을 모두 입력해주세요.");
      return false;
    }
    return true;
  };

  // 폼 제출 핸들러 - 유효성 검사 후 TanStack Query mutation 실행
  const handleSubmit = async () => {
    if (!validateForm()) return;

    // mutation 실행 - API 호출, 캐시 무효화, 성공/실패 처리 모두 자동
    createPostMutation.mutate({
      title,
      content,
      password, // 회원은 빈 값, 비회원은 입력된 비밀번호
    });
  };

  // 폼 유효성 및 제출 상태 계산
  const isFormValid = Boolean(title.trim() && content.trim()); // 제목, 내용 모두 입력되어야 유효
  const isSubmitting = createPostMutation.isPending; // TanStack Query mutation 진행 상태

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
}
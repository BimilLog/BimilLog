"use client";

import { useState, useEffect } from "react";
import { useAuth, useToast } from "@/hooks";
import { useCreatePost } from '@/hooks/api/usePostMutations';
import { useDraft } from '@/hooks/features/useDraft';

/**
 * 게시글 작성 폼을 위한 통합 훅
 * TanStack Query mutation과 로컬 폼 상태를 결합
 * 임시저장 기능 추가
 */
export function useWriteForm() {
  const { user, isAuthenticated } = useAuth();
  const { showWarning } = useToast();
  // TanStack Query의 mutation 훅 - 게시글 생성 API 호출 및 캐시 관리
  const createPostMutation = useCreatePost();

  // 로컬 폼 상태 관리
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [password, setPassword] = useState(""); // 비회원 게시글용 비밀번호
  const [isPreview, setIsPreview] = useState(false);

  // 임시저장 훅 사용
  const {
    isAutoSaving,
    lastSavedAt,
    hasSavedDraft,
    loadDraft,
    saveDraftManual,
    handleAutoSave,
    removeDraft,
    formatLastSaved
  } = useDraft({
    enabled: true,
    autoSave: true,
    onRestore: (draft) => {
      setTitle(draft.title || '');
      setContent(draft.content);
    }
  });

  // 컴포넌트 마운트 시 임시저장 확인 및 복구
  useEffect(() => {
    if (hasSavedDraft) {
      loadDraft();
    }
  }, []);

  // 자동저장 트리거 - title 또는 content 변경 시
  useEffect(() => {
    if (title || content) {
      handleAutoSave(title, content);
    }
  }, [title, content, handleAutoSave]);

  // 작성 중 이탈 방지 - 사용자가 실수로 페이지를 벗어나는 것을 방지
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      // 제목이나 내용이 있고, 제출 중이 아닐 때만 경고
      if ((title.trim() || content.trim()) && !createPostMutation.isPending) {
        e.preventDefault();
        e.returnValue = ''; // Chrome requires returnValue
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [title, content, createPostMutation.isPending]);

  // 폼 제출 핸들러 - 유효성 검사 후 TanStack Query mutation 실행
  const handleSubmit = async () => {
    // 제목과 내용 유효성 검사
    if (!title.trim() || !content.trim()) {
      showWarning("입력 확인", "제목과 내용을 모두 입력해주세요.");
      return;
    }

    // 비회원일 경우 비밀번호 검증
    if (!isAuthenticated) {
      if (!password) {
        showWarning("입력 확인", "비밀번호를 입력해주세요.");
        return;
      }

      const passwordNum = parseInt(password, 10);
      if (isNaN(passwordNum) || passwordNum < 1000 || passwordNum > 9999) {
        showWarning("입력 확인", "비밀번호는 1000~9999 범위의 4자리 숫자여야 합니다.");
        return;
      }
    }

    // mutation 실행 - API 호출, 캐시 무효화, 성공/실패 처리 모두 자동
    createPostMutation.mutate({
      title,
      content,
      password: password ? parseInt(password, 10) : undefined, // 회원은 undefined, 비회원은 숫자로 변환
    });
  };

  // 게시글 작성 성공 시 임시저장 삭제
  useEffect(() => {
    if (createPostMutation.isSuccess) {
      removeDraft();
    }
  }, [createPostMutation.isSuccess, removeDraft]);

  // 폼 유효성 및 제출 상태 계산
  const isFormValid = Boolean(
    title.trim() &&
    content.trim() &&
    (isAuthenticated || (password && password.length === 4 && parseInt(password) >= 1000 && parseInt(password) <= 9999))
  );
  const isSubmitting = createPostMutation.isPending; // TanStack Query mutation 진행 상태

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
    isFormValid,

    // User info
    user,
    isAuthenticated,

    // Draft features
    isAutoSaving,
    lastSavedAt,
    hasSavedDraft,
    saveDraftManual,
    removeDraft,
    formatLastSaved,
  };
}
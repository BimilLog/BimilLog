"use client";

import { useState, useEffect, useMemo, useRef } from "react";
import { useAuth, useToast } from "@/hooks";
import { useCreatePostAction } from '@/hooks/actions/usePostActions';
import { useDraft } from '@/hooks/features/useDraft';

/**
 * HTML 태그를 제거하여 순수 텍스트 길이 계산
 */
const stripHtmlTags = (html: string): string => {
  return html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
};

/**
 * 게시글 작성 폼을 위한 통합 훅
 * TanStack Query mutation과 로컬 폼 상태를 결합
 * 임시저장 기능 추가
 */
export function useWriteForm() {
  const { user, isAuthenticated } = useAuth();
  const { showWarning } = useToast();
  const { createPost, isPending } = useCreatePostAction();

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

  // B-7-003: 임시저장 자동 복구 → 사용자 선택 패턴으로 변경
  // hasSavedDraft 가 true 면 useDraft 가 hasSavedDraft 상태를 노출하므로,
  // UI 측에서 배너로 "이어서 작성 / 새로 시작" 선택지를 보여주고
  // 사용자가 명시적으로 loadDraft() 또는 dismissDraft() 를 호출.
  const [draftBannerDismissed, setDraftBannerDismissed] = useState(false);
  // 사용자가 이미 입력을 시작했다면 배너를 숨김 (현재 작성 중인 내용을 덮어쓰지 않도록)
  const userStartedTyping = title.trim().length > 0 || content.trim().length > 0;
  const showDraftBanner = hasSavedDraft && !draftBannerDismissed && !userStartedTyping;

  const handleRestoreDraft = () => {
    loadDraft();
    setDraftBannerDismissed(true);
  };
  const handleDismissDraft = () => {
    // 사용자가 "새로 작성" 선택 → 배너만 닫고 임시저장은 유지
    // (다음 자동저장 시점에 새 내용으로 덮어씀)
    setDraftBannerDismissed(true);
  };

  // 자동저장 트리거 - title 또는 content 변경 시
  useEffect(() => {
    if (title || content) {
      handleAutoSave(title, content);
    }
  }, [title, content, handleAutoSave]);

  // B-7-009/성능: 작성 중 이탈 방지 — 매 입력마다 listener 재등록 비용 제거
  // ref 로 latest 값 추적 + listener 는 마운트 시 1회 등록
  const titleRef = useRef(title);
  const contentRef = useRef(content);
  const isPendingRef = useRef(isPending);
  useEffect(() => { titleRef.current = title; }, [title]);
  useEffect(() => { contentRef.current = content; }, [content]);
  useEffect(() => { isPendingRef.current = isPending; }, [isPending]);

  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if ((titleRef.current.trim() || contentRef.current.trim()) && !isPendingRef.current) {
        e.preventDefault();
        e.returnValue = ''; // Chrome requires returnValue
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, []);

  // 폼 제출 핸들러 - 유효성 검사 후 TanStack Query mutation 실행
  const handleSubmit = async () => {
    // 제목과 내용 유효성 검사
    if (!title.trim() || !content.trim()) {
      showWarning("입력 확인", "제목과 내용을 모두 입력해주세요.");
      return;
    }

    // 순수 텍스트 길이 검증 (HTML 태그 제외)
    const plainTextContent = stripHtmlTags(content);
    if (plainTextContent.length < 10) {
      showWarning("입력 확인", "게시글 내용은 10자 이상이어야 합니다.");
      return;
    }
    if (plainTextContent.length > 1000) {
      showWarning("입력 확인", "게시글 내용은 1000자 이하여야 합니다.");
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

    // B-7-014: 게시 성공 토스트와 충돌하지 않도록 silent 삭제
    removeDraft({ silent: true });
    createPost({
      title,
      content,
      password: password ? parseInt(password, 10) : undefined,
    });
  };

  // 순수 텍스트 길이 계산 (memoization으로 최적화)
  const plainTextLength = useMemo(() => {
    return stripHtmlTags(content).length;
  }, [content]);

  // 폼 유효성 및 제출 상태 계산
  const isFormValid = Boolean(
    title.trim() &&
    plainTextLength >= 10 &&
    plainTextLength <= 1000 &&
    (isAuthenticated || (password && password.length === 4 && parseInt(password) >= 1000 && parseInt(password) <= 9999))
  );
  const isSubmitting = isPending;

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

    // B-7-003: 사용자 동의 기반 복구 배너
    showDraftBanner,
    handleRestoreDraft,
    handleDismissDraft,

    // Content length
    plainTextLength,
  };
}
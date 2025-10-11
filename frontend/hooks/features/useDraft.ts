"use client";

import { useState, useEffect, useCallback, useRef } from 'react';
import {
  saveDraft,
  getPostDraft,
  deletePostDraft,
  hasDraft,
  getDraftMetadata,
  AUTO_SAVE_DELAY,
  type Draft
} from '@/lib/utils/draft';
import { useToast } from '@/hooks';
import { formatKoreanDateTime } from '@/lib/utils';

interface UseDraftOptions {
  postId?: number; // 수정 모드일 때 사용
  enabled?: boolean; // 임시저장 기능 활성화 여부
  autoSave?: boolean; // 자동저장 활성화 여부
  onRestore?: (draft: Draft) => void; // 복구 시 콜백
}

export function useDraft(options: UseDraftOptions = {}) {
  const {
    postId,
    enabled = true,
    autoSave = true,
    onRestore
  } = options;

  const { showInfo, showSuccess, showWarning } = useToast();
  const [isAutoSaving, setIsAutoSaving] = useState(false);
  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);
  const [hasSavedDraft, setHasSavedDraft] = useState(false);
  const autoSaveTimerRef = useRef<NodeJS.Timeout | null>(null);
  const contentRef = useRef<{ title?: string; content: string }>({ content: '' });

  // 임시저장 존재 여부 확인
  useEffect(() => {
    if (!enabled) return;
    setHasSavedDraft(hasDraft(postId));
  }, [enabled, postId]);

  // 임시저장 불러오기
  const loadDraft = useCallback(() => {
    if (!enabled) return null;

    const draft = getPostDraft(postId);
    if (draft) {
      showInfo(
        '임시저장 복구',
        `${formatKoreanDateTime(draft.updatedAt)}에 저장된 내용이 있습니다.`
      );

      if (onRestore) {
        onRestore(draft);
      }

      setLastSavedAt(new Date(draft.updatedAt));
      setHasSavedDraft(true);
      return draft;
    }
    return null;
  }, [enabled, postId, showInfo, onRestore]);

  // 수동 저장
  const saveDraftManual = useCallback((title: string | undefined, content: string) => {
    if (!enabled || !content.trim()) return;

    try {
      saveDraft({
        type: 'post',
        title,
        content,
        postId
      });

      setLastSavedAt(new Date());
      setHasSavedDraft(true);
      showSuccess('임시저장 완료', '작성 중인 내용이 저장되었습니다.');
    } catch (error) {
      console.error('Failed to save draft:', error);
      showWarning('임시저장 실패', '내용을 저장하지 못했습니다.');
    }
  }, [enabled, postId, showSuccess, showWarning]);

  // 자동저장 처리
  const handleAutoSave = useCallback((title: string | undefined, content: string) => {
    if (!enabled || !autoSave || !content.trim()) return;

    // 내용 변경 감지
    const hasChanged =
      contentRef.current.title !== title ||
      contentRef.current.content !== content;

    if (!hasChanged) return;

    contentRef.current = { title, content };

    // 기존 타이머 취소
    if (autoSaveTimerRef.current) {
      clearTimeout(autoSaveTimerRef.current);
    }

    // 새로운 자동저장 타이머 설정
    autoSaveTimerRef.current = setTimeout(() => {
      setIsAutoSaving(true);

      try {
        saveDraft({
          type: 'post',
          title,
          content,
          postId
        });

        setLastSavedAt(new Date());
        setHasSavedDraft(true);
      } catch (error) {
        console.error('Auto-save failed:', error);
      } finally {
        setIsAutoSaving(false);
      }
    }, AUTO_SAVE_DELAY);
  }, [enabled, autoSave, postId]);

  // 임시저장 삭제
  const removeDraft = useCallback(() => {
    if (!enabled) return;

    const success = deletePostDraft(postId);
    if (success) {
      setHasSavedDraft(false);
      setLastSavedAt(null);
      showInfo('임시저장 삭제', '임시저장된 내용이 삭제되었습니다.');
    }
  }, [enabled, postId, showInfo]);

  // 컴포넌트 언마운트 시 타이머 정리
  useEffect(() => {
    return () => {
      if (autoSaveTimerRef.current) {
        clearTimeout(autoSaveTimerRef.current);
      }
    };
  }, []);

  return {
    // 상태
    isAutoSaving,
    lastSavedAt,
    hasSavedDraft,

    // 액션
    loadDraft,
    saveDraftManual,
    handleAutoSave,
    removeDraft,

    // 유틸리티
    formatLastSaved: lastSavedAt
      ? `${lastSavedAt.toLocaleTimeString()}에 저장됨`
      : null
  };
}
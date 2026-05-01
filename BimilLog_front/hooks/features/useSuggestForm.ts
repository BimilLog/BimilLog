"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useToast } from "@/hooks";
import { useAuthStore } from "@/stores/auth.store";
import { submitReportAction } from "@/lib/actions/user";
import { logger } from "@/lib/utils/logger";

export type SuggestionType = "ERROR" | "IMPROVEMENT";
type SuggestionTypeValue = SuggestionType | "";

const MIN_LENGTH = 10;
const MAX_LENGTH = 500;
const DEBOUNCE_MS = 3000;

interface UseSuggestFormResult {
  suggestionType: SuggestionTypeValue;
  setSuggestionType: (value: SuggestionTypeValue) => void;
  content: string;
  setContent: (value: string) => void;
  isSubmitting: boolean;
  isSubmitted: boolean;
  resetForm: () => void;
  handleSubmit: (e: React.FormEvent) => Promise<void>;
  contentLength: number;
  isAtLimit: boolean;
  isNearLimit: boolean;
  isErrored: boolean;
  textareaRef: React.RefObject<HTMLTextAreaElement | null>;
  formRef: React.RefObject<HTMLDivElement | null>;
  /** F-12-BUG-9: 익명/실명 인지 — 제출 시 결정될 reporterName 미리보기. */
  effectiveReporterName: string;
  isAuthenticated: boolean;
  MIN_LENGTH: number;
  MAX_LENGTH: number;
}

/**
 * 건의하기 폼 통합 훅 (라운드 12).
 *
 * 라운드 7 `useWriteForm` 패턴 차용:
 * - ref 기반 beforeunload 가드 (F-12-BUG-4) — listener 1회 등록
 * - 3초 디바운스 (lastSubmitTime)
 * - 인증 상태는 제출 시점에만 `useAuthStore.getState()` 로 접근하여 memo 컴포넌트 리렌더 회피 (F-12-BUG-7)
 * - useToast 도 selector 기반이라 입력마다 리렌더 없음
 *
 * 폼 상태:
 * - `suggestionType`: 라디오 그룹 선택 값 (IMPROVEMENT/ERROR)
 * - `content`: 본문 (10~500자)
 * - `isSubmitting`: Server Action 진행 중
 * - `isSubmitted`: 제출 성공 → SuggestSuccessExits 카드 표시 (F-12-BUG-10)
 *
 * 카운터 임계 (F-12-BUG-5):
 * - `isNearLimit`: 80% (400자) 이상 → 카운터 색상 stamp-red
 * - `isAtLimit`: 100% (500자) 도달 → font-bold + sr-only live region
 */
export function useSuggestForm(): UseSuggestFormResult {
  // useAuthStore 직접 구독 — 화면 카피("익명으로 접수됩니다" vs "{이름} 명의로 접수됩니다") 가
  // 인증 상태에 반응해야 하므로 selector 기반 구독 유지 (F-12-BUG-9 우선).
  // memo 무효화 비용은 본 페이지 한 군데만 영향이라 허용.
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const memberName = useAuthStore((state) => state.user?.memberName);

  const { showError, showWarning, showFeedback } = useToast();

  const [suggestionType, setSuggestionType] = useState<SuggestionTypeValue>("");
  const [content, setContent] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [lastSubmitTime, setLastSubmitTime] = useState(0);

  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const formRef = useRef<HTMLDivElement | null>(null);

  // F-12-BUG-4: beforeunload 가드용 latest ref
  const contentRef = useRef(content);
  const isSubmittingRef = useRef(isSubmitting);
  const isSubmittedRef = useRef(isSubmitted);

  useEffect(() => {
    contentRef.current = content;
  }, [content]);
  useEffect(() => {
    isSubmittingRef.current = isSubmitting;
  }, [isSubmitting]);
  useEffect(() => {
    isSubmittedRef.current = isSubmitted;
  }, [isSubmitted]);

  // ref 기반 beforeunload — 마운트 시 1회 등록
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (
        contentRef.current.trim().length >= 1 &&
        !isSubmittingRef.current &&
        !isSubmittedRef.current
      ) {
        e.preventDefault();
        e.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, []);

  const resetForm = useCallback(() => {
    setSuggestionType("");
    setContent("");
    setIsSubmitted(false);
  }, []);

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();

      if (!suggestionType || !content.trim()) {
        showWarning("입력 확인", "건의 종류와 내용을 모두 입력해주세요.");
        return;
      }

      const trimmed = content.trim();
      if (trimmed.length < MIN_LENGTH) {
        showWarning(
          "조금 더 적어주세요",
          `건의 내용은 최소 ${MIN_LENGTH}자 이상 입력해주세요.`
        );
        return;
      }
      if (trimmed.length > MAX_LENGTH) {
        showWarning(
          "글자 수 확인",
          `건의 내용은 최대 ${MAX_LENGTH}자까지 입력할 수 있어요.`
        );
        return;
      }

      const now = Date.now();
      if (now - lastSubmitTime < DEBOUNCE_MS) {
        showWarning("중복 제출 방지", "3초 후에 다시 시도해주세요.");
        return;
      }

      setIsSubmitting(true);

      try {
        // F-12-BUG-7: 제출 시점에만 최신 인증 정보를 getState 로 가져옴
        const authSnapshot = useAuthStore.getState();
        const reporterId =
          authSnapshot.isAuthenticated && authSnapshot.user?.memberId
            ? authSnapshot.user.memberId
            : null;
        const reporterName =
          authSnapshot.isAuthenticated && authSnapshot.user?.memberName
            ? authSnapshot.user.memberName
            : "익명";

        const response = await submitReportAction({
          reportType: suggestionType,
          content: trimmed,
          reporterId,
          reporterName,
        });

        if (response.success) {
          setLastSubmitTime(Date.now());
          setIsSubmitted(true);

          showFeedback(
            "고맙습니다, 편지를 잘 받았어요",
            "보내주신 의견은 차근차근 살펴볼게요."
          );
        } else {
          // 폼 데이터는 보존 — 사용자가 다시 시도할 수 있도록
          showError(
            "건의사항 접수 실패",
            response.error || "건의사항 접수에 실패했습니다. 다시 시도해주세요."
          );
        }
      } catch (error) {
        logger.error("Submit suggestion failed:", error);
        showError(
          "건의사항 접수 실패",
          "건의사항 접수 중 오류가 발생했습니다. 다시 시도해주세요."
        );
      } finally {
        setIsSubmitting(false);
      }
    },
    [
      suggestionType,
      content,
      lastSubmitTime,
      showWarning,
      showError,
      showFeedback,
    ]
  );

  const contentLength = content.length;
  const isNearLimit = contentLength >= Math.floor(MAX_LENGTH * 0.8); // 400자
  const isAtLimit = contentLength >= MAX_LENGTH;
  const isErrored = false; // 현재는 즉시 차단 토스트만 사용 — 인라인 에러는 미사용

  const effectiveReporterName =
    isAuthenticated && memberName ? memberName : "익명";

  return {
    suggestionType,
    setSuggestionType,
    content,
    setContent,
    isSubmitting,
    isSubmitted,
    resetForm,
    handleSubmit,
    contentLength,
    isAtLimit,
    isNearLimit,
    isErrored,
    textareaRef,
    formRef,
    effectiveReporterName,
    isAuthenticated,
    MIN_LENGTH,
    MAX_LENGTH,
  };
}

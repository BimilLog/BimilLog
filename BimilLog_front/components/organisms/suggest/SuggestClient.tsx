"use client";

import { memo, useEffect, useId } from "react";
import dynamic from "next/dynamic";
import { Button } from "@/components";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Textarea,
} from "@/components";
import { Label } from "@/components";
import { Spinner } from "@/components";
import { Send, FileText } from "lucide-react";
import { useToast } from "@/hooks";
import { cn } from "@/lib/utils";
import {
  SuggestTypeRadioGroup,
  SUGGEST_OPTIONS,
} from "./SuggestTypeRadioGroup";
import { SuggestSuccessExits } from "./SuggestSuccessExits";
import { useSuggestForm } from "@/hooks/features/useSuggestForm";

// ToastContainer 만 dynamic 유지 (라운드 1~11 표준).
// Textarea 는 native 요소라 라운드 7 처럼 정적 import 로 단순화 (F-12-BUG-8).
const ToastContainer = dynamic(
  () =>
    import("@/components/molecules/feedback/toast").then((mod) => ({
      default: mod.ToastContainer,
    })),
  { ssr: false, loading: () => null }
);

/**
 * 건의하기 페이지 클라이언트 컴포넌트 (라운드 12).
 *
 * 라운드 12 fix 요약:
 * - F-12-BUG-2: 카드 → role=radiogroup 키보드 접근 (SuggestTypeRadioGroup 분리)
 * - F-12-BUG-4: beforeunload 가드 (useSuggestForm)
 * - F-12-BUG-5: 카운터 임계 색상 + sr-only live region
 * - F-12-BUG-6: 종류 선택 후 자동 스크롤 + 포커스
 * - F-12-BUG-8: Textarea dynamic import 제거
 * - F-12-BUG-9: 익명/실명 인지 카피 (effectiveReporterName)
 * - F-12-BUG-10: 제출 성공 후 사후 진입로 카드 (SuggestSuccessExits)
 * - F-12-BUG-12: aria-required / aria-describedby
 */
const SuggestClient = memo(function SuggestClient() {
  const { toasts, removeToast } = useToast();
  const {
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
    textareaRef,
    formRef,
    effectiveReporterName,
    isAuthenticated,
    MIN_LENGTH,
    MAX_LENGTH,
  } = useSuggestForm();

  const helpId = useId();
  const counterId = useId();
  const limitNoticeId = useId();

  const selectedType = SUGGEST_OPTIONS.find(
    (option) => option.value === suggestionType
  );

  // F-12-BUG-6: 종류 선택 시 폼 카드로 자동 스크롤 + textarea 포커스
  useEffect(() => {
    if (!suggestionType || isSubmitted) return;

    const id = window.requestAnimationFrame(() => {
      formRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
      textareaRef.current?.focus({ preventScroll: true });
    });
    return () => window.cancelAnimationFrame(id);
  }, [suggestionType, isSubmitted, formRef, textareaRef]);

  // 인증 상태에 따른 인지 카피 (F-12-BUG-9)
  const reporterNoticeCopy = isAuthenticated
    ? `${effectiveReporterName} 님 명의로 접수돼요`
    : "익명으로 접수돼요";

  // 카운터 임계 색상 (F-12-BUG-5)
  const counterClass = cn(
    "text-xs transition-colors break-keep",
    isAtLimit
      ? "text-stamp-red font-bold"
      : isNearLimit
        ? "text-stamp-red font-semibold"
        : "text-brand-secondary"
  );

  return (
    <>
      <main className="container mx-auto px-4 pb-16">
        <div className="max-w-4xl mx-auto">
          {/* 건의 종류 선택 — F-12-BUG-2 */}
          <SuggestTypeRadioGroup
            value={suggestionType}
            onChange={setSuggestionType}
          />

          {/* 사후 진입로 (F-12-BUG-10) — 제출 성공 후 폼 자리에 표시 */}
          {isSubmitted && (
            <div ref={formRef}>
              <SuggestSuccessExits
                reporterName={effectiveReporterName}
                isAuthenticated={isAuthenticated}
                onWriteAnother={resetForm}
              />
            </div>
          )}

          {/* 건의 폼 — 종류 선택 후 + 미제출 상태에서만 표시 */}
          {suggestionType && !isSubmitted && (
            <div ref={formRef}>
              <Card className="border border-ink-soft shadow-brand-xl bg-paper-50/90 backdrop-blur-sm">
                <CardHeader className="text-center">
                  <div className="flex items-center justify-center space-x-3 mb-2">
                    {selectedType && (
                      <div
                        className={cn(
                          "w-10 h-10 rounded-full flex items-center justify-center",
                          selectedType.iconBg
                        )}
                      >
                        <selectedType.icon className="w-5 h-5 text-paper-50" />
                      </div>
                    )}
                    <CardTitle className="font-display text-2xl text-ink break-keep">
                      {selectedType?.label}
                    </CardTitle>
                  </div>
                  <p className="text-ink-soft break-keep">
                    {selectedType?.description}
                  </p>
                  {/* F-12-BUG-9: 익명/실명 인지 sub-text */}
                  <p className="text-xs text-ink-soft mt-2 break-keep">
                    {reporterNoticeCopy}
                  </p>
                </CardHeader>

                <CardContent className="space-y-6">
                  <form
                    onSubmit={handleSubmit}
                    className="space-y-6"
                    aria-label="건의 내용 작성"
                  >
                    {/* 내용 */}
                    <div className="space-y-2">
                      <Label
                        htmlFor="suggest-content"
                        className="text-sm font-medium text-ink-soft break-keep"
                      >
                        건의 내용{" "}
                        <span
                          className="text-stamp-red"
                          aria-hidden="true"
                        >
                          *
                        </span>
                        <span className="sr-only">필수</span>
                      </Label>
                      <Textarea
                        ref={textareaRef}
                        id="suggest-content"
                        placeholder={`${selectedType?.label}에 대해 자세히 들려주세요...`}
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        required
                        rows={8}
                        maxLength={MAX_LENGTH}
                        aria-required="true"
                        aria-describedby={`${helpId} ${counterId}`}
                        className={cn(
                          "border-ink-soft focus:border-stamp-red focus:ring-stamp-red resize-none",
                          "whitespace-pre-wrap break-keep" // F-12-006 한국어 줄바꿈
                        )}
                      />
                      <div className="flex flex-col gap-1 sm:flex-row sm:justify-between sm:items-center">
                        <p
                          id={helpId}
                          className="text-xs text-brand-secondary break-keep"
                        >
                          최소 {MIN_LENGTH}자 이상, 구체적인 설명일수록 도움이 돼요.
                        </p>
                        <p
                          id={counterId}
                          className={counterClass}
                          aria-live="off"
                        >
                          {contentLength}/{MAX_LENGTH}
                        </p>
                      </div>
                      {/* F-12-BUG-5: 한도 도달 sr-only 알림 */}
                      <p
                        id={limitNoticeId}
                        className={cn(
                          isAtLimit ? "text-stamp-red text-xs break-keep" : "sr-only"
                        )}
                        aria-live="polite"
                      >
                        {isAtLimit
                          ? `최대 ${MAX_LENGTH}자까지 입력할 수 있어요.`
                          : ""}
                      </p>
                    </div>

                    {/* 제출 버튼 */}
                    <Button
                      type="submit"
                      disabled={isSubmitting}
                      className="w-full bg-stamp-red text-paper-50 hover:bg-stamp-red/90 py-3 text-lg font-semibold"
                    >
                      {isSubmitting ? (
                        <span className="flex items-center justify-center space-x-2">
                          <Spinner size="sm" className="text-paper-50" />
                          <span>접수 중...</span>
                        </span>
                      ) : (
                        <span className="flex items-center space-x-2">
                          <Send className="w-5 h-5" aria-hidden="true" />
                          <span>의견 보내기</span>
                        </span>
                      )}
                    </Button>
                  </form>
                </CardContent>
              </Card>
            </div>
          )}

          {/* 안내 사항 — 제출 전, 종류 선택 전 항상 노출 */}
          {!isSubmitted && (
            <Card
              variant="soft"
              className="mt-8 border border-ink-soft shadow-brand-lg bg-paper-aged"
            >
              <CardContent className="p-6">
                <h3 className="font-display text-lg font-semibold text-ink mb-3 flex items-center space-x-2 break-keep">
                  <FileText
                    className="w-5 h-5 text-postal-navy"
                    aria-hidden="true"
                  />
                  <span>편지 안내</span>
                </h3>
                <ul className="space-y-2 text-sm text-ink-soft break-keep">
                  <li>· 바라는 기능이나 기능 개선 아이디어를 들려주세요.</li>
                  <li>· 버그, 오류를 발견하셨다면 함께 알려주세요.</li>
                  <li>· 욕설, 비방, 스팸성 내용은 삭제될 수 있어요.</li>
                </ul>
              </CardContent>
            </Card>
          )}
        </div>
      </main>

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
});

SuggestClient.displayName = "SuggestClient";

export default SuggestClient;

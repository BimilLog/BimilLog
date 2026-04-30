"use client";

import React, { useEffect, useRef } from "react";
import { Button, Input } from "@/components";
import { LazyEditor } from "@/lib/utils/lazy-components";

interface CommentReplyFormProps {
  targetUserName: string;
  replyContent: string;
  replyPassword: string;
  isAuthenticated: boolean;
  isSubmittingReply: boolean;
  replyPlainTextLength: number;
  onReplySubmit: () => void;
  onCancelReply: () => void;
  setReplyContent: (content: string) => void;
  setReplyPassword: (password: string) => void;
}

export const CommentReplyForm: React.FC<CommentReplyFormProps> = React.memo(({
  targetUserName,
  replyContent,
  replyPassword,
  isAuthenticated,
  isSubmittingReply,
  replyPlainTextLength,
  onReplySubmit,
  onCancelReply,
  setReplyContent,
  setReplyPassword,
}) => {
  /**
   * 라운드 8 B-8-014: 답글 폼 자동 포커스.
   * Quill 인스턴스 ref 노출이 큰 변경이므로, 컨테이너 내부의
   * `.ql-editor` 또는 `<input>` 에 비로그인 분기에 따라 포커스를 준다.
   * Quill 마운트 지연이 있어 setTimeout(120ms) 으로 한 번 시도.
   */
  const containerRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const target = containerRef.current;
    if (!target) return;
    const id = window.setTimeout(() => {
      const passwordInput = !isAuthenticated
        ? target.querySelector<HTMLInputElement>('input[type="password"]')
        : null;
      if (passwordInput) {
        passwordInput.focus();
        return;
      }
      const editorEl = target.querySelector<HTMLElement>('.ql-editor');
      editorEl?.focus();
    }, 120);
    return () => window.clearTimeout(id);
    // 마운트(=새로운 답글 대상 선택) 시 1회만 포커스
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div
      ref={containerRef}
      className="mt-4 p-3 sm:p-4 bg-paper-aged dark:bg-postal-navy/20 rounded-lg border border-postal-navy/30 dark:border-postal-navy/40 space-y-3"
      role="group"
      aria-label={`${targetUserName}님에게 답글 작성`}
    >
      <h4 className="text-sm font-semibold text-postal-navy dark:text-paper-100 break-keep">
        {targetUserName}님에게 답글 작성
      </h4>
      {/* 비로그인 사용자는 비밀번호 입력 필요 */}
      {!isAuthenticated && (
        <>
          <label htmlFor="reply-password" className="sr-only">
            비밀번호 (1000~9999)
          </label>
          <Input
            id="reply-password"
            type="password"
            placeholder="비밀번호 (1000~9999)"
            value={replyPassword}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
              setReplyPassword(e.target.value.replace(/\D/g, "").slice(0, 4))
            }
            inputMode="numeric"
            pattern="[1-9][0-9]{3}"
            maxLength={4}
            autoComplete="off"
          />
        </>
      )}
      <LazyEditor
        value={replyContent}
        onChange={setReplyContent}
      />
      <div className="flex items-center justify-between">
        <p className="text-xs text-brand-secondary break-keep">HTML 형식 지원</p>
        {replyContent && (
          <p
            aria-live="polite"
            aria-label={`답글 글자 수 ${replyPlainTextLength}/255`}
            className={`text-xs ${
              replyPlainTextLength >= 255
                ? "text-stamp-red font-semibold"
                : replyPlainTextLength >= 230
                ? "text-seal-gold font-medium"
                : "text-brand-muted"
            }`}
          >
            {replyPlainTextLength}/255자
          </p>
        )}
      </div>
      {replyPlainTextLength > 255 && (
        <p className="text-stamp-red text-sm break-keep" role="alert">
          댓글은 최대 255자까지 입력 가능합니다
        </p>
      )}
      <div className="flex gap-2">
        <Button
          size="sm"
          onClick={onReplySubmit}
          disabled={isSubmittingReply || replyPlainTextLength > 255}
          aria-busy={isSubmittingReply}
          className="flex-1 sm:flex-none"
        >
          작성
        </Button>
        <Button
          size="sm"
          variant="ghost"
          onClick={onCancelReply}
          className="flex-1 sm:flex-none"
        >
          취소
        </Button>
      </div>
    </div>
  );
});

CommentReplyForm.displayName = "CommentReplyForm";

"use client";

import React from "react";
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
  return (
    <div className="mt-4 p-3 sm:p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-700 space-y-3">
      <h4 className="text-sm font-semibold text-blue-700 dark:text-blue-400">
        {targetUserName}님에게 답글 작성
      </h4>
      {/* 비로그인 사용자는 비밀번호 입력 필요 */}
      {!isAuthenticated && (
        <Input
          type="password"
          placeholder="비밀번호 (1000~9999)"
          value={replyPassword}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setReplyPassword(e.target.value)}
        />
      )}
      <LazyEditor
        value={replyContent}
        onChange={setReplyContent}
      />
      <div className="flex items-center justify-between">
        <p className="text-xs text-brand-secondary">HTML 형식 지원</p>
        {replyContent && (
          <p
            className={`text-xs ${
              replyPlainTextLength >= 255
                ? "text-red-600 font-semibold"
                : replyPlainTextLength >= 230
                ? "text-orange-500 font-medium"
                : "text-brand-muted"
            }`}
          >
            {replyPlainTextLength}/255자
          </p>
        )}
      </div>
      {replyPlainTextLength > 255 && (
        <p className="text-red-500 text-sm">
          댓글은 최대 255자까지 입력 가능합니다
        </p>
      )}
      <div className="flex gap-2">
        <Button
          size="sm"
          onClick={onReplySubmit}
          disabled={isSubmittingReply || replyPlainTextLength > 255}
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

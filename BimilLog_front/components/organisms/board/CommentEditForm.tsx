"use client";

import React from "react";
import { Button, Input, Spinner } from "@/components";
import { LazyEditor } from "@/lib/utils/lazy-components";

interface CommentEditFormProps {
  editContent: string;
  editPassword: string;
  isAnonymous: boolean;
  isUpdatingComment: boolean;
  editPlainTextLength: number;
  onUpdateComment: () => void;
  onCancelEdit: () => void;
  setEditContent: (content: string) => void;
  setEditPassword: (password: string) => void;
}

export const CommentEditForm: React.FC<CommentEditFormProps> = React.memo(({
  editContent,
  editPassword,
  isAnonymous,
  isUpdatingComment,
  editPlainTextLength,
  onUpdateComment,
  onCancelEdit,
  setEditContent,
  setEditPassword,
}) => {
  return (
    <div
      className="p-3 sm:p-4 bg-paper-aged dark:bg-postal-navy/15 rounded-lg space-y-3 border border-postal-navy/20 dark:border-postal-navy/40"
      role="group"
      aria-label="댓글 수정 폼"
    >
      <LazyEditor
        value={editContent}
        onChange={setEditContent}
      />
      <div className="flex items-center justify-between">
        <p className="text-xs text-brand-secondary break-keep">HTML 형식 지원</p>
        {editContent && (
          <p
            aria-live="polite"
            aria-label={`수정 댓글 글자 수 ${editPlainTextLength}/255`}
            className={`text-xs ${
              editPlainTextLength >= 255
                ? "text-stamp-red font-semibold"
                : editPlainTextLength >= 230
                ? "text-seal-gold font-medium"
                : "text-brand-muted"
            }`}
          >
            {editPlainTextLength}/255자
          </p>
        )}
      </div>
      {editPlainTextLength > 255 && (
        <p className="text-stamp-red text-sm break-keep" role="alert">
          댓글은 최대 255자까지 입력 가능합니다
        </p>
      )}
      {/* 익명 댓글 수정 시에만 비밀번호 입력 필요 */}
      {isAnonymous && (
        <>
          <label htmlFor="edit-password" className="sr-only">
            비밀번호 (1000~9999)
          </label>
          <Input
            id="edit-password"
            type="password"
            placeholder="비밀번호 (1000~9999)"
            value={editPassword}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
              setEditPassword(e.target.value.replace(/\D/g, "").slice(0, 4))
            }
            disabled={isUpdatingComment}
            inputMode="numeric"
            pattern="[1-9][0-9]{3}"
            maxLength={4}
            autoComplete="off"
          />
        </>
      )}
      <div className="flex flex-col sm:flex-row gap-2 sm:justify-end">
        <Button
          onClick={onUpdateComment}
          size="sm"
          className="w-full sm:w-auto"
          disabled={isUpdatingComment || editPlainTextLength > 255}
        >
          {isUpdatingComment ? (
            <>
              <Spinner size="sm" className="mr-2" />
              수정 중...
            </>
          ) : (
            "수정완료"
          )}
        </Button>
        <Button
          variant="ghost"
          onClick={onCancelEdit}
          size="sm"
          className="w-full sm:w-auto"
          disabled={isUpdatingComment}
        >
          취소
        </Button>
      </div>
    </div>
  );
});

CommentEditForm.displayName = "CommentEditForm";

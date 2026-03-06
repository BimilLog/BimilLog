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
    <div className="p-3 sm:p-4 bg-gray-100 dark:bg-gray-800/70 rounded-lg space-y-3">
      <LazyEditor
        value={editContent}
        onChange={setEditContent}
      />
      <div className="flex items-center justify-between">
        <p className="text-xs text-brand-secondary">HTML 형식 지원</p>
        {editContent && (
          <p
            className={`text-xs ${
              editPlainTextLength >= 255
                ? "text-red-600 font-semibold"
                : editPlainTextLength >= 230
                ? "text-orange-500 font-medium"
                : "text-brand-muted"
            }`}
          >
            {editPlainTextLength}/255자
          </p>
        )}
      </div>
      {editPlainTextLength > 255 && (
        <p className="text-red-500 text-sm">
          댓글은 최대 255자까지 입력 가능합니다
        </p>
      )}
      {/* 익명 댓글 수정 시에만 비밀번호 입력 필요 */}
      {isAnonymous && (
        <Input
          type="password"
          placeholder="비밀번호 (1000~9999)"
          value={editPassword}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditPassword(e.target.value)}
          disabled={isUpdatingComment}
        />
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

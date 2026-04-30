"use client";

import React, { useState, useMemo, useId } from "react";
import { Button, Card, CardContent, CardHeader, CardTitle, Input } from "@/components";
import { Send, Lightbulb } from "lucide-react";
import { LazyEditor } from "@/lib/utils/lazy-components";
import { stripHtmlTags } from "@/lib/utils/sanitize";

interface CommentFormProps {
  isAuthenticated: boolean;
  isSubmittingComment: boolean;
  /**
   * 라운드 8 B-8-005: onSubmit 시그니처에 onSuccess 콜백 옵션 추가.
   * 부모(useCommentInteraction.handleCommentSubmitForSection) 가 Server Action
   * 성공 시 호출하면 폼이 reset 된다. 실패 시에는 reset 되지 않아 입력 유실 방지.
   * 기존 호출자가 onSuccess 미사용이면 폼은 reset 되지 않는다.
   */
  onSubmit: (
    comment: string,
    password: string,
    callbacks?: { onSuccess?: () => void }
  ) => void;
}

export const CommentForm: React.FC<CommentFormProps> = React.memo(({
  isAuthenticated,
  isSubmittingComment,
  onSubmit,
}) => {
  const [comment, setComment] = useState("");
  const [password, setPassword] = useState("");
  const counterId = useId();

  // HTML 태그를 제거하고 순수 텍스트 길이 계산
  const plainTextLength = useMemo(() => {
    return stripHtmlTags(comment).length;
  }, [comment]);

  // 폼 제출 핸들러
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // 검증
    if (!comment.trim()) return;
    if (plainTextLength > 255) return;
    if (!isAuthenticated && (!password || !/^[1-9]\d{3}$/.test(password))) return;

    // 라운드 8 B-8-005: 성공 시에만 reset 되도록 콜백을 onSubmit 으로 전달.
    onSubmit(comment, password, {
      onSuccess: () => {
        setComment("");
        setPassword("");
      },
    });
  };

  // 제출 가능 여부
  const canSubmit = useMemo(() => {
    if (isSubmittingComment) return false;
    if (!comment.trim()) return false;
    if (plainTextLength > 255) return false;
    if (!isAuthenticated && (!password || !/^[1-9]\d{3}$/.test(password))) return false;
    return true;
  }, [isSubmittingComment, comment, plainTextLength, isAuthenticated, password]);

  return (
    <Card variant="elevated" className="mb-8">
      <CardHeader>
        <CardTitle className="text-lg break-keep">댓글 작성</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={handleSubmit} className="space-y-4" aria-label="댓글 작성 폼">
          {/* 댓글 내용 입력 */}
          <div className="space-y-2">
            <label htmlFor="comment-editor" className="sr-only">
              댓글 내용
            </label>
            <LazyEditor value={comment} onChange={setComment} />

            <div className="flex items-center justify-between">
              <p className="text-xs text-brand-secondary flex items-center space-x-1 break-keep">
                <Lightbulb className="w-3 h-3 stroke-seal-gold fill-paper-100" aria-hidden="true" />
                <span>다양한 스타일로 댓글을 꾸며보세요.</span>
              </p>
              {comment && (
                <p
                  id={counterId}
                  aria-live="polite"
                  aria-label={`댓글 글자 수 ${plainTextLength}/255`}
                  className={`text-xs ${
                    plainTextLength >= 255
                      ? "text-stamp-red font-semibold"
                      : plainTextLength >= 230
                      ? "text-seal-gold font-medium"
                      : "text-brand-muted"
                  }`}
                >
                  {plainTextLength}/255자
                </p>
              )}
            </div>

            {plainTextLength > 255 && (
              <p className="text-stamp-red text-sm break-keep" role="alert">
                댓글은 최대 255자까지 입력 가능합니다
              </p>
            )}
          </div>

          {/* 비로그인 사용자용 비밀번호 입력 */}
          {!isAuthenticated && (
            <div>
              <label htmlFor="comment-password" className="sr-only">
                비밀번호 (1000~9999)
              </label>
              <Input
                id="comment-password"
                type="password"
                placeholder="비밀번호 (1000~9999)"
                value={password}
                onChange={(e) => setPassword(e.target.value.replace(/\D/g, ""))}
                maxLength={4}
                inputMode="numeric"
                pattern="[1-9][0-9]{3}"
                autoComplete="off"
              />
              {password && !/^[1-9]\d{3}$/.test(password) && (
                <p className="text-stamp-red text-sm mt-1 break-keep" role="alert">
                  1000~9999 사이의 숫자를 입력해주세요
                </p>
              )}
            </div>
          )}

          {/* 제출 버튼 */}
          <div className="flex justify-end">
            <Button type="submit" disabled={!canSubmit} aria-busy={isSubmittingComment} className="mt-2">
              <Send className="w-4 h-4 mr-2 stroke-blue-600 fill-blue-100" aria-hidden="true" />
              작성
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
});

CommentForm.displayName = "CommentForm";

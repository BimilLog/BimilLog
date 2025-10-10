"use client";

import React, { useState, useMemo } from "react";
import { Button, Card, CardContent, CardHeader, CardTitle, Input } from "@/components";
import { Send, Lightbulb } from "lucide-react";
import { LazyEditor } from "@/lib/utils/lazy-components";
import { stripHtmlTags } from "@/lib/utils/sanitize";

interface CommentFormProps {
  isAuthenticated: boolean;
  isSubmittingComment: boolean;
  onSubmit: (comment: string, password: string) => void;
}

export const CommentForm: React.FC<CommentFormProps> = ({
  isAuthenticated,
  isSubmittingComment,
  onSubmit,
}) => {
  const [comment, setComment] = useState("");
  const [password, setPassword] = useState("");

  // HTML 태그를 제거하고 순수 텍스트 길이 계산
  const plainTextLength = useMemo(() => {
    return stripHtmlTags(comment).length;
  }, [comment]);

  // 폼 제출 핸들러
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // 검증
    if (!comment.trim()) {
      return;
    }
    if (plainTextLength > 255) {
      return;
    }
    if (!isAuthenticated && (!password || !/^[1-9]\d{3}$/.test(password))) {
      return;
    }

    // HTML 형식으로 전송
    onSubmit(comment, password);

    // 폼 초기화
    setComment("");
    setPassword("");
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
        <CardTitle className="text-lg">댓글 작성</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* 댓글 내용 입력 */}
          <div className="space-y-2">
            <LazyEditor value={comment} onChange={setComment} height={200} />

            <div className="flex items-center justify-between">
              <p className="text-xs text-brand-secondary flex items-center space-x-1">
                <Lightbulb className="w-3 h-3 stroke-indigo-600 fill-indigo-100" />
                <span>다양한 스타일로 댓글을 꾸며보세요.</span>
              </p>
              {comment && (
                <p
                  className={`text-xs ${
                    plainTextLength >= 255
                      ? "text-red-600 font-semibold"
                      : plainTextLength >= 230
                      ? "text-orange-500 font-medium"
                      : "text-brand-muted"
                  }`}
                >
                  {plainTextLength}/255자
                </p>
              )}
            </div>

            {plainTextLength > 255 && (
              <p className="text-red-500 text-sm">
                댓글은 최대 255자까지 입력 가능합니다
              </p>
            )}
          </div>

          {/* 비로그인 사용자용 비밀번호 입력 */}
          {!isAuthenticated && (
            <div>
              <Input
                type="password"
                placeholder="비밀번호 (1000~9999)"
                value={password}
                onChange={(e) => setPassword(e.target.value.replace(/\D/g, ""))}
                maxLength={4}
              />
              {password && !/^[1-9]\d{3}$/.test(password) && (
                <p className="text-red-500 text-sm mt-1">
                  1000~9999 사이의 숫자를 입력해주세요
                </p>
              )}
            </div>
          )}

          {/* 제출 버튼 */}
          <div className="flex justify-end">
            <Button type="submit" disabled={!canSubmit} className="mt-2">
              <Send className="w-4 h-4 mr-2 stroke-blue-600 fill-blue-100" />
              작성
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};

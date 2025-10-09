"use client";

import React from "react";
import { useForm } from "react-hook-form";
import { Button, Card, CardContent, CardHeader, CardTitle, Input, Textarea } from "@/components";
import { Send } from "lucide-react";

interface CommentFormData {
  comment: string;
  password: string;
}

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
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<CommentFormData>({
    mode: "onChange",
    defaultValues: {
      comment: "",
      password: "",
    },
  });

  const password = watch("password");
  const comment = watch("comment");

  const onSubmitForm = (data: CommentFormData) => {
    onSubmit(data.comment, data.password);
    reset();
  };

  return (
    <Card variant="elevated" className="mb-8">
      <CardHeader>
        <CardTitle className="text-lg">댓글 작성</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={handleSubmit(onSubmitForm)}>
          {!isAuthenticated && (
            <div className="mb-4">
              <Input
                type="password"
                placeholder="비밀번호 (1000~9999)"
                {...register("password", {
                  required: !isAuthenticated,
                  pattern: {
                    value: /^[1-9]\d{3}$/,
                    message: "1000~9999 사이의 숫자를 입력해주세요",
                  },
                })}
                maxLength={4}
              />
              {errors.password && (
                <p className="text-red-500 text-sm mt-1">
                  {errors.password.message || "1000~9999 사이의 숫자를 입력해주세요"}
                </p>
              )}
            </div>
          )}
          <div className="space-y-2">
            <div className="flex space-x-4">
              <Textarea
                placeholder="댓글을 입력하세요..."
                {...register("comment", {
                  required: "댓글을 입력해주세요",
                  minLength: {
                    value: 1,
                    message: "댓글을 입력해주세요",
                  },
                  maxLength: {
                    value: 255,
                    message: "댓글은 최대 255자까지 입력 가능합니다"
                  }
                })}
                maxLength={255}
                className="flex-1"
              />
              <Button
                type="submit"
                disabled={
                  isSubmittingComment ||
                  !comment.trim() ||
                  (!isAuthenticated && (!password || !/^[1-9]\d{3}$/.test(password)))
                }
              >
                <Send className="w-4 h-4 stroke-blue-600 fill-blue-100" />
              </Button>
            </div>
            {comment && comment.length > 0 && (
              <div className="text-sm text-gray-500 text-right">
                {comment.length} / 255자
              </div>
            )}
            {errors.comment && (
              <p className="text-red-500 text-sm">{errors.comment.message}</p>
            )}
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
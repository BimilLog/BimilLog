"use client";

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
    formState: { errors, isValid },
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
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg mb-8">
      <CardHeader>
        <CardTitle className="text-lg">댓글 작성</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={handleSubmit(onSubmitForm)}>
          {!isAuthenticated && (
            <div className="mb-4">
              <Input
                type="password"
                placeholder="비밀번호 (4자리 숫자)"
                {...register("password", {
                  required: !isAuthenticated,
                  pattern: {
                    value: /^\d{4}$/,
                    message: "4자리 숫자를 입력해주세요",
                  },
                })}
                maxLength={4}
              />
              {errors.password && (
                <p className="text-red-500 text-sm mt-1">
                  {errors.password.message || "4자리 숫자를 입력해주세요"}
                </p>
              )}
            </div>
          )}
          <div className="flex space-x-4">
            <Textarea
              placeholder="댓글을 입력하세요..."
              {...register("comment", {
                required: "댓글을 입력해주세요",
                minLength: {
                  value: 1,
                  message: "댓글을 입력해주세요",
                },
              })}
              className="flex-1"
            />
            <Button 
              type="submit"
              disabled={
                isSubmittingComment || 
                !comment.trim() || 
                (!isAuthenticated && (!password || !/^\d{4}$/.test(password)))
              }
            >
              <Send className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
"use client";

import { useState } from "react";
import dynamic from "next/dynamic";
import { Button } from "@/components";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { Label } from "@/components";
import { Spinner } from "@/components";
import { Lightbulb, Send, Bug, FileText } from "lucide-react";
import { useToast } from "@/hooks";
import { logger } from '@/lib/utils/logger';
import { useAuthStore } from "@/stores/auth.store";
import { submitReportAction } from "@/lib/actions/user";

// Dynamic import for heavy components
const Textarea = dynamic(
  () => import("@/components").then(mod => ({ default: mod.Textarea })),
  {
    ssr: false,
    loading: () => (
      <div className="min-h-[200px] bg-gray-100 rounded-lg animate-pulse flex items-center justify-center">
        <Spinner size="md" />
      </div>
    )
  }
);

const ToastContainer = dynamic(
  () => import("@/components/molecules/feedback/toast").then(mod => ({ default: mod.ToastContainer })),
  {
    ssr: false,
    loading: () => null
  }
);

type SuggestionType = "ERROR" | "IMPROVEMENT";

const suggestionTypes = [
  {
    value: "IMPROVEMENT" as const,
    label: "기능 개선 제안",
    description: "새로운 기능이나 기존 기능 개선 아이디어",
    icon: Lightbulb,
    color: "bg-blue-500",
  },
  {
    value: "ERROR" as const,
    label: "오류 신고",
    description: "버그, 오작동, 기술적 문제 신고",
    icon: Bug,
    color: "bg-red-500",
  },
];

export default function SuggestClient() {
  const [suggestionType, setSuggestionType] = useState<SuggestionType | "">("");
  const [content, setContent] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [lastSubmitTime, setLastSubmitTime] = useState<number>(0);
  const { showError, showWarning, showFeedback, toasts, removeToast } =
    useToast();
  const { user, isAuthenticated } = useAuthStore();

  const selectedType = suggestionTypes.find(
    (type) => type.value === suggestionType
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!suggestionType || !content.trim()) {
      showWarning("입력 확인", "건의 종류와 내용을 모두 입력해주세요.");
      return;
    }

    if (content.trim().length < 10) {
      showWarning("입력 확인", "건의 내용은 최소 10자 이상 입력해주세요.");
      return;
    }

    const now = Date.now();
    if (now - lastSubmitTime < 3000) {
      showWarning("중복 제출 방지", "3초 후에 다시 시도해주세요.");
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await submitReportAction({
        reportType: suggestionType,
        content: content.trim(),
        reporterId: isAuthenticated && user?.memberId ? user.memberId : null,
        reporterName: isAuthenticated && user?.memberName ? user.memberName : "익명",
      });

      if (response.success) {
        setLastSubmitTime(Date.now());

        showFeedback(
          "건의사항 접수 완료",
          "소중한 의견 감사합니다! 빠른 시일 내에 검토하여 반영하겠습니다.",
          {
            label: "추가 건의하기",
            onClick: () => {
              setSuggestionType("");
              setContent("");
            }
          }
        );
        setSuggestionType("");
        setContent("");
      } else {
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
  };

  return (
    <>
      <main className="container mx-auto px-4 pb-16">
        <div className="max-w-4xl mx-auto">
          {/* 건의 종류 선택 */}
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-brand-primary mb-6 text-center">
              어떤 종류의 건의사항인가요?
            </h2>
            <div className="grid md:grid-cols-2 gap-4">
              {suggestionTypes.map((type) => {
                const Icon = type.icon;
                const isSelected = suggestionType === type.value;

                return (
                  <Card
                    key={type.value}
                    className={`cursor-pointer transition-all border-2 hover:shadow-brand-lg ${
                      isSelected
                        ? "border-purple-500 shadow-brand-lg bg-purple-50"
                        : "border-gray-200 hover:border-gray-300 bg-white/80"
                    } backdrop-blur-sm`}
                    onClick={() => setSuggestionType(type.value)}
                  >
                    <CardContent className="p-6 text-center">
                      <div
                        className={`w-12 h-12 ${type.color} rounded-full flex items-center justify-center mx-auto mb-4`}
                      >
                        <Icon className="w-6 h-6 text-white" />
                      </div>
                      <h3 className="text-lg font-semibold mb-2 text-brand-primary">
                        {type.label}
                      </h3>
                      <p className="text-sm text-brand-muted">
                        {type.description}
                      </p>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          </div>

          {/* 건의 폼 */}
          {suggestionType && (
            <Card className="border-0 shadow-brand-xl bg-white/90 backdrop-blur-sm">
              <CardHeader className="text-center">
                <div className="flex items-center justify-center space-x-3 mb-2">
                  {selectedType && (
                    <div
                      className={`w-10 h-10 bg-gradient-to-r ${selectedType.color} rounded-full flex items-center justify-center`}
                    >
                      <selectedType.icon className="w-5 h-5 text-white" />
                    </div>
                  )}
                  <CardTitle className="text-2xl text-brand-primary">
                    {selectedType?.label}
                  </CardTitle>
                </div>
                <p className="text-brand-muted">{selectedType?.description}</p>
              </CardHeader>

              <CardContent className="space-y-6">
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* 내용 */}
                  <div className="space-y-2">
                    <Label
                      htmlFor="content"
                      className="text-sm font-medium text-brand-primary"
                    >
                      건의 내용 <span className="text-red-500">*</span>
                    </Label>
                    <Textarea
                      id="content"
                      placeholder={`${selectedType?.label}에 대해 자세히 설명해주세요...`}
                      value={content}
                      onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setContent(e.target.value)}
                      required
                      rows={8}
                      className="border-gray-300 focus:border-purple-500 focus:ring-purple-500 resize-none"
                      maxLength={500}
                    />
                    <div className="flex justify-between items-center">
                      <p className="text-xs text-brand-secondary">
                        구체적이고 상세한 설명일수록 더 도움이 됩니다.
                      </p>
                      <p className="text-xs text-brand-secondary">
                        {content.length}/500
                      </p>
                    </div>
                  </div>

                  {/* 제출 버튼 */}
                  <Button
                    type="submit"
                    disabled={isSubmitting}
                    className="w-full bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 py-3 text-lg font-semibold"
                  >
                    {isSubmitting ? (
                      <div className="flex items-center justify-center space-x-2">
                        <Spinner size="sm" className="text-white" />
                        <span>접수 중...</span>
                      </div>
                    ) : (
                      <div className="flex items-center space-x-2">
                        <Send className="w-5 h-5" />
                        <span>건의사항 접수하기</span>
                      </div>
                    )}
                  </Button>
                </form>
              </CardContent>
            </Card>
          )}

          {/* 안내 사항 */}
          <Card variant="soft" className="mt-8 border-0 shadow-brand-lg">
            <CardContent className="p-6">
              <h3 className="text-lg font-semibold text-brand-primary mb-3 flex items-center space-x-2">
                <FileText className="w-5 h-5 text-blue-600" />
                <span>건의하기 안내</span>
              </h3>
              <ul className="space-y-2 text-sm text-brand-muted">
                <li>
                  • 바라는 기능이나 기능 개선에 대한 제안을 해주세요.
                </li>
                <li>• 버그, 오류를 발견할 시에는 제보할 수 있습니다.</li>
                <li>• 욕설, 비방, 스팸성 내용은 삭제될 수 있습니다.</li>
              </ul>
            </CardContent>
          </Card>
        </div>
      </main>

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
}
"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { AuthHeader } from "@/components/organisms/auth-header";
import { Lightbulb, Send, Bug } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { userApi } from "@/lib/api";

type SuggestionType = "ERROR" | "IMPROVEMENT";

const suggestionTypes = [
  {
    value: "IMPROVEMENT" as const,
    label: "기능 개선 제안",
    description: "새로운 기능이나 기존 기능 개선 아이디어",
    icon: Lightbulb,
    color: "from-blue-500 to-cyan-500",
  },
  {
    value: "ERROR" as const,
    label: "오류 신고",
    description: "버그, 오작동, 기술적 문제 신고",
    icon: Bug,
    color: "from-red-500 to-orange-500",
  },
];

export default function SuggestPage() {
  const { user, isAuthenticated } = useAuth();
  const [suggestionType, setSuggestionType] = useState<SuggestionType | "">("");
  const [content, setContent] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selectedType = suggestionTypes.find(
    (type) => type.value === suggestionType
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!suggestionType || !content.trim()) {
      alert("건의 종류와 내용을 모두 입력해주세요.");
      return;
    }

    if (content.trim().length < 10) {
      alert("건의 내용은 최소 10자 이상 입력해주세요.");
      return;
    }

    setIsSubmitting(true);

    try {
      const suggestionData: {
        reportType: SuggestionType;
        content: string;
        userId?: number;
      } = {
        reportType: suggestionType,
        content: content.trim(),
        userId: isAuthenticated && user?.userId ? user.userId : undefined,
      };

      const response = await userApi.submitSuggestion(suggestionData);

      if (response.success) {
        alert("건의사항이 성공적으로 접수되었습니다. 소중한 의견 감사합니다!");
        setSuggestionType("");
        setContent("");
      } else {
        alert(
          response.error || "건의사항 접수에 실패했습니다. 다시 시도해주세요."
        );
      }
    } catch (error) {
      console.error("Submit suggestion failed:", error);
      alert("건의사항 접수 중 오류가 발생했습니다. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      {/* Header */}
      <header className="py-8">
        <div className="container mx-auto px-4 text-center">
          <div className="flex items-center justify-center space-x-3 mb-4">
            <h1 className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
              건의하기
            </h1>
          </div>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            비밀로그를 더 좋은 서비스로 만들어가는데 도움을 주세요. 여러분의
            소중한 의견을 기다립니다.
          </p>
        </div>
      </header>

      <main className="container mx-auto px-4 pb-16">
        <div className="max-w-4xl mx-auto">
          {/* 건의 종류 선택 */}
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-gray-800 mb-6 text-center">
              어떤 종류의 건의사항인가요?
            </h2>
            <div className="grid md:grid-cols-2 gap-4">
              {suggestionTypes.map((type) => {
                const Icon = type.icon;
                const isSelected = suggestionType === type.value;

                return (
                  <Card
                    key={type.value}
                    className={`cursor-pointer transition-all border-2 hover:shadow-lg ${
                      isSelected
                        ? "border-purple-500 shadow-lg bg-purple-50"
                        : "border-gray-200 hover:border-gray-300 bg-white/80"
                    } backdrop-blur-sm`}
                    onClick={() => setSuggestionType(type.value)}
                  >
                    <CardContent className="p-6 text-center">
                      <div
                        className={`w-12 h-12 bg-gradient-to-r ${type.color} rounded-full flex items-center justify-center mx-auto mb-4`}
                      >
                        <Icon className="w-6 h-6 text-white" />
                      </div>
                      <h3 className="text-lg font-semibold mb-2 text-gray-800">
                        {type.label}
                      </h3>
                      <p className="text-sm text-gray-600">
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
            <Card className="border-0 shadow-xl bg-white/90 backdrop-blur-sm">
              <CardHeader className="text-center">
                <div className="flex items-center justify-center space-x-3 mb-2">
                  {selectedType && (
                    <div
                      className={`w-10 h-10 bg-gradient-to-r ${selectedType.color} rounded-full flex items-center justify-center`}
                    >
                      <selectedType.icon className="w-5 h-5 text-white" />
                    </div>
                  )}
                  <CardTitle className="text-2xl text-gray-800">
                    {selectedType?.label}
                  </CardTitle>
                </div>
                <p className="text-gray-600">{selectedType?.description}</p>
              </CardHeader>

              <CardContent className="space-y-6">
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* 내용 */}
                  <div className="space-y-2">
                    <Label
                      htmlFor="content"
                      className="text-sm font-medium text-gray-700"
                    >
                      건의 내용 <span className="text-red-500">*</span>
                    </Label>
                    <Textarea
                      id="content"
                      placeholder={`${selectedType?.label}에 대해 자세히 설명해주세요...`}
                      value={content}
                      onChange={(e) => setContent(e.target.value)}
                      required
                      rows={8}
                      className="border-gray-300 focus:border-purple-500 focus:ring-purple-500 resize-none"
                      maxLength={500}
                    />
                    <div className="flex justify-between items-center">
                      <p className="text-xs text-gray-500">
                        구체적이고 상세한 설명일수록 더 도움이 됩니다.
                      </p>
                      <p className="text-xs text-gray-400">
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
                      <div className="flex items-center space-x-2">
                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
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
          <Card className="mt-8 border-0 shadow-lg bg-gradient-to-r from-blue-50 to-indigo-50">
            <CardContent className="p-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-3">
                📝 건의하기 안내
              </h3>
              <ul className="space-y-2 text-sm text-gray-600">
                <li>
                  • 바라는 기능이나 기능 개선에 대한 제안을 해주세요 어떠한
                  의견도 좋아요.
                </li>
                <li>• 버그, 오류를 발견할 시에는 제보할 수 있습니다.</li>
                <li>• 욕설, 비방, 스팸성 내용은 삭제될 수 있습니다.</li>
              </ul>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}

"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { boardApi } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SafeHTML } from "@/components/ui";
import { ArrowLeft, Save, Eye } from "lucide-react";
import Link from "next/link";
import { AuthHeader } from "@/components/organisms/auth-header";
import Editor from "@/components/molecules/editor";

const stripHtml = (html: string) => html.replace(/<[^>]*>?/gm, "");

export default function WritePostPage() {
  const { user, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPreview, setIsPreview] = useState(false);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <AuthHeader />
        <div className="flex items-center justify-center flex-1 min-h-[calc(100vh-80px)]">
          <div className="text-center">
            <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
              <Save className="w-7 h-7 text-white animate-pulse" />
            </div>
            <p className="text-gray-600">로딩 중...</p>
          </div>
        </div>
      </div>
    );
  }

  const handleSubmit = async () => {
    const plainContent = stripHtml(content).trim();

    if (!title.trim() || !plainContent) {
      alert("제목과 내용을 모두 입력해주세요.");
      return;
    }

    if (!isAuthenticated && !password) {
      alert("비회원은 비밀번호를 입력해야 합니다.");
      return;
    }

    if (password && !/^[0-9]+$/.test(password)) {
      alert("비밀번호는 숫자만 입력 가능합니다.");
      return;
    }

    setIsSubmitting(true);
    try {
      const postData: {
        userName: string | null;
        title: string;
        content: string;
        password?: number;
      } = {
        userName: isAuthenticated ? user!.userName : null,
        title: title.trim(),
        content: plainContent,
      };

      if (!isAuthenticated && password) {
        postData.password = Number.parseInt(password);
      }

      const response = await boardApi.createPost(postData);
      if (response.success && response.data) {
        alert("게시글이 성공적으로 작성되었습니다!");
        router.push(`/board/post/${response.data.postId}`);
      }
    } catch (error) {
      console.error("Failed to create post:", error);
      alert("게시글 작성 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatPreviewContent = (htmlContent: string) => {
    return <SafeHTML html={htmlContent} />;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      {/* 페이지 전용 서브 헤더 (모바일 최적화) */}
      <div className="bg-white/60 backdrop-blur-sm border-b sticky top-0 z-40">
        <div className="container mx-auto px-4 py-3">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            {/* 좌측: 뒤로가기 및 제목 */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Link href="/board">
                  <Button variant="ghost" size="sm" className="pl-0">
                    <ArrowLeft className="w-4 h-4 mr-1" />
                    <span className="hidden sm:inline">게시판</span>
                  </Button>
                </Link>
                <h1 className="text-lg sm:text-xl font-bold text-gray-800 whitespace-nowrap">
                  새 글 작성
                </h1>
              </div>
              {/* 모바일에서만 보이는 버튼 그룹 */}
              <div className="sm:hidden flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setIsPreview(!isPreview)}
                  className="bg-white"
                >
                  <Eye className="w-4 h-4" />
                </Button>
                <Button
                  size="sm"
                  onClick={handleSubmit}
                  disabled={isSubmitting || !title.trim() || !content.trim()}
                  className="bg-gradient-to-r from-pink-500 to-purple-600"
                >
                  <Save className="w-4 h-4" />
                </Button>
              </div>
            </div>

            {/* 우측: 버튼 그룹 (데스크톱) */}
            <div className="hidden sm:flex items-center gap-2">
              <Button
                variant="outline"
                onClick={() => setIsPreview(!isPreview)}
                className="bg-white"
              >
                <Eye className="w-4 h-4 mr-2" />
                {isPreview ? "편집" : "미리보기"}
              </Button>
              <Button
                onClick={handleSubmit}
                disabled={isSubmitting || !title.trim() || !content.trim()}
                className="bg-gradient-to-r from-pink-500 to-purple-600"
              >
                <Save className="w-4 h-4 mr-2" />
                {isSubmitting ? "작성 중..." : "작성완료"}
              </Button>
            </div>
          </div>
        </div>
      </div>

      <div className="container mx-auto px-4 py-8 max-w-4xl">
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Save className="w-5 h-5 text-purple-600" />
              <span>게시글 작성</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {!isPreview ? (
              <>
                <div className="space-y-2">
                  <Label
                    htmlFor="title"
                    className="text-sm font-medium text-gray-700"
                  >
                    제목
                  </Label>
                  <Input
                    id="title"
                    placeholder="제목을 입력하세요"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="text-lg font-medium border-2 border-gray-200 focus:border-purple-400"
                  />
                </div>

                <div className="space-y-2">
                  <Label
                    htmlFor="content"
                    className="text-sm font-medium text-gray-700"
                  >
                    내용
                  </Label>
                  <Editor value={content} onChange={setContent} />
                  <p className="text-xs text-gray-500">
                    💡 다양한 스타일로 내용을 꾸며보세요.
                  </p>
                </div>

                {!isAuthenticated && (
                  <div className="space-y-2 pt-4">
                    <Label
                      htmlFor="password"
                      className="text-sm font-medium text-gray-700"
                    >
                      비밀번호 (4자리 숫자)
                    </Label>
                    <Input
                      id="password"
                      type="password"
                      placeholder="게시글 수정/삭제 시 필요합니다."
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="border-2 border-gray-200 focus:border-purple-400"
                    />
                  </div>
                )}

                {isAuthenticated && user && (
                  <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <div className="flex items-center space-x-2">
                      <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-full flex items-center justify-center">
                        <span className="text-white text-sm font-bold">
                          {user?.userName?.charAt(0) || "?"}
                        </span>
                      </div>
                      <div>
                        <p className="text-sm font-medium text-gray-800">
                          작성자: {user?.userName}
                        </p>
                        <p className="text-xs text-gray-600">
                          게시글은 수정 및 삭제가 가능합니다
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div className="prose max-w-none">
                <h1 className="text-3xl font-bold mb-4">{title}</h1>
                <div className="text-sm text-gray-500 mb-6">
                  작성자: {isAuthenticated ? user?.userName : "익명"}
                </div>
                {formatPreviewContent(content)}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

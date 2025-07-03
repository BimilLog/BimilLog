"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SafeHTML } from "@/components/ui";
import { Save } from "lucide-react";
import Editor from "@/components/molecules/editor";

interface User {
  userName: string;
  role?: string;
}

interface WriteFormProps {
  // Form states
  title: string;
  setTitle: (value: string) => void;
  content: string;
  setContent: (value: string) => void;
  password: string;
  setPassword: (value: string) => void;

  // User info
  user: User | null;
  isAuthenticated: boolean;

  // Preview
  isPreview: boolean;
}

export const WriteForm: React.FC<WriteFormProps> = ({
  title,
  setTitle,
  content,
  setContent,
  password,
  setPassword,
  user,
  isAuthenticated,
  isPreview,
}) => {
  const formatPreviewContent = (htmlContent: string) => {
    return <SafeHTML html={htmlContent} />;
  };

  return (
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
  );
};

"use client";

import { useAuth } from "@/hooks/useAuth";
import { Save } from "lucide-react";
import { AuthHeader } from "@/components/organisms/auth-header";
import dynamic from "next/dynamic";

// 분리된 훅과 컴포넌트들 import
import { useWriteForm } from "./hooks/useWriteForm";
import { WritePageHeader, Breadcrumb } from "@/components";

// WriteForm을 동적 import로 변경하여 Editor 컴포넌트 최적화
const WriteForm = dynamic(() => import("@/components").then(mod => ({ default: mod.WriteForm })), {
  loading: () => (
    <div className="flex items-center justify-center p-8">
      <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center">
        <Save className="w-7 h-7 text-white animate-pulse" />
      </div>
    </div>
  ),
  ssr: false, // Editor는 클라이언트에서만 렌더링
});

export default function WritePostPage() {
  const { isLoading } = useAuth();

  const {
    // Form states
    title,
    setTitle,
    content,
    setContent,
    password,
    setPassword,
    isSubmitting,
    isPreview,
    setIsPreview,

    // Form actions
    handleSubmit,
    isFormValid,

    // User info
    user,
    isAuthenticated,
  } = useWriteForm();

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

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      {/* 페이지 전용 서브 헤더 (모바일 최적화) */}
      <WritePageHeader
        isPreview={isPreview}
        onTogglePreview={() => setIsPreview(!isPreview)}
        onSubmit={handleSubmit}
        isSubmitting={isSubmitting}
        isFormValid={isFormValid}
      />

      <div className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="mb-4">
          <Breadcrumb
            items={[
              { title: "홈", href: "/" },
              { title: "커뮤니티", href: "/board" },
              { title: "글쓰기" },
            ]}
          />
        </div>
        <WriteForm
          title={title}
          setTitle={setTitle}
          content={content}
          setContent={setContent}
          password={password}
          setPassword={setPassword}
          user={user}
          isAuthenticated={isAuthenticated}
          isPreview={isPreview}
        />
      </div>
    </div>
  );
}

"use client";

import { useAuth } from "@/hooks";
import { Save } from "lucide-react";
import dynamic from "next/dynamic";

// 분리된 훅과 컴포넌트들 import
import { useWriteForm } from "@/hooks/features";
import { AuthHeader, WritePageHeader, Breadcrumb, Spinner } from "@/components";

// WriteForm을 동적 import로 변경하여 Editor 컴포넌트 최적화
// Quill Editor는 무거운 라이브러리이므로 필요할 때만 로드하여 초기 번들 크기 감소
const WriteForm = dynamic(() => import("@/components").then(mod => ({ default: mod.WriteForm })), {
  // 컴포넌트 로딩 중 표시할 스피너
  loading: () => (
    <div className="flex items-center justify-center p-8">
      <Spinner size="xl" />
    </div>
  ),
  // Quill Editor는 window 객체를 사용하므로 서버사이드 렌더링 비활성화
  ssr: false,
});

export default function WritePostPage() {
  const { isLoading } = useAuth();

  // useWriteForm 훅에서 폼 상태와 액션들을 한 번에 가져옴
  // TanStack Query mutation과 로컬 상태가 결합된 통합 훅
  const {
    // 폼 입력 상태들
    title,
    setTitle,
    content,
    setContent,
    password,
    setPassword,
    isSubmitting,
    isPreview,
    setIsPreview,

    // 폼 액션들
    handleSubmit,
    isFormValid,

    // 사용자 정보 (회원/비회원 구분용)
    user,
    isAuthenticated,

    // 임시저장 기능
    isAutoSaving,
    formatLastSaved,
    saveDraftManual,
    removeDraft,
  } = useWriteForm();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-brand-gradient">
        <AuthHeader />
        <div className="flex items-center justify-center flex-1 min-h-[calc(100vh-80px)]">
          <div className="flex flex-col items-center">
            <Spinner
              size="xl"
              message="로딩 중..."
            />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-brand-gradient">
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
        <div className="mb-4 flex items-center justify-between">
          <Breadcrumb
            items={[
              { title: "홈", href: "/" },
              { title: "커뮤니티", href: "/board" },
              { title: "글쓰기" },
            ]}
          />
          {/* 임시저장 상태 표시 */}
          {formatLastSaved && (
            <div className="flex items-center gap-2 text-sm text-brand-muted">
              {isAutoSaving ? (
                <span className="flex items-center gap-1">
                  <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse" />
                  자동 저장 중...
                </span>
              ) : (
                <span className="flex items-center gap-1">
                  <Save className="w-3.5 h-3.5" />
                  {formatLastSaved}
                </span>
              )}
            </div>
          )}
        </div>
        {/* WriteForm에 필요한 모든 상태와 핸들러를 props로 전달 */}
        {/* 회원/비회원에 따라 다른 폼 필드가 렌더링됨 */}
        <WriteForm
          title={title}
          setTitle={setTitle}
          content={content}
          setContent={setContent}
          password={password}
          setPassword={setPassword}
          user={user} // 사용자 정보 (회원일 때만 존재)
          isAuthenticated={isAuthenticated} // 로그인 여부로 폼 UI 조건부 렌더링
          isPreview={isPreview}
        />

        {/* 임시저장 관리 버튼 */}
        {(title || content) && (
          <div className="mt-4 flex gap-2">
            <button
              onClick={() => saveDraftManual(title, content)}
              className="px-4 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              <Save className="w-4 h-4 inline mr-1" />
              수동 저장
            </button>
            {formatLastSaved && (
              <button
                onClick={removeDraft}
                className="px-4 py-2 text-sm text-red-600 hover:bg-red-50 rounded-lg transition-colors"
              >
                임시저장 삭제
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

"use client";

import { useAuth } from "@/hooks";
import { Save, RotateCcw, X } from "lucide-react";

// 분리된 훅과 컴포넌트들 import
import { useWriteForm } from "@/hooks/features";
import { AuthHeader, WritePageHeader, Breadcrumb, Spinner, Button } from "@/components";
import { AnonymousWriteNotice } from "@/components/organisms/board/AnonymousWriteNotice";
// 코드 패턴 권고: dynamic 이중 호출 정리 — LazyWriteForm 단일 wrapper 재사용
import { LazyWriteForm } from "@/lib/utils/lazy-components";

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

    // B-7-003: 사용자 동의 기반 임시저장 복구 배너
    showDraftBanner,
    handleRestoreDraft,
    handleDismissDraft,

    // Content length
    plainTextLength,
  } = useWriteForm();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-paper">
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
    <div className="min-h-screen bg-paper">
      <AuthHeader />

      {/* 비로그인 안내 (B-M5) */}
      {!isAuthenticated && <AnonymousWriteNotice />}

      {/* 페이지 전용 서브 헤더 (모바일 최적화) */}
      <WritePageHeader
        isPreview={isPreview}
        onTogglePreview={() => setIsPreview(!isPreview)}
        onSubmit={handleSubmit}
        isSubmitting={isSubmitting}
        isFormValid={isFormValid}
      />

      <main className="container mx-auto px-4 py-8 max-w-4xl">
        {/* B-7-003: 임시저장 복구 배너 — 자동 적용 대신 사용자 선택 (NN/g Heuristic #6 Recognition rather than recall) */}
        {showDraftBanner && (
          <div
            role="region"
            aria-label="임시저장 복구"
            className="mb-4 flex flex-col gap-3 rounded-lg border border-postal-navy/30 bg-paper-aged p-4 sm:flex-row sm:items-center sm:justify-between dark:border-postal-navy/40 dark:bg-postal-navy/20"
          >
            <div className="flex items-start gap-3">
              <Save className="mt-0.5 h-5 w-5 flex-shrink-0 stroke-postal-navy" />
              <div>
                <p className="text-sm font-medium text-brand-primary">
                  이전에 작성하던 편지가 있어요
                </p>
                <p className="mt-1 text-xs text-brand-muted break-keep">
                  편지지에 임시 보관된 내용을 이어서 작성하시겠어요?
                </p>
              </div>
            </div>
            <div className="flex shrink-0 gap-2">
              <Button
                size="sm"
                onClick={handleRestoreDraft}
                className="bg-stamp-red text-paper-50 hover:bg-stamp-red/90"
              >
                <RotateCcw className="mr-1 h-4 w-4 stroke-paper-50" />
                이어서 작성
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleDismissDraft}
                className="bg-paper-50"
              >
                <X className="mr-1 h-4 w-4 stroke-postal-navy" />
                새로 작성
              </Button>
            </div>
          </div>
        )}

        <div className="mb-4 flex items-center justify-between">
          <Breadcrumb
            items={[
              { title: "홈", href: "/" },
              { title: "커뮤니티", href: "/board" },
              { title: "글쓰기" },
            ]}
          />
          {/* 임시저장 상태 표시 — 우표/봉투 메타포 + aria-live + HH:MM 축약은 useDraft 측에서 적용 */}
          {formatLastSaved && (
            <div className="flex items-center gap-2 text-sm text-brand-muted" aria-live="polite">
              {isAutoSaving ? (
                <span className="flex items-center gap-1">
                  <span aria-hidden="true" className="w-2 h-2 bg-postal-navy/60 rounded-full animate-pulse" />
                  편지지에 임시 보관 중...
                </span>
              ) : (
                <span className="flex items-center gap-1">
                  <Save className="w-3.5 h-3.5 stroke-postal-navy fill-paper-100" />
                  {formatLastSaved}
                </span>
              )}
            </div>
          )}
        </div>
        {/* LazyWriteForm 사용 (dynamic 이중 호출 정리) */}
        {/* 회원/비회원에 따라 다른 폼 필드가 렌더링됨 */}
        <LazyWriteForm
          title={title}
          setTitle={setTitle}
          content={content}
          setContent={setContent}
          password={password}
          setPassword={setPassword}
          user={user} // 사용자 정보 (회원일 때만 존재)
          isAuthenticated={isAuthenticated} // 로그인 여부로 폼 UI 조건부 렌더링
          isPreview={isPreview}
          plainTextLength={plainTextLength} // 순수 텍스트 길이 (HTML 태그 제외)
        />
      </main>
    </div>
  );
}

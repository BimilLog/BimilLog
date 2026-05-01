"use client";

import { useEditForm } from "@/hooks/features";
import { Button, Card, CardContent, CardHeader, CardTitle, Input, Label, Editor, SafeHTML, Spinner } from "@/components";
import { ArrowLeft, Save, Eye } from "lucide-react";
import Link from "next/link";
import { AuthHeader } from "@/components/organisms/common";
import type { Post } from "@/types/domains/post";
import { formatRelativeDate } from "@/lib/utils";

interface EditPostClientProps {
  initialPost: Post | null;
  postId: number;
}

export default function EditPostClient({ initialPost, postId }: EditPostClientProps) {

  // useEditForm 훅에서 모든 상태와 액션을 가져옴
  const {
    post,
    postId: resolvedPostId,
    isLoading,
    isAuthorized,
    title,
    setTitle,
    content,
    setContent,
    guestPassword,
    setGuestPassword,
    isGuest,
    isPreview,
    setIsPreview,
    isSubmitting,
    isFormValid,
    handleSubmit,
    isAutoSaving,
    formatLastSaved,
  } = useEditForm({ initialPost, initialPostId: postId });

  if (isLoading) {
    return (
      <div className="min-h-screen bg-paper dark:bg-background">
        <AuthHeader />
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)]">
          <Spinner size="xl" message="게시글 불러오는 중..." />
        </div>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="min-h-screen bg-paper dark:bg-background">
        <AuthHeader />
        <div className="container mx-auto flex min-h-[calc(100vh-80px)] flex-col items-center justify-center gap-4 px-4 text-center">
          <p className="text-ink-soft break-keep">게시글 정보를 찾을 수 없습니다.</p>
          <Link href="/board">
            <Button variant="outline" className="bg-paper-50">
              <ArrowLeft className="w-4 h-4 mr-1 stroke-postal-navy" />
              게시판으로 돌아가기
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  // B-7-007: 권한 없음 화면 — 무한 placeholder 대신 명시적 안내 + 복귀 버튼
  if (!isAuthorized) {
    return (
      <div className="min-h-screen bg-paper dark:bg-background">
        <AuthHeader />
        <div className="container mx-auto flex min-h-[calc(100vh-80px)] flex-col items-center justify-center gap-4 px-4 text-center">
          <p className="text-ink dark:text-ink-900 font-medium break-keep">
            이 게시글을 수정할 권한이 없습니다.
          </p>
          <p className="text-sm text-ink-soft break-keep">
            본인이 작성한 게시글만 수정할 수 있어요.
          </p>
          <Link href={`/board/post/${resolvedPostId ?? postId}`}>
            <Button variant="outline" className="bg-paper-50">
              <ArrowLeft className="w-4 h-4 mr-1 stroke-postal-navy" />
              게시글로 돌아가기
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="min-h-screen bg-paper dark:bg-background">
        <AuthHeader />

        {/* Header (모바일 최적화)
            B-7-005: top 좌표를 --app-header-height 토큰으로 통일 (write 페이지와 일치) */}
        <header
          data-toast-anchor
          className="sticky z-sticky-header bg-paper-50/85 shadow-sm backdrop-blur-md border-b border-ink-soft dark:border-postal-navy/40 dark:bg-postal-navy/30 dark:shadow-md dark:shadow-black/20"
          style={{ top: "var(--app-header-height)" }}
        >
          <div className="container mx-auto px-4 py-3">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              {/* 좌측: 뒤로가기 및 제목 */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Link href={`/board/post/${resolvedPostId}`}>
                    <Button variant="ghost" size="sm" className="pl-0">
                      <ArrowLeft className="w-4 h-4 mr-1 stroke-ink-soft" aria-hidden="true" />
                      <span className="hidden sm:inline">게시글</span>
                    </Button>
                  </Link>
                  <h1 className="text-lg sm:text-xl font-bold text-ink dark:text-ink-900 whitespace-nowrap">
                    게시글 수정
                  </h1>
                </div>
                {/* 모바일에서만 보이는 버튼 그룹 */}
                <div className="sm:hidden flex items-center gap-2">
                  {/* 미리보기 모드 토글 버튼 */}
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setIsPreview(!isPreview)}
                    className="bg-paper-50"
                  >
                    <Eye className="w-4 h-4 stroke-postal-navy fill-paper-100" />
                  </Button>
                  <Button
                    size="sm"
                    onClick={handleSubmit}
                    disabled={isSubmitting || !isFormValid}
                    className="bg-stamp-red text-paper-50 hover:bg-stamp-red/90"
                  >
                    <Save className="w-4 h-4 stroke-paper-50 fill-stamp-red/40" />
                  </Button>
                </div>
              </div>

              {/* 우측: 버튼 그룹 (데스크톱) */}
              <div className="hidden sm:flex items-center gap-2">
                {/* 편집/미리보기 모드 전환 버튼 */}
                <Button
                  variant="outline"
                  onClick={() => setIsPreview(!isPreview)}
                  className="bg-paper-50"
                >
                  <Eye className="w-4 h-4 mr-2 stroke-postal-navy fill-paper-100" />
                  {isPreview ? "편집" : "미리보기"}
                </Button>
                <Button
                  onClick={handleSubmit}
                  disabled={isSubmitting || !isFormValid}
                  className="bg-stamp-red text-paper-50 hover:bg-stamp-red/90"
                >
                  <Save className="w-4 h-4 mr-2 stroke-paper-50 fill-stamp-red/40" />
                  {isSubmitting ? "수정 중..." : "수정완료"}
                </Button>
              </div>
            </div>
          </div>
        </header>

        <main className="container mx-auto px-4 py-8 max-w-4xl">
          {/* 자동저장 상태 표시 (B-7-004 수정 모드 임시저장 인디케이터) */}
          {(isAutoSaving || formatLastSaved) && (
            <div className="mb-3 flex justify-end" aria-live="polite">
              {isAutoSaving ? (
                <span className="flex items-center gap-1 text-sm text-ink-soft">
                  <span
                    aria-hidden="true"
                    className="w-2 h-2 bg-postal-navy/60 rounded-full animate-pulse"
                  />
                  편지지에 임시 보관 중...
                </span>
              ) : formatLastSaved ? (
                <span className="flex items-center gap-1 text-sm text-ink-soft">
                  <Save className="w-3.5 h-3.5 stroke-postal-navy fill-paper-100" />
                  {formatLastSaved}
                </span>
              ) : null}
            </div>
          )}
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Save className="w-5 h-5 stroke-stamp-red fill-paper-100" />
                <span>게시글 수정</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* 편집/미리보기 모드에 따른 UI 전환 */}
              {!isPreview ? (
                <>
                  {/* 제목 입력 */}
                  <div className="space-y-2">
                    <Label
                      htmlFor="title"
                      className="text-sm font-medium text-ink dark:text-ink-900"
                    >
                      제목
                    </Label>
                    <Input
                      id="title"
                      placeholder="제목을 입력하세요"
                      value={title}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setTitle(e.target.value)}
                      className="text-lg font-medium border-2 border-ink-soft focus:border-stamp-red"
                    />
                  </div>

                  {/* 내용 입력 */}
                  <div className="space-y-2">
                    <Label
                      htmlFor="content"
                      className="text-sm font-medium text-ink dark:text-ink-900"
                    >
                      내용
                    </Label>
                    <Editor value={content} onChange={setContent} />
                  </div>

                  {/* 비회원 게시글인 경우 비밀번호 입력
                      B-7-011: write 폼과 동일 정책 (4자리 숫자, 1000~9999) 라벨 명시 */}
                  {isGuest && (
                    <div className="space-y-2">
                      <Label
                        htmlFor="edit-password"
                        className="text-sm font-medium text-ink dark:text-ink-900"
                      >
                        비밀번호 (1000~9999 4자리 숫자)
                      </Label>
                      <Input
                        id="edit-password"
                        type="text"
                        inputMode="numeric"
                        maxLength={4}
                        placeholder="게시글 작성 시 설정한 4자리 숫자"
                        value={guestPassword}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                          // 숫자만 허용 (write 폼과 동일 정책)
                          const value = e.target.value.replace(/\D/g, "");
                          setGuestPassword(value);
                        }}
                        className="border-2 border-ink-soft focus:border-stamp-red"
                      />
                      <p className="text-xs text-ink-soft break-keep">
                        게시글 작성 시 설정한 비밀번호를 입력해주세요.
                      </p>
                    </div>
                  )}

                  {/* 작성자 정보 */}
                  <div className="p-4 bg-paper-aged border border-postal-navy/30 rounded-lg">
                    <div className="flex items-center space-x-2">
                      <div className="w-8 h-8 bg-stamp-red rounded-full flex items-center justify-center">
                        <span className="text-paper-50 text-sm font-bold">
                          {/* grapheme 단위 첫 글자 추출 (이모지/한글 안전) */}
                          {post.memberName ? [...post.memberName][0] : "?"}
                        </span>
                      </div>
                      <div>
                        <p className="text-sm font-medium text-ink dark:text-ink-900">
                          작성자: {post.memberName}
                        </p>
                        <p className="text-xs text-ink-soft">
                          원본 작성일: {formatRelativeDate(post.createdAt)}
                        </p>
                        {post.updatedAt && post.updatedAt !== post.createdAt && (
                          <p className="text-xs text-ink-soft">
                            마지막 수정: {formatRelativeDate(post.updatedAt)}
                          </p>
                        )}
                      </div>
                    </div>
                  </div>
                </>
              ) : (
                /* 미리보기 — B-7-006: SafeHTML 로 HTML 서식 그대로 렌더 */
                <div className="space-y-6">
                  <div className="border-b border-ink-soft pb-4">
                    <h2 className="text-2xl font-bold text-ink dark:text-ink-900 mb-2 break-keep">
                      {title || "제목을 입력하세요"}
                    </h2>
                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-ink-soft">
                      <span>작성자: {post.memberName}</span>
                      <span>작성일: {formatRelativeDate(post.createdAt)}</span>
                      {post.updatedAt && post.updatedAt !== post.createdAt && (
                        <span className="text-stamp-red dark:text-stamp-red/90">
                          이전에 수정됨
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="prose dark:prose-invert max-w-none">
                    {content ? (
                      <SafeHTML
                        html={content}
                        className="text-ink dark:text-ink-900 leading-relaxed"
                      />
                    ) : (
                      <p className="text-ink-soft">내용을 입력하세요</p>
                    )}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </main>
      </div>
    </>
  );
}

"use client";

import { useEditForm } from "@/hooks/features";
import { Button, Card, CardContent, CardHeader, CardTitle, Input, Label, Editor } from "@/components";
import { ArrowLeft, Save, Eye } from "lucide-react";
import Link from "next/link";
import { ToastContainer } from "@/components/molecules/feedback/toast";
import { AuthHeader } from "@/components/organisms/common";
import { useToast } from "@/hooks";

export default function EditPostPage() {
  const { toasts, removeToast } = useToast();

  // useEditForm 훅에서 모든 상태와 액션을 가져옴
  const {
    post,
    postId,
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
  } = useEditForm();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <AuthHeader />
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)]">
          <div className="text-center">
            <p className="text-brand-muted">로딩 중...</p>
          </div>
        </div>
      </div>
    );
  }
  
  if (!post) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <AuthHeader />
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)]">
          <p className="text-brand-muted">게시글 정보를 찾을 수 없습니다.</p>
        </div>
      </div>
    );
  }

  if (!isAuthorized) {
    return (
      <>
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
          <div className="text-center">
            <p className="text-brand-muted">권한을 확인하는 중...</p>
          </div>
        </div>
        <ToastContainer toasts={toasts} onRemove={removeToast} />
      </>
    );
  }

  return (
    <>
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <AuthHeader />
        
        {/* Header (모바일 최적화) */}
        <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
          <div className="container mx-auto px-4 py-3">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              {/* 좌측: 뒤로가기 및 제목 */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Link href={`/board/post/${postId}`}>
                    <Button variant="ghost" size="sm" className="pl-0">
                      <ArrowLeft className="w-4 h-4 mr-1 stroke-slate-600" />
                      <span className="hidden sm:inline">게시글</span>
                    </Button>
                  </Link>
                  <h1 className="text-lg sm:text-xl font-bold text-brand-primary whitespace-nowrap">
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
                    className="bg-white"
                  >
                    <Eye className="w-4 h-4 stroke-purple-600 fill-purple-100" />
                  </Button>
                  <Button
                    size="sm"
                    onClick={handleSubmit}
                    disabled={isSubmitting || !isFormValid}
                    className="bg-gradient-to-r from-pink-500 to-purple-600"
                  >
                    <Save className="w-4 h-4 stroke-green-600 fill-green-100" />
                  </Button>
                </div>
              </div>

              {/* 우측: 버튼 그룹 (데스크톱) */}
              <div className="hidden sm:flex items-center gap-2">
                {/* 편집/미리보기 모드 전환 버튼 */}
                <Button
                  variant="outline"
                  onClick={() => setIsPreview(!isPreview)}
                  className="bg-white"
                >
                  <Eye className="w-4 h-4 mr-2 stroke-purple-600 fill-purple-100" />
                  {isPreview ? "편집" : "미리보기"}
                </Button>
                <Button
                  onClick={handleSubmit}
                  disabled={isSubmitting || !isFormValid}
                  className="bg-gradient-to-r from-pink-500 to-purple-600"
                >
                  <Save className="w-4 h-4 mr-2 stroke-green-600 fill-green-100" />
                  {isSubmitting ? "수정 중..." : "수정완료"}
                </Button>
              </div>
            </div>
          </div>
        </header>

        <div className="container mx-auto px-4 py-8 max-w-4xl">
          <Card variant="elevated">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Save className="w-5 h-5 stroke-purple-600 fill-purple-100" />
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
                      className="text-sm font-medium text-brand-primary"
                    >
                      제목
                    </Label>
                    <Input
                      id="title"
                      placeholder="제목을 입력하세요"
                      value={title}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setTitle(e.target.value)}
                      className="text-lg font-medium border-2 border-gray-200 focus:border-purple-400"
                    />
                  </div>

                  {/* 내용 입력 */}
                  <div className="space-y-2">
                    <Label
                      htmlFor="content"
                      className="text-sm font-medium text-brand-primary"
                    >
                      내용
                    </Label>
                    <Editor value={content} onChange={setContent} />
                  </div>

                  {/* 비회원 게시글인 경우 비밀번호 입력 */}
                  {isGuest && (
                    <div className="space-y-2">
                      <Label
                        htmlFor="edit-password"
                        className="text-sm font-medium text-brand-primary"
                      >
                        비밀번호 (4자리 숫자)
                      </Label>
                      <Input
                        id="edit-password"
                        type="password"
                        placeholder="게시글 수정을 위한 비밀번호를 입력하세요"
                        value={guestPassword}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) => setGuestPassword(e.target.value)}
                        className="border-2 border-gray-200 focus:border-purple-400"
                      />
                      <p className="text-xs text-brand-secondary">
                        게시글 작성 시 설정한 비밀번호를 입력해주세요.
                      </p>
                    </div>
                  )}

                  {/* 작성자 정보 */}
                  <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <div className="flex items-center space-x-2">
                      <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-full flex items-center justify-center">
                        <span className="text-white text-sm font-bold">
                          {post.userName?.charAt(0) || "?"}
                        </span>
                      </div>
                      <div>
                        <p className="text-sm font-medium text-brand-primary">
                          작성자: {post.userName}
                        </p>
                        <p className="text-xs text-brand-muted">
                          원본 작성일: {post.createdAt}
                        </p>
                      </div>
                    </div>
                  </div>
                </>
              ) : (
                /* 미리보기 */
                <div className="space-y-6">
                  <div className="border-b pb-4">
                    <h1 className="text-2xl font-bold text-brand-primary mb-2">
                      {title || "제목을 입력하세요"}
                    </h1>
                    <div className="flex items-center space-x-4 text-sm text-brand-muted">
                      <span>작성자: {post.userName}</span>
                      <span>작성일: {post.createdAt}</span>
                      <span className="text-orange-600">수정됨</span>
                    </div>
                  </div>

                  <div className="prose max-w-none">
                    <div className="text-brand-primary leading-relaxed whitespace-pre-wrap">
                      {content || "내용을 입력하세요"}
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
}

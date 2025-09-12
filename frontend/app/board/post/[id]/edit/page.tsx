"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { boardApi, type Post } from "@/lib/api";
import { Button, Card, CardContent, CardHeader, CardTitle, Input, Label } from "@/components";
import { ArrowLeft, Save, Eye } from "lucide-react";
import Link from "next/link";
import { Editor } from "@/components";
import { useToast } from "@/hooks/useToast";
import { ToastContainer } from "@/components/molecules/feedback/toast";
import { stripHtml, validatePassword } from "@/lib/utils";
import { AuthHeader } from "@/components/organisms/auth-header";

export default function EditPostPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const params = useParams();
  const [postId, setPostId] = useState<number | null>(null);
  const { showSuccess, showError, showWarning, toasts, removeToast } =
    useToast();

  // params에서 id 추출
  useEffect(() => {
    if (params.id) {
      setPostId(Number.parseInt(params.id as string));
    }
  }, [params]);

  const [post, setPost] = useState<Post | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPreview, setIsPreview] = useState(false);

  // 비회원 게시글 수정을 위한 상태
  const [isGuest, setIsGuest] = useState(false);
  const [isAuthorized, setIsAuthorized] = useState(false);
  const [guestPassword, setGuestPassword] = useState("");

  // 게시글 정보 조회
  useEffect(() => {
    const fetchPost = async () => {
      if (!postId) return;

      try {
        const response = await boardApi.getPost(postId);
        if (response.success && response.data) {
          const postData = response.data;
          setPost(postData);
          setTitle(postData.title);
          setContent(postData.content);
          setPassword(postData.password?.toString() || "");

          const isGuestPost = postData.userId === null || postData.userId === 0;
          setIsGuest(isGuestPost);

          // 회원 글일 경우 작성자만 권한 부여
          if (!isGuestPost) {
            if (isAuthenticated && user?.userId === postData.userId) {
              setIsAuthorized(true);
            } else {
              // 회원 글인데 다른 사람이 접근
              showError("권한 없음", "수정 권한이 없습니다.");
              router.push(`/board/post/${postId}`);
            }
          } else {
            // 비회원 글의 경우 바로 수정 화면으로 (비밀번호는 수정 시 확인)
            setIsAuthorized(true);
          }
        } else {
          showError("게시글 없음", "게시글을 찾을 수 없습니다.");
          router.push("/board");
        }
      } catch (error) {
        showError("오류", "게시글을 불러오는 중 오류가 발생했습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchPost();
  }, [postId, router, isAuthenticated, user, showError]);

  const handleSubmit = async () => {
    const plainContent = stripHtml(content).trim();

    if (!title.trim() || !plainContent) {
      showWarning("입력 확인", "제목과 내용을 입력해주세요.");
      return;
    }
    if (!post) return;

    // 비밀번호 validation
    let validatedPassword: number | undefined;
    try {
      validatedPassword = isGuest ? validatePassword(guestPassword, false) : undefined;
    } catch (error) {
      if (error instanceof Error) {
        showWarning("비밀번호 확인", error.message);
      }
      return;
    }

    setIsSubmitting(true);
    try {
      const updatedPost: Post = {
        ...post,
        title: title.trim(),
        content: plainContent,
        password: validatedPassword,
      };

      const response = await boardApi.updatePost(updatedPost);
      if (response.success) {
        showSuccess("수정 완료", "게시글이 성공적으로 수정되었습니다!");
        router.push(`/board/post/${postId}`);
      } else {
        // 비밀번호 불일치 에러 처리
        if (
          response.error &&
          response.error.includes("게시글 비밀번호가 일치하지 않습니다")
        ) {
          showError("비밀번호 오류", "비밀번호가 일치하지 않습니다.");
        } else {
          showError(
            "수정 실패",
            response.error || "게시글 수정에 실패했습니다."
          );
        }
      }
    } catch (error) {
      // HTTP 에러 상태 처리
      if (error instanceof Error && error.message.includes("403")) {
        showError("비밀번호 오류", "비밀번호가 일치하지 않습니다.");
      } else {
        showError("수정 실패", "게시글 수정 중 오류가 발생했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading || authLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <AuthHeader />
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)]">
          <div className="text-center">
            <p className="text-gray-600">로딩 중...</p>
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
          <p className="text-gray-600">게시글 정보를 찾을 수 없습니다.</p>
        </div>
      </div>
    );
  }

  if (!isAuthorized) {
    return (
      <>
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
          <div className="text-center">
            <p className="text-gray-600">권한을 확인하는 중...</p>
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
                      <ArrowLeft className="w-4 h-4 mr-1" />
                      <span className="hidden sm:inline">게시글</span>
                    </Button>
                  </Link>
                  <h1 className="text-lg sm:text-xl font-bold text-gray-800 whitespace-nowrap">
                    게시글 수정
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
                  {isSubmitting ? "수정 중..." : "수정완료"}
                </Button>
              </div>
            </div>
          </div>
        </header>

        <div className="container mx-auto px-4 py-8 max-w-4xl">
          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl">
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Save className="w-5 h-5 text-purple-600" />
                <span>게시글 수정</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              {!isPreview ? (
                <>
                  {/* 제목 입력 */}
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
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => setTitle(e.target.value)}
                      className="text-lg font-medium border-2 border-gray-200 focus:border-purple-400"
                    />
                  </div>

                  {/* 내용 입력 */}
                  <div className="space-y-2">
                    <Label
                      htmlFor="content"
                      className="text-sm font-medium text-gray-700"
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
                        className="text-sm font-medium text-gray-700"
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
                      <p className="text-xs text-gray-500">
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
                        <p className="text-sm font-medium text-gray-800">
                          작성자: {post.userName}
                        </p>
                        <p className="text-xs text-gray-600">
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
                    <h1 className="text-2xl font-bold text-gray-800 mb-2">
                      {title || "제목을 입력하세요"}
                    </h1>
                    <div className="flex items-center space-x-4 text-sm text-gray-600">
                      <span>작성자: {post.userName}</span>
                      <span>작성일: {post.createdAt}</span>
                      <span className="text-orange-600">수정됨</span>
                    </div>
                  </div>

                  <div className="prose max-w-none">
                    <div className="text-gray-800 leading-relaxed whitespace-pre-wrap">
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

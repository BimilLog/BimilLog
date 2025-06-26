"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { boardApi, type Post } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { ArrowLeft, Save, Eye, Loader2, LockKeyhole } from "lucide-react";
import Link from "next/link";
import Editor from "@/components/molecules/editor";

const stripHtml = (html: string) => html.replace(/<[^>]*>?/gm, "");

export default function EditPostPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const params = useParams();
  const [postId, setPostId] = useState<number | null>(null);

  // params에서 id 추출
  useEffect(() => {
    if (params.id) {
      setPostId(Number.parseInt(params.id as string));
    }
  }, [params]);

  const [post, setPost] = useState<Post | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [isPrivate, setIsPrivate] = useState(false);
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
          setIsPrivate(!!postData.password);
          setPassword(postData.password?.toString() || "");

          const isGuestPost = postData.userId === null || postData.userId === 0;
          setIsGuest(isGuestPost);

          // 회원 글일 경우 바로 권한 부여
          if (
            !isGuestPost &&
            isAuthenticated &&
            user?.userId === postData.userId
          ) {
            setIsAuthorized(true);
          } else if (!isGuestPost) {
            // 회원 글인데 다른 사람이 접근
            alert("수정 권한이 없습니다.");
            router.push(`/board/post/${postId}`);
          }
        } else {
          alert("게시글을 찾을 수 없습니다.");
          router.push("/board");
        }
      } catch (error) {
        console.error("Failed to fetch post:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchPost();
  }, [postId, router, isAuthenticated, user]);

  const handleGuestAuth = () => {
    // 실제로는 백엔드에서 비밀번호를 확인해야 함
    // 여기서는 클라이언트에서 임시로 확인
    if (post && post.password && Number(guestPassword) === post.password) {
      setIsAuthorized(true);
    } else {
      alert("비밀번호가 일치하지 않습니다.");
    }
  };

  const handleSubmit = async () => {
    const plainContent = stripHtml(content).trim();

    if (!title.trim() || !plainContent)
      return alert("제목과 내용을 입력해주세요.");
    if (!post) return;

    setIsSubmitting(true);
    try {
      const updatedPost: Post = {
        ...post,
        title: title.trim(),
        content: plainContent,
        password: password ? Number(password) : undefined,
      };

      const response = await boardApi.updatePost(updatedPost);
      if (response.success) {
        alert("게시글이 성공적으로 수정되었습니다!");
        router.push(`/board/post/${postId}`);
      } else {
        // 비밀번호 불일치 에러 처리
        if (
          response.error &&
          response.error.includes("게시글 비밀번호가 일치하지 않습니다")
        ) {
          alert("비밀번호가 일치하지 않습니다.");
        } else {
          alert(response.error || "게시글 수정에 실패했습니다.");
        }
      }
    } catch (error) {
      console.error("Failed to update post:", error);
      // HTTP 에러 상태 처리
      if (error instanceof Error && error.message.includes("403")) {
        alert("비밀번호가 일치하지 않습니다.");
      } else {
        alert("게시글 수정 중 오류가 발생했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading || authLoading) return <div>로딩 중...</div>;
  if (!post) return <div>게시글 정보를 찾을 수 없습니다.</div>;

  if (!isAuthorized) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>비밀번호 확인</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p>게시글을 수정하려면 비밀번호를 입력하세요.</p>
            <div className="space-y-2">
              <Label htmlFor="guest-password">비밀번호</Label>
              <Input
                id="guest-password"
                type="password"
                value={guestPassword}
                onChange={(e) => setGuestPassword(e.target.value)}
              />
            </div>
            <Button onClick={handleGuestAuth} className="w-full">
              확인
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
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
                    onChange={(e) => setTitle(e.target.value)}
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

                {/* 비밀글 설정 */}
                <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
                  <div className="flex items-center justify-between">
                    <div>
                      <Label
                        htmlFor="private"
                        className="text-sm font-medium text-gray-700"
                      >
                        비밀글로 작성
                      </Label>
                      <p className="text-xs text-gray-500">
                        비밀번호를 설정하여 특정 사용자만 볼 수 있게 합니다
                      </p>
                    </div>
                    <Switch
                      id="private"
                      checked={isPrivate}
                      onCheckedChange={setIsPrivate}
                    />
                  </div>

                  {isPrivate && (
                    <div className="space-y-2">
                      <Label
                        htmlFor="password"
                        className="text-sm font-medium text-gray-700"
                      >
                        비밀번호 (4자리 숫자)
                      </Label>
                      <Input
                        id="password"
                        type="number"
                        placeholder="4자리 숫자 비밀번호"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        maxLength={4}
                        className="border-2 border-gray-200 focus:border-purple-400"
                      />
                    </div>
                  )}
                </div>

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
                    {isPrivate && (
                      <span className="text-red-600">🔒 비밀글</span>
                    )}
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
  );
}

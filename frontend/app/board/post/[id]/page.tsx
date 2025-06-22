"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { boardApi, commentApi, type Post, type Comment } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  ArrowLeft,
  ThumbsUp,
  MessageSquare,
  Edit,
  Trash2,
  Eye,
  Lock,
  Send,
  Loader2,
} from "lucide-react";
import Link from "next/link";
import dynamic from "next/dynamic";

const Editor = dynamic(() => import("@/components/editor"), { ssr: false });

export default function PostDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const postId = Number.parseInt(params.id as string);

  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isPasswordRequired, setIsPasswordRequired] = useState(false);
  const [passwordInput, setPasswordInput] = useState("");
  const [newComment, setNewComment] = useState("");
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);

  // 게시글 조회
  useEffect(() => {
    const fetchPost = async () => {
      try {
        const response = await boardApi.getPost(postId);
        if (response.success && response.data) {
          setPost(response.data);
          // 비밀글이고 비밀번호가 필요한 경우
          if (response.data.password && !response.data.content) {
            setIsPasswordRequired(true);
          }
        } else {
          alert("게시글을 찾을 수 없습니다.");
          router.push("/board");
        }
      } catch (error) {
        console.error("Failed to fetch post:", error);
        alert("게시글을 불러오는데 실패했습니다.");
        router.push("/board");
      } finally {
        setIsLoading(false);
      }
    };

    if (postId) {
      fetchPost();
    }
  }, [postId, router]);

  // 댓글 조회
  useEffect(() => {
    const fetchComments = async () => {
      if (!post || isPasswordRequired) return;

      try {
        const response = await commentApi.getComments(postId);
        if (response.success && response.data) {
          setComments(response.data.content);
        }
      } catch (error) {
        console.error("Failed to fetch comments:", error);
      }
    };

    fetchComments();
  }, [postId, post, isPasswordRequired]);

  const handlePasswordSubmit = async () => {
    if (!passwordInput) {
      alert("비밀번호를 입력해주세요.");
      return;
    }

    try {
      // 비밀번호 확인 로직 (실제 API에서는 서버에서 처리)
      if (post && Number.parseInt(passwordInput) === post.password) {
        setIsPasswordRequired(false);
        setPasswordInput("");
      } else {
        alert("비밀번호가 틀렸습니다.");
      }
    } catch (error) {
      alert("비밀번호 확인 중 오류가 발생했습니다.");
    }
  };

  const handleLike = async () => {
    if (!isAuthenticated || !post) {
      alert("로그인이 필요합니다.");
      return;
    }

    try {
      const response = await boardApi.likePost(post);
      if (response.success) {
        setPost((prev) =>
          prev
            ? {
                ...prev,
                likes: prev.userLike ? prev.likes - 1 : prev.likes + 1,
                userLike: !prev.userLike,
              }
            : null
        );
      }
    } catch (error) {
      console.error("Failed to like post:", error);
    }
  };

  const handleCommentSubmit = async () => {
    if (!isAuthenticated || !user) {
      alert("로그인이 필요합니다.");
      return;
    }

    if (!newComment.trim()) {
      alert("댓글 내용을 입력해주세요.");
      return;
    }

    setIsSubmittingComment(true);
    try {
      const response = await commentApi.createComment({
        postId,
        userName: user.userName,
        content: newComment.trim(),
      });

      if (response.success) {
        setNewComment("");
        // 댓글 목록 새로고침
        const commentsResponse = await commentApi.getComments(postId);
        if (commentsResponse.success && commentsResponse.data) {
          setComments(commentsResponse.data.content);
        }
      } else {
        alert("댓글 작성에 실패했습니다.");
      }
    } catch (error) {
      console.error("Failed to create comment:", error);
      alert("댓글 작성 중 오류가 발생했습니다.");
    } finally {
      setIsSubmittingComment(false);
    }
  };

  const handleDeletePost = async () => {
    if (!post || !user || post.userId !== user.userId) return;

    if (confirm("정말로 이 게시글을 삭제하시겠습니까?")) {
      try {
        const response = await boardApi.deletePost(post);
        if (response.success) {
          alert("게시글이 삭제되었습니다.");
          router.push("/board");
        } else {
          alert("게시글 삭제에 실패했습니다.");
        }
      } catch (error) {
        console.error("Failed to delete post:", error);
        alert("게시글 삭제 중 오류가 발생했습니다.");
      }
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-12 h-12 text-purple-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">게시글을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (!post) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <Link href="/board">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="w-4 h-4 mr-2" />
              게시판으로
            </Button>
          </Link>
          {isAuthenticated && user && post.userId === user.userId && (
            <div className="flex items-center space-x-2">
              <Button variant="outline" size="sm" asChild className="bg-white">
                <Link href={`/board/post/${postId}/edit`}>
                  <Edit className="w-4 h-4 mr-2" />
                  수정
                </Link>
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleDeletePost}
                className="bg-white text-red-600 border-red-200 hover:bg-red-50"
              >
                <Trash2 className="w-4 h-4 mr-2" />
                삭제
              </Button>
            </div>
          )}
        </div>
      </header>

      <div className="container mx-auto px-4 py-8 max-w-4xl">
        {/* 비밀번호 입력 */}
        {isPasswordRequired && (
          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl mb-8">
            <CardContent className="p-8 text-center">
              <Lock className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <h2 className="text-xl font-bold text-gray-800 mb-2">
                비밀글입니다
              </h2>
              <p className="text-gray-600 mb-6">
                이 게시글을 보려면 비밀번호를 입력해주세요.
              </p>
              <div className="flex items-center space-x-2 max-w-sm mx-auto">
                <Input
                  type="password"
                  placeholder="비밀번호"
                  value={passwordInput}
                  onChange={(e) => setPasswordInput(e.target.value)}
                  onKeyPress={(e) =>
                    e.key === "Enter" && handlePasswordSubmit()
                  }
                />
                <Button onClick={handlePasswordSubmit}>확인</Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* 게시글 내용 */}
        {!isPasswordRequired && (
          <>
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl mb-8">
              <CardHeader className="border-b">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-2 mb-2">
                      {post.password && (
                        <Lock className="w-4 h-4 text-red-500" />
                      )}
                      {post.notice && (
                        <Badge className="bg-red-500 text-white">공지</Badge>
                      )}
                      {post.popularFlag && (
                        <Badge className="bg-orange-500 text-white">
                          {post.popularFlag === "REALTIME"
                            ? "실시간"
                            : post.popularFlag === "WEEKLY"
                            ? "주간"
                            : "레전드"}
                        </Badge>
                      )}
                    </div>
                    <CardTitle className="text-2xl font-bold text-gray-800 mb-3">
                      {post.title}
                    </CardTitle>
                    <div className="flex items-center space-x-4 text-sm text-gray-600">
                      <div className="flex items-center space-x-2">
                        <Avatar className="w-6 h-6">
                          <AvatarFallback className="bg-gradient-to-r from-pink-500 to-purple-600 text-white text-xs">
                            {post.userName.charAt(0)}
                          </AvatarFallback>
                        </Avatar>
                        <span>{post.userName}</span>
                      </div>
                      <span>{post.createdAt}</span>
                      <div className="flex items-center space-x-1">
                        <Eye className="w-4 h-4" />
                        <span>{post.views}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <ThumbsUp className="w-4 h-4" />
                        <span>{post.likes}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <MessageSquare className="w-4 h-4" />
                        <span>{comments.length}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="p-6">
                <div className="prose max-w-none">
                  <div
                    className="text-gray-800 leading-relaxed"
                    dangerouslySetInnerHTML={{ __html: post.content }}
                  />
                </div>

                {/* 추천 버튼 */}
                <div className="flex items-center justify-center mt-8 pt-6 border-t">
                  <Button
                    onClick={handleLike}
                    variant={post.userLike ? "default" : "outline"}
                    className={
                      post.userLike ? "bg-red-500 hover:bg-red-600" : ""
                    }
                    disabled={!isAuthenticated}
                  >
                    <ThumbsUp className="w-4 h-4 mr-2" />
                    추천 {post.likes}
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* 댓글 작성 */}
            {isAuthenticated && (
              <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg mb-8">
                <CardHeader>
                  <CardTitle className="text-lg">댓글 작성</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <Editor value={newComment} onChange={setNewComment} />
                  <div className="flex justify-end">
                    <Button
                      onClick={handleCommentSubmit}
                      disabled={isSubmittingComment || !newComment.trim()}
                      className="bg-gradient-to-r from-pink-500 to-purple-600"
                    >
                      <Send className="w-4 h-4 mr-2" />
                      {isSubmittingComment ? "작성 중..." : "댓글 작성"}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* 댓글 목록 */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <MessageSquare className="w-5 h-5 text-purple-600" />
                  <span>댓글 {comments.length}개</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                {comments.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    첫 번째 댓글을 남겨보세요!
                  </div>
                ) : (
                  <div className="space-y-4">
                    {comments.map((comment) => (
                      <div
                        key={comment.id}
                        className="p-4 bg-gray-50 rounded-lg"
                      >
                        <div className="flex items-start justify-between mb-2">
                          <div className="flex items-center space-x-2">
                            <Avatar className="w-6 h-6">
                              <AvatarFallback className="bg-gradient-to-r from-blue-500 to-green-600 text-white text-xs">
                                {comment.userName.charAt(0)}
                              </AvatarFallback>
                            </Avatar>
                            <span className="font-medium text-gray-800">
                              {comment.userName}
                            </span>
                            <span className="text-xs text-gray-500">
                              {comment.createdAt}
                            </span>
                          </div>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-gray-500 hover:text-red-600"
                          >
                            <ThumbsUp className="w-4 h-4 mr-1" />
                            {comment.likes}
                          </Button>
                        </div>
                        <div
                          className="text-gray-700"
                          dangerouslySetInnerHTML={{ __html: comment.content }}
                        />
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </>
        )}
      </div>
    </div>
  );
}

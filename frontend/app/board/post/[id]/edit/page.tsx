"use client"

import { useState, useEffect } from "react"
import { useRouter, useParams } from "next/navigation"
import { useAuth } from "@/hooks/useAuth"
import { boardApi, type Post } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { ArrowLeft, Save, Eye, Loader2 } from "lucide-react"
import Link from "next/link"

export default function EditPostPage() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth()
  const router = useRouter()
  const params = useParams()
  const postId = Number.parseInt(params.id as string)

  const [post, setPost] = useState<Post | null>(null)
  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const [isPrivate, setIsPrivate] = useState(false)
  const [password, setPassword] = useState("")
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isPreview, setIsPreview] = useState(false)

  // 게시글 정보 조회
  useEffect(() => {
    const fetchPost = async () => {
      if (!postId) return

      try {
        const response = await boardApi.getPost(postId)
        if (response.success && response.data) {
          const postData = response.data
          setPost(postData)
          setTitle(postData.title)
          setContent(postData.content)
          setIsPrivate(!!postData.password)
          setPassword(postData.password?.toString() || "")
        } else {
          alert("게시글을 찾을 수 없습니다.")
          router.push("/board")
        }
      } catch (error) {
        console.error("Failed to fetch post:", error)
        alert("게시글을 불러오는데 실패했습니다.")
        router.push("/board")
      } finally {
        setIsLoading(false)
      }
    }

    fetchPost()
  }, [postId, router])

  // 권한 확인
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push("/login")
    } else if (!authLoading && isAuthenticated && post && user?.userId !== post.userId) {
      alert("수정 권한이 없습니다.")
      router.push(`/board/post/${postId}`)
    }
  }, [authLoading, isAuthenticated, post, user, postId, router])

  if (authLoading || isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-12 h-12 text-purple-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">게시글을 불러오는 중...</p>
        </div>
      </div>
    )
  }

  if (!post) {
    return null
  }

  const handleSubmit = async () => {
    if (!title.trim() || !content.trim()) {
      alert("제목과 내용을 모두 입력해주세요.")
      return
    }

    if (isPrivate && !password) {
      alert("비밀글로 설정하려면 비밀번호를 입력해주세요.")
      return
    }

    setIsSubmitting(true)
    try {
      const updatedPost: Post = {
        ...post,
        title: title.trim(),
        content: content.trim(),
        ...(isPrivate && password && { password: Number.parseInt(password) }),
        ...(!isPrivate && { password: undefined }),
      }

      const response = await boardApi.updatePost(updatedPost)
      if (response.success) {
        alert("게시글이 성공적으로 수정되었습니다!")
        router.push(`/board/post/${postId}`)
      } else {
        alert("게시글 수정에 실패했습니다. 다시 시도해주세요.")
      }
    } catch (error) {
      console.error("Failed to update post:", error)
      alert("게시글 수정 중 오류가 발생했습니다.")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link href={`/board/post/${postId}`}>
              <Button variant="ghost" size="sm">
                <ArrowLeft className="w-4 h-4 mr-2" />
                게시글로
              </Button>
            </Link>
            <h1 className="text-xl font-bold text-gray-800">게시글 수정</h1>
          </div>
          <div className="flex items-center space-x-2">
            <Button variant="outline" onClick={() => setIsPreview(!isPreview)} className="bg-white">
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
                  <Label htmlFor="title" className="text-sm font-medium text-gray-700">
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
                  <Label htmlFor="content" className="text-sm font-medium text-gray-700">
                    내용
                  </Label>
                  <Textarea
                    id="content"
                    placeholder="내용을 입력하세요. Enter를 눌러 문단을 나눌 수 있습니다."
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    rows={15}
                    className="resize-none border-2 border-gray-200 focus:border-purple-400 font-mono text-sm leading-relaxed"
                  />
                  <p className="text-xs text-gray-500">
                    💡 팁: Enter를 두 번 누르면 문단이 나뉩니다. 마크다운 문법은 지원하지 않습니다.
                  </p>
                </div>

                {/* 비밀글 설정 */}
                <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
                  <div className="flex items-center justify-between">
                    <div>
                      <Label htmlFor="private" className="text-sm font-medium text-gray-700">
                        비밀글로 작성
                      </Label>
                      <p className="text-xs text-gray-500">비밀번호를 설정하여 특정 사용자만 볼 수 있게 합니다</p>
                    </div>
                    <Switch id="private" checked={isPrivate} onCheckedChange={setIsPrivate} />
                  </div>

                  {isPrivate && (
                    <div className="space-y-2">
                      <Label htmlFor="password" className="text-sm font-medium text-gray-700">
                        비밀번호 (숫자만)
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
                      <span className="text-white text-sm font-bold">{post.userName?.charAt(0)}</span>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-800">작성자: {post.userName}</p>
                      <p className="text-xs text-gray-600">원본 작성일: {post.createdAt}</p>
                    </div>
                  </div>
                </div>
              </>
            ) : (
              /* 미리보기 */
              <div className="space-y-6">
                <div className="border-b pb-4">
                  <h1 className="text-2xl font-bold text-gray-800 mb-2">{title || "제목을 입력하세요"}</h1>
                  <div className="flex items-center space-x-4 text-sm text-gray-600">
                    <span>작성자: {post.userName}</span>
                    <span>작성일: {post.createdAt}</span>
                    <span className="text-orange-600">수정됨</span>
                    {isPrivate && <span className="text-red-600">🔒 비밀글</span>}
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
  )
}

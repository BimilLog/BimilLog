"use client"

import { useState, useEffect } from "react"
import { boardApi, type SimplePost } from "@/lib/api"
import { useAuth } from "@/hooks/useAuth"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { MessageSquare, ThumbsUp, Search, TrendingUp, Clock, Crown, ArrowLeft, Pin, Edit } from "lucide-react"
import Link from "next/link"

export default function BoardPage() {
  const { isAuthenticated } = useAuth()
  const [searchTerm, setSearchTerm] = useState("")
  const [postsPerPage, setPostsPerPage] = useState("30")
  const [posts, setPosts] = useState<SimplePost[]>([])
  const [popularPosts, setPopularPosts] = useState<SimplePost[]>([])
  const [isLoading, setIsLoading] = useState(true)

  const notices = [
    {
      id: 1,
      title: "비밀로그 서비스 이용약관 안내",
      author: "관리자",
      date: "2024.01.20",
      views: 1234,
      isPinned: true,
    },
    { id: 2, title: "새로운 기능 업데이트 안내", author: "관리자", date: "2024.01.18", views: 856, isPinned: true },
  ]

  // 게시글 목록 조회
  useEffect(() => {
    const fetchPosts = async () => {
      try {
        const response = await boardApi.getPosts(0, Number.parseInt(postsPerPage))
        if (response.success && response.data) {
          setPosts(response.data.content)
        }
      } catch (error) {
        console.error("Failed to fetch posts:", error)
      } finally {
        setIsLoading(false)
      }
    }

    fetchPosts()
  }, [searchTerm, postsPerPage])

  // 인기글 조회
  useEffect(() => {
    const fetchPopularPosts = async () => {
      try {
        const response = await boardApi.getRealtimePosts()
        if (response.success && response.data) {
          setPopularPosts(response.data)
        }
      } catch (error) {
        console.error("Failed to fetch popular posts:", error)
      }
    }

    fetchPopularPosts()
  }, [])

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link href="/">
              <Button variant="ghost" size="sm">
                <ArrowLeft className="w-4 h-4 mr-2" />
                홈으로
              </Button>
            </Link>
            <div className="flex items-center space-x-2">
              <MessageSquare className="w-6 h-6 text-purple-600" />
              <h1 className="text-xl font-bold text-gray-800">커뮤니티 게시판</h1>
            </div>
          </div>
          {isAuthenticated && (
            <Button
              asChild
              className="bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700"
            >
              <Link href="/board/write">
                <Edit className="w-4 h-4 mr-2" />
                글쓰기
              </Link>
            </Button>
          )}
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        {/* Search and Filters */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg mb-6">
          <CardContent className="p-6">
            <div className="flex flex-col md:flex-row gap-4 items-center">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                <Input
                  placeholder="게시글 또는 작성자 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10 bg-white"
                />
              </div>
              <Select value={postsPerPage} onValueChange={setPostsPerPage}>
                <SelectTrigger className="w-32 bg-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="10">10개씩</SelectItem>
                  <SelectItem value="30">30개씩</SelectItem>
                  <SelectItem value="50">50개씩</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        <Tabs defaultValue="all" className="space-y-6">
          <TabsList className="grid w-full grid-cols-4 bg-white/80 backdrop-blur-sm">
            <TabsTrigger value="all" className="flex items-center space-x-2">
              <MessageSquare className="w-4 h-4" />
              <span>전체글</span>
            </TabsTrigger>
            <TabsTrigger value="popular" className="flex items-center space-x-2">
              <TrendingUp className="w-4 h-4" />
              <span>인기글</span>
            </TabsTrigger>
            <TabsTrigger value="recent" className="flex items-center space-x-2">
              <Clock className="w-4 h-4" />
              <span>최신글</span>
            </TabsTrigger>
            <TabsTrigger value="legend" className="flex items-center space-x-2">
              <Crown className="w-4 h-4" />
              <span>레전드</span>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="all" className="space-y-4">
            {/* Notice Posts */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2 text-lg">
                  <Pin className="w-5 h-5 text-red-500" />
                  <span>공지사항</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {notices.map((notice) => (
                  <div
                    key={notice.id}
                    className="flex items-center justify-between p-3 bg-red-50 rounded-lg border border-red-100"
                  >
                    <div className="flex-1">
                      <div className="flex items-center space-x-2">
                        <Badge className="bg-red-500 text-white">공지</Badge>
                        <h3 className="font-medium text-gray-800">{notice.title}</h3>
                      </div>
                      <div className="flex items-center space-x-4 mt-1 text-sm text-gray-500">
                        <span>{notice.author}</span>
                        <span>{notice.date}</span>
                        <span>조회 {notice.views}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>

            {/* Regular Posts */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardContent className="p-6">
                <div className="space-y-3">
                  {posts.map((post) => (
                    <Link key={post.postId} href={`/board/post/${post.postId}`}>
                      <div className="flex items-center justify-between p-4 hover:bg-gray-50 rounded-lg transition-colors cursor-pointer">
                        <div className="flex-1">
                          <div className="flex items-center space-x-2">
                            {post.popularFlag && <Badge className="bg-orange-500 text-white">HOT</Badge>}
                            <h3 className="font-medium text-gray-800 hover:text-purple-600 transition-colors">
                              {post.title}
                            </h3>
                          </div>
                          <div className="flex items-center space-x-4 mt-2 text-sm text-gray-500">
                            <span>{post.userName}</span>
                            <span>{post.createdAt}</span>
                            <span>조회 {post.views}</span>
                            <div className="flex items-center space-x-1">
                              <ThumbsUp className="w-4 h-4" />
                              <span>{post.likes}</span>
                            </div>
                            <div className="flex items-center space-x-1">
                              <MessageSquare className="w-4 h-4" />
                              <span>{post.commentCount}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="popular" className="space-y-4">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <TrendingUp className="w-5 h-5 text-orange-500" />
                  <span>실시간 인기글</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {popularPosts.map((post, index) => (
                  <Link key={post.postId} href={`/board/post/${post.postId}`}>
                    <div className="flex items-center space-x-4 p-4 hover:bg-gray-50 rounded-lg transition-colors cursor-pointer">
                      <div className="flex-shrink-0">
                        <div
                          className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-white ${
                            index === 0 ? "bg-yellow-500" : index === 1 ? "bg-gray-400" : "bg-orange-400"
                          }`}
                        >
                          {index + 1}
                        </div>
                      </div>
                      <div className="flex-1">
                        <h3 className="font-medium text-gray-800 hover:text-purple-600 transition-colors">
                          {post.title}
                        </h3>
                        <div className="flex items-center space-x-4 mt-1 text-sm text-gray-500">
                          <span>{post.userName}</span>
                          <span>{post.createdAt}</span>
                          <div className="flex items-center space-x-1">
                            <ThumbsUp className="w-4 h-4" />
                            <span>{post.likes}</span>
                          </div>
                          <div className="flex items-center space-x-1">
                            <MessageSquare className="w-4 h-4" />
                            <span>{post.commentCount}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </Link>
                ))}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="recent">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Clock className="w-5 h-5 text-blue-500" />
                  <span>최신글</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-gray-500 text-center py-8">최신 게시글을 불러오는 중...</p>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="legend">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Crown className="w-5 h-5 text-purple-500" />
                  <span>레전드 글</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-gray-500 text-center py-8">레전드 게시글을 불러오는 중...</p>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        {/* Pagination */}
        <div className="flex justify-center mt-8">
          <div className="flex items-center space-x-2">
            <Button variant="outline" size="sm" className="bg-white">
              이전
            </Button>
            <Button size="sm" className="bg-purple-600 text-white">
              1
            </Button>
            <Button variant="outline" size="sm" className="bg-white">
              2
            </Button>
            <Button variant="outline" size="sm" className="bg-white">
              3
            </Button>
            <Button variant="outline" size="sm" className="bg-white">
              다음
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}

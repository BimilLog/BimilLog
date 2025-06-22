"use client"

import { useState, useEffect } from "react"
import { useAuth } from "@/hooks/useAuth"
import { userApi, rollingPaperApi } from "@/lib/api"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Switch } from "@/components/ui/switch"
import {
  User,
  MessageSquare,
  ThumbsUp,
  FileText,
  MessageCircle,
  Settings,
  Trash2,
  Edit,
  ArrowLeft,
  Heart,
} from "lucide-react"
import Link from "next/link"

export default function MyPage() {
  const { user, isAuthenticated, isLoading, logout, deleteAccount, updateUserName, refreshUser } = useAuth()
  const router = useRouter()
  const [userStats, setUserStats] = useState({
    totalMessages: 0,
    totalPosts: 0,
    totalComments: 0,
    totalLikes: 0,
  })
  const [notifications, setNotifications] = useState({
    newMessage: true,
    newComment: true,
    popularPost: false,
    system: true,
  })
  const [nicknameInput, setNicknameInput] = useState(user?.userName || "") // 닉네임 입력 필드 상태
  const [isNicknameChangeSubmitting, setIsNicknameChangeSubmitting] = useState(false)

  // 닉네임 변경 처리
  const handleNicknameChange = async () => {
    if (!nicknameInput.trim() || nicknameInput === user?.userName) {
      alert("새 닉네임을 입력하거나 현재 닉네임과 다른 닉네임을 입력해주세요.")
      return
    }
    setIsNicknameChangeSubmitting(true)
    try {
      const success = await updateUserName(nicknameInput)
      if (success) {
        alert("닉네임이 성공적으로 변경되었습니다!")
        await refreshUser() // 변경된 닉네임으로 사용자 정보 새로고침
      } else {
        alert("닉네임 변경에 실패했습니다. 이미 사용 중인 닉네임일 수 있습니다.")
      }
    } catch (error) {
      console.error("Failed to change nickname:", error)
      alert("닉네임 변경 중 오류가 발생했습니다.")
    } finally {
      setIsNicknameChangeSubmitting(false)
    }
  }

  // 로그인하지 않은 사용자는 로그인 페이지로 리다이렉트
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login")
    }
  }, [isAuthenticated, isLoading, router])

  // 사용자 통계 조회
  useEffect(() => {
    const fetchUserStats = async () => {
      if (!isAuthenticated) return

      try {
        const [postsResponse, commentsResponse, messagesResponse] = await Promise.all([
          userApi.getUserPosts(0, 1),
          userApi.getUserComments(0, 1),
          rollingPaperApi.getMyRollingPaper(),
        ])

        setUserStats({
          totalPosts: postsResponse.data?.totalElements || 0,
          totalComments: commentsResponse.data?.totalElements || 0,
          totalMessages: messagesResponse.data?.messages.length || 0,
          totalLikes: 0, // API에서 추가 구현 필요
        })
      } catch (error) {
        console.error("Failed to fetch user stats:", error)
      }
    }

    fetchUserStats()
  }, [isAuthenticated])

  // 사용자 설정 조회
  useEffect(() => {
    const fetchUserSettings = async () => {
      if (!isAuthenticated) return

      try {
        const response = await userApi.getUserSettings()
        if (response.success && response.data) {
          setNotifications({
            newMessage: response.data.messageNotification,
            newComment: response.data.commentNotification,
            popularPost: response.data.popularPostNotification,
            system: true, // 시스템 알림은 항상 true
          })
        }
      } catch (error) {
        console.error("Failed to fetch user settings:", error)
      }
    }

    fetchUserSettings()
  }, [isAuthenticated])

  // 설정 업데이트
  const updateNotificationSettings = async (newSettings: typeof notifications) => {
    try {
      const response = await userApi.updateUserSettings({
        messageNotification: newSettings.newMessage,
        commentNotification: newSettings.newComment,
        popularPostNotification: newSettings.popularPost,
      })
      if (response.success) {
        setNotifications(newSettings)
      }
    } catch (error) {
      console.error("Failed to update settings:", error)
    }
  }

  // 회원 탈퇴 처리
  const handleDeleteAccount = async () => {
    if (confirm("정말로 회원 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) {
      const success = await deleteAccount()
      if (!success) {
        alert("회원 탈퇴에 실패했습니다. 다시 시도해주세요.")
      }
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <User className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-gray-600">사용자 정보를 불러오는 중...</p>
        </div>
      </div>
    )
  }

  if (!isAuthenticated || !user) {
    return null
  }

  const myMessages = [
    { id: 1, content: "안녕하세요! 좋은 하루 되세요", author: "익명의 친구", date: "2024.01.20", isDeleted: false },
    { id: 2, content: "항상 응원하고 있어요!", author: "응원단장", date: "2024.01.19", isDeleted: false },
    { id: 3, content: "감사합니다 :)", author: "고마운 사람", date: "2024.01.18", isDeleted: true },
  ]

  const myPosts = [
    { id: 1, title: "롤링페이퍼 정말 좋은 아이디어네요!", date: "2024.01.20", views: 45, likes: 12, comments: 8 },
    { id: 2, title: "친구들과 함께 사용하니까 너무 재밌어요", date: "2024.01.19", views: 32, likes: 7, comments: 3 },
  ]

  const myComments = [
    { id: 1, content: "정말 좋은 글이네요! 감사합니다", postTitle: "비밀로그 사용법", date: "2024.01.20", likes: 5 },
    { id: 2, content: "저도 같은 생각이에요", postTitle: "익명 메시지의 매력", date: "2024.01.19", likes: 3 },
  ]

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
              <User className="w-6 h-6 text-purple-600" />
              <h1 className="text-xl font-bold text-gray-800">마이페이지</h1>
            </div>
          </div>
          <Dialog>
            <DialogTrigger asChild>
              <Button variant="outline" className="bg-white">
                <Settings className="w-4 h-4 mr-2" />
                설정
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>설정</DialogTitle>
              </DialogHeader>
              <div className="space-y-6">
                <div>
                  <h3 className="font-medium mb-4">알림 설정</h3>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm">새 메시지 알림</span>
                      <Switch
                        checked={notifications.newMessage}
                        onCheckedChange={(checked) =>
                          updateNotificationSettings({ ...notifications, newMessage: checked })
                        }
                      />
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm">댓글 알림</span>
                      <Switch
                        checked={notifications.newComment}
                        onCheckedChange={(checked) =>
                          updateNotificationSettings({ ...notifications, newComment: checked })
                        }
                      />
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm">인기글 선정 알림</span>
                      <Switch
                        checked={notifications.popularPost}
                        onCheckedChange={(checked) =>
                          updateNotificationSettings({ ...notifications, popularPost: checked })
                        }
                      />
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm">시스템 알림</span>
                      <Switch
                        checked={notifications.system}
                        onCheckedChange={(checked) => setNotifications((prev) => ({ ...prev, system: checked }))}
                      />
                    </div>
                  </div>
                </div>
                <div className="pt-4 border-t">
                  <Button variant="destructive" className="w-full" onClick={handleDeleteAccount}>
                    회원 탈퇴
                  </Button>
                </div>
              </div>
            </DialogContent>
          </Dialog>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        {/* Profile Section */}
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg mb-8">
          <CardContent className="p-6">
            <div className="flex items-center space-x-6">
              <Avatar className="w-20 h-20">
                <AvatarFallback className="bg-gradient-to-r from-pink-500 to-purple-600 text-white text-2xl">
                  {user.nickname.charAt(0)}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1">
                <div className="flex items-center space-x-3 mb-2">
                  <h2 className="text-2xl font-bold text-gray-800">{user.nickname}</h2>
                  <Dialog>
                    <DialogTrigger asChild>
                      <Button variant="outline" size="sm">
                        <Edit className="w-4 h-4 mr-2" />
                        수정
                      </Button>
                    </DialogTrigger>
                    <DialogContent>
                      <DialogHeader>
                        <DialogTitle>닉네임 변경</DialogTitle>
                      </DialogHeader>
                      <div className="space-y-4">
                        <Input
                          value={nicknameInput}
                          onChange={(e) => setNicknameInput(e.target.value)}
                          placeholder="새 닉네임"
                        />
                        <Button className="w-full" onClick={handleNicknameChange} disabled={isNicknameChangeSubmitting}>
                          {isNicknameChangeSubmitting ? "변경 중..." : "변경하기"}
                        </Button>
                      </div>
                    </DialogContent>
                  </Dialog>
                </div>
                <p className="text-gray-600">비밀로그와 함께한 지 30일</p>
              </div>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
              <div className="text-center p-3 bg-pink-50 rounded-lg">
                <MessageSquare className="w-6 h-6 text-pink-600 mx-auto mb-1" />
                <div className="text-lg font-bold text-gray-800">{userStats.totalMessages}</div>
                <div className="text-sm text-gray-600">받은 메시지</div>
              </div>
              <div className="text-center p-3 bg-blue-50 rounded-lg">
                <FileText className="w-6 h-6 text-blue-600 mx-auto mb-1" />
                <div className="text-lg font-bold text-gray-800">{userStats.totalPosts}</div>
                <div className="text-sm text-gray-600">작성한 글</div>
              </div>
              <div className="text-center p-3 bg-green-50 rounded-lg">
                <MessageCircle className="w-6 h-6 text-green-600 mx-auto mb-1" />
                <div className="text-lg font-bold text-gray-800">{userStats.totalComments}</div>
                <div className="text-sm text-gray-600">작성한 댓글</div>
              </div>
              <div className="text-center p-3 bg-purple-50 rounded-lg">
                <Heart className="w-6 h-6 text-purple-600 mx-auto mb-1" />
                <div className="text-lg font-bold text-gray-800">{userStats.totalLikes}</div>
                <div className="text-sm text-gray-600">받은 추천</div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Content Tabs */}
        <Tabs defaultValue="messages" className="space-y-6">
          <TabsList className="grid w-full grid-cols-4 bg-white/80 backdrop-blur-sm">
            <TabsTrigger value="messages">받은 메시지</TabsTrigger>
            <TabsTrigger value="posts">내 글</TabsTrigger>
            <TabsTrigger value="comments">내 댓글</TabsTrigger>
            <TabsTrigger value="likes">추천한 글</TabsTrigger>
          </TabsList>

          <TabsContent value="messages">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <MessageSquare className="w-5 h-5 text-pink-600" />
                  <span>받은 메시지</span>
                  <Badge variant="secondary">{myMessages.length}</Badge>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {myMessages.map((message) => (
                  <div
                    key={message.id}
                    className={`p-4 rounded-lg border ${message.isDeleted ? "bg-gray-50 border-gray-200" : "bg-white border-gray-200"}`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <p className={`text-gray-800 mb-2 ${message.isDeleted ? "line-through text-gray-500" : ""}`}>
                          {message.content}
                        </p>
                        <div className="flex items-center space-x-4 text-sm text-gray-500">
                          <Badge variant="outline">{message.author}</Badge>
                          <span>{message.date}</span>
                          {message.isDeleted && <Badge variant="secondary">삭제됨</Badge>}
                        </div>
                      </div>
                      <Button variant="outline" size="sm" className="text-red-600 border-red-200 hover:bg-red-50">
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="posts">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <FileText className="w-5 h-5 text-blue-600" />
                  <span>내가 쓴 글</span>
                  <Badge variant="secondary">{myPosts.length}</Badge>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {myPosts.map((post) => (
                  <div key={post.id} className="p-4 bg-white rounded-lg border border-gray-200">
                    <h3 className="font-medium text-gray-800 mb-2 hover:text-purple-600 cursor-pointer">
                      {post.title}
                    </h3>
                    <div className="flex items-center space-x-4 text-sm text-gray-500">
                      <span>{post.date}</span>
                      <span>조회 {post.views}</span>
                      <div className="flex items-center space-x-1">
                        <ThumbsUp className="w-4 h-4" />
                        <span>{post.likes}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <MessageCircle className="w-4 h-4" />
                        <span>{post.comments}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="comments">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <MessageCircle className="w-5 h-5 text-green-600" />
                  <span>내가 쓴 댓글</span>
                  <Badge variant="secondary">{myComments.length}</Badge>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {myComments.map((comment) => (
                  <div key={comment.id} className="p-4 bg-white rounded-lg border border-gray-200">
                    <p className="text-gray-800 mb-2">{comment.content}</p>
                    <div className="flex items-center space-x-4 text-sm text-gray-500">
                      <span className="text-purple-600 hover:underline cursor-pointer">{comment.postTitle}</span>
                      <span>{comment.date}</span>
                      <div className="flex items-center space-x-1">
                        <ThumbsUp className="w-4 h-4" />
                        <span>{comment.likes}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="likes">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Heart className="w-5 h-5 text-purple-600" />
                  <span>추천한 글</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-gray-500 text-center py-8">추천한 글이 없습니다.</p>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}

"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Textarea } from "@/components/ui/textarea"
import {
  User,
  ArrowLeft,
  Ban,
  CheckCircle,
  MessageSquare,
  FileText,
  AlertTriangle,
  Calendar,
  Mail,
  Activity,
  Shield,
} from "lucide-react"
import Link from "next/link"

export default function UserDetailPage({ params }: { params: { id: string } }) {
  const [banReason, setBanReason] = useState("")
  const [showBanDialog, setShowBanDialog] = useState(false)

  // Mock user data
  const userDetail = {
    id: params.id,
    nickname: "스팸유저1",
    email: "spam@kakao.com",
    joinDate: "2024.01.18",
    lastActive: "2024.01.20 14:30",
    status: "reported",
    avatar: null,
    stats: {
      postsCount: 45,
      messagesCount: 67,
      commentsCount: 23,
      likesReceived: 12,
      reportsCount: 8,
      loginCount: 156,
    },
    recentPosts: [
      {
        id: 1,
        title: "이 제품으로 돈을 벌어보세요!",
        content: "지금 바로 연락하면 특별 할인!",
        date: "2024.01.20 14:30",
        views: 23,
        likes: 1,
        reports: 5,
        status: "reported",
      },
      {
        id: 2,
        title: "부업으로 월 100만원 벌기",
        content: "간단한 방법으로 돈을 벌 수 있어요",
        date: "2024.01.20 13:15",
        views: 45,
        likes: 2,
        reports: 3,
        status: "reported",
      },
    ],
    recentMessages: [
      {
        id: 1,
        content: "안녕하세요! 좋은 제품 소개해드릴게요",
        recipient: "사용자123",
        date: "2024.01.20 15:00",
        reports: 2,
      },
      {
        id: 2,
        content: "연락 주시면 자세히 설명드릴게요",
        recipient: "사용자456",
        date: "2024.01.20 14:45",
        reports: 1,
      },
    ],
    reportHistory: [
      {
        id: 1,
        type: "post",
        reason: "스팸 게시글",
        reporter: "신고자1",
        date: "2024.01.20 14:30",
        status: "pending",
      },
      {
        id: 2,
        type: "post",
        reason: "광고성 게시글",
        reporter: "신고자2",
        date: "2024.01.20 13:15",
        status: "pending",
      },
      {
        id: 3,
        type: "message",
        reason: "스팸 메시지",
        reporter: "신고자3",
        date: "2024.01.20 12:00",
        status: "resolved",
      },
    ],
    actionHistory: [
      {
        action: "계정 생성",
        admin: "시스템",
        date: "2024.01.18 10:30",
        note: "카카오 로그인으로 계정 생성",
      },
      {
        action: "첫 신고 접수",
        admin: "시스템",
        date: "2024.01.19 14:20",
        note: "스팸 게시글로 신고됨",
      },
      {
        action: "경고 발송",
        admin: "관리자1",
        date: "2024.01.19 15:00",
        note: "스팸 활동에 대한 경고 메시지 발송",
      },
    ],
  }

  const handleBanUser = () => {
    // API call to ban user
    console.log("User banned:", { userId: params.id, reason: banReason })
    setShowBanDialog(false)
  }

  const handleUnbanUser = () => {
    // API call to unban user
    console.log("User unbanned:", params.id)
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link href="/admin">
              <Button variant="ghost" size="sm">
                <ArrowLeft className="w-4 h-4 mr-2" />
                관리자 대시보드
              </Button>
            </Link>
            <div className="flex items-center space-x-2">
              <User className="w-6 h-6 text-blue-600" />
              <h1 className="text-xl font-bold text-gray-800">사용자 상세 정보</h1>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            {userDetail.status !== "blocked" ? (
              <Dialog open={showBanDialog} onOpenChange={setShowBanDialog}>
                <DialogTrigger asChild>
                  <Button variant="outline" className="text-red-600 border-red-200 hover:bg-red-50">
                    <Ban className="w-4 h-4 mr-2" />
                    사용자 차단
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>사용자 차단</DialogTitle>
                  </DialogHeader>
                  <div className="space-y-4">
                    <p className="text-sm text-gray-600">
                      <strong>{userDetail.nickname}</strong> 사용자를 차단하시겠습니까?
                    </p>
                    <div>
                      <label className="text-sm font-medium text-gray-700">차단 사유</label>
                      <Textarea
                        placeholder="차단 사유를 입력하세요..."
                        value={banReason}
                        onChange={(e) => setBanReason(e.target.value)}
                        rows={3}
                        className="mt-1"
                      />
                    </div>
                    <div className="flex items-center space-x-2">
                      <Button onClick={handleBanUser} className="bg-red-600 hover:bg-red-700">
                        차단하기
                      </Button>
                      <Button variant="outline" onClick={() => setShowBanDialog(false)}>
                        취소
                      </Button>
                    </div>
                  </div>
                </DialogContent>
              </Dialog>
            ) : (
              <Button onClick={handleUnbanUser} className="bg-green-600 hover:bg-green-700">
                <CheckCircle className="w-4 h-4 mr-2" />
                차단 해제
              </Button>
            )}
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        <div className="grid lg:grid-cols-3 gap-8">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            {/* User Profile */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardContent className="p-6">
                <div className="flex items-center space-x-6 mb-6">
                  <Avatar className="w-20 h-20">
                    <AvatarFallback className="bg-gradient-to-r from-red-500 to-orange-600 text-white text-2xl">
                      {userDetail.nickname.charAt(0)}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <h2 className="text-2xl font-bold text-gray-800">{userDetail.nickname}</h2>
                      <Badge
                        className={`${
                          userDetail.status === "active"
                            ? "bg-green-100 text-green-800"
                            : userDetail.status === "reported"
                              ? "bg-yellow-100 text-yellow-800"
                              : "bg-red-100 text-red-800"
                        }`}
                      >
                        {userDetail.status === "active"
                          ? "활성"
                          : userDetail.status === "reported"
                            ? "신고됨"
                            : "차단됨"}
                      </Badge>
                    </div>
                    <div className="flex items-center space-x-4 text-sm text-gray-600">
                      <div className="flex items-center space-x-1">
                        <Mail className="w-4 h-4" />
                        <span>{userDetail.email}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <Calendar className="w-4 h-4" />
                        <span>가입: {userDetail.joinDate}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <Activity className="w-4 h-4" />
                        <span>최종: {userDetail.lastActive}</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* User Stats */}
                <div className="grid grid-cols-3 md:grid-cols-6 gap-4">
                  <div className="text-center p-3 bg-blue-50 rounded-lg">
                    <div className="text-lg font-bold text-blue-600">{userDetail.stats.postsCount}</div>
                    <div className="text-xs text-gray-600">게시글</div>
                  </div>
                  <div className="text-center p-3 bg-pink-50 rounded-lg">
                    <div className="text-lg font-bold text-pink-600">{userDetail.stats.messagesCount}</div>
                    <div className="text-xs text-gray-600">메시지</div>
                  </div>
                  <div className="text-center p-3 bg-green-50 rounded-lg">
                    <div className="text-lg font-bold text-green-600">{userDetail.stats.commentsCount}</div>
                    <div className="text-xs text-gray-600">댓글</div>
                  </div>
                  <div className="text-center p-3 bg-purple-50 rounded-lg">
                    <div className="text-lg font-bold text-purple-600">{userDetail.stats.likesReceived}</div>
                    <div className="text-xs text-gray-600">받은 추천</div>
                  </div>
                  <div className="text-center p-3 bg-red-50 rounded-lg">
                    <div className="text-lg font-bold text-red-600">{userDetail.stats.reportsCount}</div>
                    <div className="text-xs text-gray-600">신고 횟수</div>
                  </div>
                  <div className="text-center p-3 bg-indigo-50 rounded-lg">
                    <div className="text-lg font-bold text-indigo-600">{userDetail.stats.loginCount}</div>
                    <div className="text-xs text-gray-600">로그인</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Content Tabs */}
            <Tabs defaultValue="posts" className="space-y-6">
              <TabsList className="grid w-full grid-cols-3 bg-white/80 backdrop-blur-sm">
                <TabsTrigger value="posts">게시글</TabsTrigger>
                <TabsTrigger value="messages">메시지</TabsTrigger>
                <TabsTrigger value="reports">신고 이력</TabsTrigger>
              </TabsList>

              <TabsContent value="posts">
                <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
                  <CardHeader>
                    <CardTitle className="flex items-center space-x-2">
                      <FileText className="w-5 h-5 text-purple-600" />
                      <span>최근 게시글</span>
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {userDetail.recentPosts.map((post) => (
                      <div key={post.id} className="p-4 bg-white rounded-lg border border-gray-200">
                        <div className="flex items-start justify-between mb-2">
                          <h3 className="font-medium text-gray-800">{post.title}</h3>
                          <Badge
                            className={`text-xs ${
                              post.status === "reported" ? "bg-red-100 text-red-800" : "bg-green-100 text-green-800"
                            }`}
                          >
                            {post.status === "reported" ? "신고됨" : "정상"}
                          </Badge>
                        </div>
                        <p className="text-sm text-gray-600 mb-3">{post.content}</p>
                        <div className="flex items-center space-x-4 text-xs text-gray-500">
                          <span>{post.date}</span>
                          <span>조회 {post.views}</span>
                          <span>추천 {post.likes}</span>
                          {post.reports > 0 && <span className="text-red-600 font-medium">신고 {post.reports}회</span>}
                        </div>
                      </div>
                    ))}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="messages">
                <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
                  <CardHeader>
                    <CardTitle className="flex items-center space-x-2">
                      <MessageSquare className="w-5 h-5 text-pink-600" />
                      <span>최근 메시지</span>
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {userDetail.recentMessages.map((message) => (
                      <div key={message.id} className="p-4 bg-white rounded-lg border border-gray-200">
                        <p className="text-sm text-gray-800 mb-2">{message.content}</p>
                        <div className="flex items-center justify-between text-xs text-gray-500">
                          <div className="flex items-center space-x-4">
                            <span>받는이: {message.recipient}</span>
                            <span>{message.date}</span>
                          </div>
                          {message.reports > 0 && (
                            <Badge className="bg-red-100 text-red-800 text-xs">신고 {message.reports}회</Badge>
                          )}
                        </div>
                      </div>
                    ))}
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="reports">
                <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
                  <CardHeader>
                    <CardTitle className="flex items-center space-x-2">
                      <AlertTriangle className="w-5 h-5 text-red-600" />
                      <span>신고 이력</span>
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {userDetail.reportHistory.map((report) => (
                      <div key={report.id} className="p-4 bg-white rounded-lg border border-gray-200">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center space-x-2">
                            <Badge variant="outline" className="text-xs">
                              {report.type === "post" ? "게시글" : report.type === "comment" ? "댓글" : "메시지"}
                            </Badge>
                            <span className="text-sm font-medium text-gray-800">{report.reason}</span>
                          </div>
                          <Badge
                            className={`text-xs ${
                              report.status === "pending"
                                ? "bg-yellow-100 text-yellow-800"
                                : report.status === "resolved"
                                  ? "bg-green-100 text-green-800"
                                  : "bg-red-100 text-red-800"
                            }`}
                          >
                            {report.status === "pending"
                              ? "대기중"
                              : report.status === "resolved"
                                ? "해결됨"
                                : "반려됨"}
                          </Badge>
                        </div>
                        <div className="flex items-center space-x-4 text-xs text-gray-500">
                          <span>신고자: {report.reporter}</span>
                          <span>{report.date}</span>
                        </div>
                      </div>
                    ))}
                  </CardContent>
                </Card>
              </TabsContent>
            </Tabs>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Quick Actions */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Shield className="w-5 h-5 text-blue-600" />
                  <span>빠른 조치</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Button variant="outline" className="w-full justify-start">
                  <MessageSquare className="w-4 h-4 mr-2" />
                  메시지 보내기
                </Button>
                <Button variant="outline" className="w-full justify-start">
                  <FileText className="w-4 h-4 mr-2" />
                  게시글 목록 보기
                </Button>
                <Button
                  variant="outline"
                  className="w-full justify-start text-orange-600 border-orange-200 hover:bg-orange-50"
                >
                  <AlertTriangle className="w-4 h-4 mr-2" />
                  경고 발송
                </Button>
                <Button variant="outline" className="w-full justify-start text-red-600 border-red-200 hover:bg-red-50">
                  <Ban className="w-4 h-4 mr-2" />
                  콘텐츠 삭제
                </Button>
              </CardContent>
            </Card>

            {/* Action History */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Calendar className="w-5 h-5 text-green-600" />
                  <span>조치 이력</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {userDetail.actionHistory.map((item, index) => (
                    <div key={index} className="flex items-start space-x-3">
                      <div className="w-2 h-2 bg-blue-600 rounded-full mt-2"></div>
                      <div className="flex-1">
                        <div className="flex items-center justify-between mb-1">
                          <span className="text-sm font-medium text-gray-800">{item.action}</span>
                          <span className="text-xs text-gray-500">{item.date}</span>
                        </div>
                        <p className="text-sm text-gray-600 mb-1">{item.note}</p>
                        <p className="text-xs text-gray-500">처리자: {item.admin}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Risk Assessment */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <AlertTriangle className="w-5 h-5 text-orange-600" />
                  <span>위험도 평가</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">스팸 활동</span>
                    <Badge className="bg-red-100 text-red-800">높음</Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">신고 빈도</span>
                    <Badge className="bg-red-100 text-red-800">높음</Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">계정 연령</span>
                    <Badge className="bg-yellow-100 text-yellow-800">신규</Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">활동 패턴</span>
                    <Badge className="bg-red-100 text-red-800">의심</Badge>
                  </div>

                  <div className="pt-4 border-t">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-medium text-gray-700">종합 위험도</span>
                      <Badge className="bg-red-500 text-white">높음</Badge>
                    </div>
                    <p className="text-xs text-gray-600">
                      이 사용자는 스팸 활동과 다수의 신고로 인해 높은 위험도로 분류됩니다. 즉시 조치가 필요할 수
                      있습니다.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}

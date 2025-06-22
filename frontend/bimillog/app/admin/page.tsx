"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Textarea } from "@/components/ui/textarea"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import {
  Shield,
  Users,
  AlertTriangle,
  BarChart3,
  Search,
  Ban,
  Eye,
  MessageSquare,
  FileText,
  TrendingUp,
  Calendar,
  Download,
  ArrowLeft,
  CheckCircle,
  XCircle,
  Clock,
} from "lucide-react"
import Link from "next/link"

export default function AdminPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedReport, setSelectedReport] = useState<any>(null)
  const [filterStatus, setFilterStatus] = useState("all")
  const [filterType, setFilterType] = useState("all")

  // Mock data for admin dashboard
  const dashboardStats = {
    totalUsers: 1247,
    activeUsers: 892,
    totalPosts: 3456,
    totalMessages: 8923,
    pendingReports: 23,
    blockedUsers: 15,
    todaySignups: 34,
    todayPosts: 127,
  }

  const recentReports = [
    {
      id: 1,
      type: "post",
      reportType: "spam",
      targetId: "post_123",
      targetTitle: "스팸성 광고글입니다",
      targetAuthor: "스팸유저1",
      reporterNickname: "신고자1",
      reason: "광고성 게시글을 반복적으로 올리고 있습니다",
      status: "pending",
      createdAt: "2024.01.20 14:30",
      content: "이 제품을 사면 돈을 벌 수 있습니다! 지금 바로 연락하세요!",
    },
    {
      id: 2,
      type: "comment",
      reportType: "inappropriate",
      targetId: "comment_456",
      targetTitle: "부적절한 댓글",
      targetAuthor: "문제유저2",
      reporterNickname: "신고자2",
      reason: "욕설과 비방이 포함된 댓글입니다",
      status: "pending",
      createdAt: "2024.01.20 13:15",
      content: "이런 바보같은 글을 왜 올리냐",
    },
    {
      id: 3,
      type: "message",
      reportType: "harassment",
      targetId: "message_789",
      targetTitle: "괴롭힘 메시지",
      targetAuthor: "익명유저",
      reporterNickname: "피해자1",
      reason: "지속적으로 괴롭힘 메시지를 보내고 있습니다",
      status: "resolved",
      createdAt: "2024.01.20 11:45",
      content: "너는 정말 못생겼다",
    },
    {
      id: 4,
      type: "user",
      reportType: "fake_account",
      targetId: "user_101",
      targetTitle: "가짜 계정 신고",
      targetAuthor: "가짜유저",
      reporterNickname: "신고자3",
      reason: "다른 사람을 사칭하는 계정입니다",
      status: "investigating",
      createdAt: "2024.01.20 10:20",
      content: "유명인을 사칭하여 활동하고 있습니다",
    },
  ]

  const userList = [
    {
      id: 1,
      nickname: "사용자123",
      email: "user123@kakao.com",
      joinDate: "2024.01.15",
      lastActive: "2024.01.20 15:30",
      status: "active",
      postsCount: 12,
      messagesCount: 34,
      reportsCount: 0,
    },
    {
      id: 2,
      nickname: "스팸유저1",
      email: "spam@kakao.com",
      joinDate: "2024.01.18",
      lastActive: "2024.01.20 14:30",
      status: "reported",
      postsCount: 45,
      messagesCount: 2,
      reportsCount: 8,
    },
    {
      id: 3,
      nickname: "문제유저2",
      email: "problem@kakao.com",
      joinDate: "2024.01.10",
      lastActive: "2024.01.19 20:15",
      status: "blocked",
      postsCount: 23,
      messagesCount: 67,
      reportsCount: 15,
    },
  ]

  const getStatusColor = (status: string) => {
    switch (status) {
      case "pending":
        return "bg-yellow-100 text-yellow-800 border-yellow-200"
      case "investigating":
        return "bg-blue-100 text-blue-800 border-blue-200"
      case "resolved":
        return "bg-green-100 text-green-800 border-green-200"
      case "rejected":
        return "bg-red-100 text-red-800 border-red-200"
      default:
        return "bg-gray-100 text-gray-800 border-gray-200"
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case "pending":
        return <Clock className="w-4 h-4" />
      case "investigating":
        return <Eye className="w-4 h-4" />
      case "resolved":
        return <CheckCircle className="w-4 h-4" />
      case "rejected":
        return <XCircle className="w-4 h-4" />
      default:
        return <Clock className="w-4 h-4" />
    }
  }

  const getUserStatusColor = (status: string) => {
    switch (status) {
      case "active":
        return "bg-green-100 text-green-800"
      case "reported":
        return "bg-yellow-100 text-yellow-800"
      case "blocked":
        return "bg-red-100 text-red-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Link href="/">
              <Button variant="ghost" size="sm">
                <ArrowLeft className="w-4 h-4 mr-2" />
                메인으로
              </Button>
            </Link>
            <div className="flex items-center space-x-2">
              <Shield className="w-6 h-6 text-blue-600" />
              <h1 className="text-xl font-bold text-gray-800">관리자 대시보드</h1>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Badge className="bg-red-500 text-white">
              <AlertTriangle className="w-4 h-4 mr-1" />
              {dashboardStats.pendingReports}개 대기
            </Badge>
            <Button variant="outline" className="bg-white">
              <Download className="w-4 h-4 mr-2" />
              리포트 다운로드
            </Button>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        {/* Dashboard Stats */}
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-4 mb-8">
          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <Users className="w-6 h-6 text-blue-600 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-800">{dashboardStats.totalUsers.toLocaleString()}</div>
              <div className="text-xs text-gray-600">총 사용자</div>
            </CardContent>
          </Card>

          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <TrendingUp className="w-6 h-6 text-green-600 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-800">{dashboardStats.activeUsers.toLocaleString()}</div>
              <div className="text-xs text-gray-600">활성 사용자</div>
            </CardContent>
          </Card>

          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <FileText className="w-6 h-6 text-purple-600 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-800">{dashboardStats.totalPosts.toLocaleString()}</div>
              <div className="text-xs text-gray-600">총 게시글</div>
            </CardContent>
          </Card>

          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <MessageSquare className="w-6 h-6 text-pink-600 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-800">{dashboardStats.totalMessages.toLocaleString()}</div>
              <div className="text-xs text-gray-600">총 메시지</div>
            </CardContent>
          </Card>

          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <AlertTriangle className="w-6 h-6 text-red-600 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-800">{dashboardStats.pendingReports}</div>
              <div className="text-xs text-gray-600">대기 신고</div>
            </CardContent>
          </Card>

          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <Ban className="w-6 h-6 text-gray-600 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-800">{dashboardStats.blockedUsers}</div>
              <div className="text-xs text-gray-600">차단 사용자</div>
            </CardContent>
          </Card>

          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <Calendar className="w-6 h-6 text-indigo-600 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-800">{dashboardStats.todaySignups}</div>
              <div className="text-xs text-gray-600">오늘 가입</div>
            </CardContent>
          </Card>

          <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
            <CardContent className="p-4 text-center">
              <BarChart3 className="w-6 h-6 text-orange-600 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-800">{dashboardStats.todayPosts}</div>
              <div className="text-xs text-gray-600">오늘 게시글</div>
            </CardContent>
          </Card>
        </div>

        {/* Main Content Tabs */}
        <Tabs defaultValue="reports" className="space-y-6">
          <TabsList className="grid w-full grid-cols-4 bg-white/80 backdrop-blur-sm">
            <TabsTrigger value="reports" className="flex items-center space-x-2">
              <AlertTriangle className="w-4 h-4" />
              <span>신고 관리</span>
            </TabsTrigger>
            <TabsTrigger value="users" className="flex items-center space-x-2">
              <Users className="w-4 h-4" />
              <span>사용자 관리</span>
            </TabsTrigger>
            <TabsTrigger value="content" className="flex items-center space-x-2">
              <FileText className="w-4 h-4" />
              <span>콘텐츠 관리</span>
            </TabsTrigger>
            <TabsTrigger value="analytics" className="flex items-center space-x-2">
              <BarChart3 className="w-4 h-4" />
              <span>통계</span>
            </TabsTrigger>
          </TabsList>

          {/* Reports Management */}
          <TabsContent value="reports" className="space-y-6">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <AlertTriangle className="w-5 h-5 text-red-600" />
                    <span>신고 목록</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Select value={filterStatus} onValueChange={setFilterStatus}>
                      <SelectTrigger className="w-32 bg-white">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">전체 상태</SelectItem>
                        <SelectItem value="pending">대기중</SelectItem>
                        <SelectItem value="investigating">조사중</SelectItem>
                        <SelectItem value="resolved">해결됨</SelectItem>
                        <SelectItem value="rejected">반려됨</SelectItem>
                      </SelectContent>
                    </Select>
                    <Select value={filterType} onValueChange={setFilterType}>
                      <SelectTrigger className="w-32 bg-white">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">전체 유형</SelectItem>
                        <SelectItem value="post">게시글</SelectItem>
                        <SelectItem value="comment">댓글</SelectItem>
                        <SelectItem value="message">메시지</SelectItem>
                        <SelectItem value="user">사용자</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {recentReports.map((report) => (
                  <div
                    key={report.id}
                    className="p-4 bg-white rounded-lg border border-gray-200 hover:shadow-md transition-shadow"
                  >
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex-1">
                        <div className="flex items-center space-x-2 mb-2">
                          <Badge variant="outline" className="text-xs">
                            {report.type === "post"
                              ? "게시글"
                              : report.type === "comment"
                                ? "댓글"
                                : report.type === "message"
                                  ? "메시지"
                                  : "사용자"}
                          </Badge>
                          <Badge variant="outline" className="text-xs">
                            {report.reportType === "spam"
                              ? "스팸"
                              : report.reportType === "inappropriate"
                                ? "부적절"
                                : report.reportType === "harassment"
                                  ? "괴롭힘"
                                  : "가짜계정"}
                          </Badge>
                          <Badge className={`text-xs ${getStatusColor(report.status)}`}>
                            <div className="flex items-center space-x-1">
                              {getStatusIcon(report.status)}
                              <span>
                                {report.status === "pending"
                                  ? "대기중"
                                  : report.status === "investigating"
                                    ? "조사중"
                                    : report.status === "resolved"
                                      ? "해결됨"
                                      : "반려됨"}
                              </span>
                            </div>
                          </Badge>
                        </div>
                        <h3 className="font-medium text-gray-800 mb-1">{report.targetTitle}</h3>
                        <p className="text-sm text-gray-600 mb-2">
                          작성자: {report.targetAuthor} | 신고자: {report.reporterNickname}
                        </p>
                        <p className="text-sm text-gray-700 mb-2">신고 사유: {report.reason}</p>
                        <p className="text-xs text-gray-500">{report.createdAt}</p>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Dialog>
                          <DialogTrigger asChild>
                            <Button variant="outline" size="sm" onClick={() => setSelectedReport(report)}>
                              <Eye className="w-4 h-4 mr-2" />
                              상세보기
                            </Button>
                          </DialogTrigger>
                          <DialogContent className="max-w-2xl">
                            <DialogHeader>
                              <DialogTitle>신고 상세 정보</DialogTitle>
                            </DialogHeader>
                            {selectedReport && (
                              <div className="space-y-4">
                                <div className="grid grid-cols-2 gap-4">
                                  <div>
                                    <label className="text-sm font-medium text-gray-700">신고 유형</label>
                                    <p className="text-sm text-gray-900">
                                      {selectedReport.type === "post"
                                        ? "게시글"
                                        : selectedReport.type === "comment"
                                          ? "댓글"
                                          : selectedReport.type === "message"
                                            ? "메시지"
                                            : "사용자"}
                                    </p>
                                  </div>
                                  <div>
                                    <label className="text-sm font-medium text-gray-700">신고 분류</label>
                                    <p className="text-sm text-gray-900">
                                      {selectedReport.reportType === "spam"
                                        ? "스팸"
                                        : selectedReport.reportType === "inappropriate"
                                          ? "부적절한 내용"
                                          : selectedReport.reportType === "harassment"
                                            ? "괴롭힘"
                                            : "가짜 계정"}
                                    </p>
                                  </div>
                                  <div>
                                    <label className="text-sm font-medium text-gray-700">대상 작성자</label>
                                    <p className="text-sm text-gray-900">{selectedReport.targetAuthor}</p>
                                  </div>
                                  <div>
                                    <label className="text-sm font-medium text-gray-700">신고자</label>
                                    <p className="text-sm text-gray-900">{selectedReport.reporterNickname}</p>
                                  </div>
                                </div>

                                <div>
                                  <label className="text-sm font-medium text-gray-700">신고된 내용</label>
                                  <div className="mt-1 p-3 bg-gray-50 rounded-lg">
                                    <p className="text-sm text-gray-900">{selectedReport.content}</p>
                                  </div>
                                </div>

                                <div>
                                  <label className="text-sm font-medium text-gray-700">신고 사유</label>
                                  <p className="text-sm text-gray-900 mt-1">{selectedReport.reason}</p>
                                </div>

                                <div>
                                  <label className="text-sm font-medium text-gray-700">관리자 메모</label>
                                  <Textarea placeholder="처리 내용을 기록하세요..." className="mt-1" />
                                </div>

                                <div className="flex items-center space-x-2 pt-4 border-t">
                                  <Button className="bg-green-600 hover:bg-green-700">
                                    <CheckCircle className="w-4 h-4 mr-2" />
                                    승인 (콘텐츠 삭제)
                                  </Button>
                                  <Button variant="outline" className="border-red-200 text-red-600 hover:bg-red-50">
                                    <XCircle className="w-4 h-4 mr-2" />
                                    반려
                                  </Button>
                                  <Button
                                    variant="outline"
                                    className="border-orange-200 text-orange-600 hover:bg-orange-50"
                                  >
                                    <Ban className="w-4 h-4 mr-2" />
                                    사용자 차단
                                  </Button>
                                </div>
                              </div>
                            )}
                          </DialogContent>
                        </Dialog>
                      </div>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          </TabsContent>

          {/* User Management */}
          <TabsContent value="users" className="space-y-6">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Users className="w-5 h-5 text-blue-600" />
                    <span>사용자 관리</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                      <Input
                        placeholder="사용자 검색..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="pl-10 w-64 bg-white"
                      />
                    </div>
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {userList.map((user) => (
                    <div
                      key={user.id}
                      className="flex items-center justify-between p-4 bg-white rounded-lg border border-gray-200"
                    >
                      <div className="flex items-center space-x-4">
                        <Avatar className="w-12 h-12">
                          <AvatarFallback className="bg-gradient-to-r from-blue-500 to-purple-600 text-white">
                            {user.nickname.charAt(0)}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <div className="flex items-center space-x-2 mb-1">
                            <h3 className="font-medium text-gray-800">{user.nickname}</h3>
                            <Badge className={`text-xs ${getUserStatusColor(user.status)}`}>
                              {user.status === "active" ? "활성" : user.status === "reported" ? "신고됨" : "차단됨"}
                            </Badge>
                          </div>
                          <p className="text-sm text-gray-600">{user.email}</p>
                          <div className="flex items-center space-x-4 text-xs text-gray-500 mt-1">
                            <span>가입: {user.joinDate}</span>
                            <span>최종: {user.lastActive}</span>
                            <span>게시글: {user.postsCount}</span>
                            <span>메시지: {user.messagesCount}</span>
                            {user.reportsCount > 0 && <span className="text-red-600">신고: {user.reportsCount}회</span>}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Button variant="outline" size="sm">
                          <Eye className="w-4 h-4 mr-2" />
                          상세보기
                        </Button>
                        {user.status !== "blocked" && (
                          <Button variant="outline" size="sm" className="text-red-600 border-red-200 hover:bg-red-50">
                            <Ban className="w-4 h-4 mr-2" />
                            차단
                          </Button>
                        )}
                        {user.status === "blocked" && (
                          <Button
                            variant="outline"
                            size="sm"
                            className="text-green-600 border-green-200 hover:bg-green-50"
                          >
                            <CheckCircle className="w-4 h-4 mr-2" />
                            차단해제
                          </Button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Content Management */}
          <TabsContent value="content" className="space-y-6">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <FileText className="w-5 h-5 text-purple-600" />
                  <span>콘텐츠 관리</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid md:grid-cols-3 gap-6">
                  <Card className="border border-gray-200">
                    <CardContent className="p-6 text-center">
                      <FileText className="w-8 h-8 text-purple-600 mx-auto mb-4" />
                      <h3 className="font-semibold mb-2">게시글 관리</h3>
                      <p className="text-sm text-gray-600 mb-4">신고된 게시글 및 스팸 게시글 관리</p>
                      <Button variant="outline" className="w-full">
                        게시글 관리
                      </Button>
                    </CardContent>
                  </Card>

                  <Card className="border border-gray-200">
                    <CardContent className="p-6 text-center">
                      <MessageSquare className="w-8 h-8 text-pink-600 mx-auto mb-4" />
                      <h3 className="font-semibold mb-2">메시지 관리</h3>
                      <p className="text-sm text-gray-600 mb-4">부적절한 롤링페이퍼 메시지 관리</p>
                      <Button variant="outline" className="w-full">
                        메시지 관리
                      </Button>
                    </CardContent>
                  </Card>

                  <Card className="border border-gray-200">
                    <CardContent className="p-6 text-center">
                      <Shield className="w-8 h-8 text-green-600 mx-auto mb-4" />
                      <h3 className="font-semibold mb-2">필터 설정</h3>
                      <p className="text-sm text-gray-600 mb-4">금지어 및 자동 필터링 설정</p>
                      <Button variant="outline" className="w-full">
                        필터 설정
                      </Button>
                    </CardContent>
                  </Card>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Analytics */}
          <TabsContent value="analytics" className="space-y-6">
            <div className="grid md:grid-cols-2 gap-6">
              <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <BarChart3 className="w-5 h-5 text-orange-600" />
                    <span>사용자 통계</span>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-blue-50 rounded-lg">
                      <span className="text-sm font-medium">일일 활성 사용자</span>
                      <span className="text-lg font-bold text-blue-600">892명</span>
                    </div>
                    <div className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
                      <span className="text-sm font-medium">주간 신규 가입</span>
                      <span className="text-lg font-bold text-green-600">156명</span>
                    </div>
                    <div className="flex items-center justify-between p-3 bg-purple-50 rounded-lg">
                      <span className="text-sm font-medium">월간 활성률</span>
                      <span className="text-lg font-bold text-purple-600">78.5%</span>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
                <CardHeader>
                  <CardTitle className="flex items-center space-x-2">
                    <TrendingUp className="w-5 h-5 text-green-600" />
                    <span>콘텐츠 통계</span>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-3 bg-pink-50 rounded-lg">
                      <span className="text-sm font-medium">일일 메시지</span>
                      <span className="text-lg font-bold text-pink-600">234개</span>
                    </div>
                    <div className="flex items-center justify-between p-3 bg-indigo-50 rounded-lg">
                      <span className="text-sm font-medium">일일 게시글</span>
                      <span className="text-lg font-bold text-indigo-600">127개</span>
                    </div>
                    <div className="flex items-center justify-between p-3 bg-orange-50 rounded-lg">
                      <span className="text-sm font-medium">평균 참여도</span>
                      <span className="text-lg font-bold text-orange-600">85.2%</span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <AlertTriangle className="w-5 h-5 text-red-600" />
                  <span>신고 통계</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="text-center p-4 bg-red-50 rounded-lg">
                    <div className="text-2xl font-bold text-red-600">23</div>
                    <div className="text-sm text-gray-600">대기중인 신고</div>
                  </div>
                  <div className="text-center p-4 bg-yellow-50 rounded-lg">
                    <div className="text-2xl font-bold text-yellow-600">8</div>
                    <div className="text-sm text-gray-600">조사중인 신고</div>
                  </div>
                  <div className="text-center p-4 bg-green-50 rounded-lg">
                    <div className="text-2xl font-bold text-green-600">156</div>
                    <div className="text-sm text-gray-600">해결된 신고</div>
                  </div>
                  <div className="text-center p-4 bg-gray-50 rounded-lg">
                    <div className="text-2xl font-bold text-gray-600">34</div>
                    <div className="text-sm text-gray-600">반려된 신고</div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}

"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Textarea } from "@/components/ui/textarea"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle,
  XCircle,
  Ban,
  Eye,
  MessageSquare,
  User,
  Calendar,
  Flag,
} from "lucide-react"
import Link from "next/link"

export default function ReportDetailPage({ params }: { params: { id: string } }) {
  const [adminNote, setAdminNote] = useState("")
  const [actionTaken, setActionTaken] = useState("")

  // Mock data for report detail
  const reportDetail = {
    id: params.id,
    type: "post",
    reportType: "spam",
    targetId: "post_123",
    targetTitle: "스팸성 광고글입니다",
    targetContent: "이 제품을 사면 돈을 벌 수 있습니다! 지금 바로 연락하세요! 카카오톡: spam123",
    targetAuthor: {
      id: "user_456",
      nickname: "스팸유저1",
      email: "spam@kakao.com",
      joinDate: "2024.01.18",
      postsCount: 45,
      reportsCount: 8,
      status: "reported",
    },
    reporter: {
      nickname: "신고자1",
      reportCount: 3,
    },
    reason: "광고성 게시글을 반복적으로 올리고 있습니다. 같은 내용의 글을 여러 번 게시하고 있어서 신고합니다.",
    status: "pending",
    createdAt: "2024.01.20 14:30",
    evidence: [
      {
        type: "screenshot",
        url: "/placeholder.svg?height=200&width=300",
        description: "스팸 게시글 스크린샷",
      },
      {
        type: "link",
        url: "https://example.com/spam-post",
        description: "문제가 된 게시글 링크",
      },
    ],
    relatedReports: [
      {
        id: "report_789",
        reporter: "신고자2",
        reason: "같은 사용자의 다른 스팸 게시글",
        date: "2024.01.20 13:15",
      },
      {
        id: "report_101",
        reporter: "신고자3",
        reason: "반복적인 광고 게시글",
        date: "2024.01.19 16:45",
      },
    ],
    history: [
      {
        action: "신고 접수",
        admin: "시스템",
        date: "2024.01.20 14:30",
        note: "자동으로 신고가 접수되었습니다.",
      },
      {
        action: "조사 시작",
        admin: "관리자1",
        date: "2024.01.20 15:00",
        note: "신고 내용 검토를 시작했습니다.",
      },
    ],
  }

  const handleAction = (action: string) => {
    setActionTaken(action)
    // Here you would typically make an API call to update the report status
    console.log(`Action taken: ${action}`)
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
              <AlertTriangle className="w-6 h-6 text-red-600" />
              <h1 className="text-xl font-bold text-gray-800">신고 상세 정보</h1>
              <Badge className="bg-red-100 text-red-800">#{reportDetail.id}</Badge>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Badge
              className={`${
                reportDetail.status === "pending"
                  ? "bg-yellow-100 text-yellow-800"
                  : reportDetail.status === "investigating"
                    ? "bg-blue-100 text-blue-800"
                    : reportDetail.status === "resolved"
                      ? "bg-green-100 text-green-800"
                      : "bg-red-100 text-red-800"
              }`}
            >
              {reportDetail.status === "pending"
                ? "대기중"
                : reportDetail.status === "investigating"
                  ? "조사중"
                  : reportDetail.status === "resolved"
                    ? "해결됨"
                    : "반려됨"}
            </Badge>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        <div className="grid lg:grid-cols-3 gap-8">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            {/* Report Overview */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Flag className="w-5 h-5 text-red-600" />
                  <span>신고 개요</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-gray-700">신고 유형</label>
                    <p className="text-sm text-gray-900">
                      {reportDetail.type === "post" ? "게시글" : reportDetail.type === "comment" ? "댓글" : "메시지"}
                    </p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700">신고 분류</label>
                    <p className="text-sm text-gray-900">
                      {reportDetail.reportType === "spam"
                        ? "스팸"
                        : reportDetail.reportType === "inappropriate"
                          ? "부적절한 내용"
                          : "괴롭힘"}
                    </p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700">신고 일시</label>
                    <p className="text-sm text-gray-900">{reportDetail.createdAt}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700">신고자</label>
                    <p className="text-sm text-gray-900">{reportDetail.reporter.nickname}</p>
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700">신고 사유</label>
                  <div className="mt-1 p-3 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-900">{reportDetail.reason}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Reported Content */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <MessageSquare className="w-5 h-5 text-purple-600" />
                  <span>신고된 콘텐츠</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div>
                    <label className="text-sm font-medium text-gray-700">제목</label>
                    <p className="text-sm text-gray-900 mt-1">{reportDetail.targetTitle}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700">내용</label>
                    <div className="mt-1 p-4 bg-red-50 border border-red-200 rounded-lg">
                      <p className="text-sm text-gray-900">{reportDetail.targetContent}</p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Evidence */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Eye className="w-5 h-5 text-blue-600" />
                  <span>증거 자료</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {reportDetail.evidence.map((item, index) => (
                    <div key={index} className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
                      {item.type === "screenshot" ? (
                        <img
                          src={item.url || "/placeholder.svg"}
                          alt={item.description}
                          className="w-16 h-16 object-cover rounded"
                        />
                      ) : (
                        <div className="w-16 h-16 bg-blue-100 rounded flex items-center justify-center">
                          <Eye className="w-6 h-6 text-blue-600" />
                        </div>
                      )}
                      <div>
                        <p className="text-sm font-medium text-gray-900">{item.description}</p>
                        {item.type === "link" && (
                          <a
                            href={item.url}
                            className="text-xs text-blue-600 hover:underline"
                            target="_blank"
                            rel="noopener noreferrer"
                          >
                            {item.url}
                          </a>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Admin Action */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle>관리자 조치</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-gray-700">처리 메모</label>
                  <Textarea
                    placeholder="처리 내용과 사유를 기록하세요..."
                    value={adminNote}
                    onChange={(e) => setAdminNote(e.target.value)}
                    rows={4}
                    className="mt-1"
                  />
                </div>

                <div className="flex flex-wrap gap-3">
                  <Button
                    onClick={() => handleAction("approve")}
                    className="bg-green-600 hover:bg-green-700"
                    disabled={actionTaken !== ""}
                  >
                    <CheckCircle className="w-4 h-4 mr-2" />
                    승인 (콘텐츠 삭제)
                  </Button>
                  <Button
                    onClick={() => handleAction("reject")}
                    variant="outline"
                    className="border-red-200 text-red-600 hover:bg-red-50"
                    disabled={actionTaken !== ""}
                  >
                    <XCircle className="w-4 h-4 mr-2" />
                    반려
                  </Button>
                  <Button
                    onClick={() => handleAction("ban_user")}
                    variant="outline"
                    className="border-orange-200 text-orange-600 hover:bg-orange-50"
                    disabled={actionTaken !== ""}
                  >
                    <Ban className="w-4 h-4 mr-2" />
                    사용자 차단
                  </Button>
                </div>

                {actionTaken && (
                  <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
                    <p className="text-sm text-green-800">
                      조치가 완료되었습니다:{" "}
                      {actionTaken === "approve" ? "승인됨" : actionTaken === "reject" ? "반려됨" : "사용자 차단됨"}
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Target User Info */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <User className="w-5 h-5 text-blue-600" />
                  <span>대상 사용자</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center space-x-3 mb-4">
                  <Avatar className="w-12 h-12">
                    <AvatarFallback className="bg-gradient-to-r from-red-500 to-orange-600 text-white">
                      {reportDetail.targetAuthor.nickname.charAt(0)}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <h3 className="font-medium text-gray-800">{reportDetail.targetAuthor.nickname}</h3>
                    <p className="text-sm text-gray-600">{reportDetail.targetAuthor.email}</p>
                  </div>
                </div>

                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-600">가입일:</span>
                    <span className="text-gray-900">{reportDetail.targetAuthor.joinDate}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">게시글 수:</span>
                    <span className="text-gray-900">{reportDetail.targetAuthor.postsCount}개</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">신고 횟수:</span>
                    <span className="text-red-600 font-medium">{reportDetail.targetAuthor.reportsCount}회</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">상태:</span>
                    <Badge
                      className={`text-xs ${
                        reportDetail.targetAuthor.status === "active"
                          ? "bg-green-100 text-green-800"
                          : reportDetail.targetAuthor.status === "reported"
                            ? "bg-yellow-100 text-yellow-800"
                            : "bg-red-100 text-red-800"
                      }`}
                    >
                      {reportDetail.targetAuthor.status === "active"
                        ? "활성"
                        : reportDetail.targetAuthor.status === "reported"
                          ? "신고됨"
                          : "차단됨"}
                    </Badge>
                  </div>
                </div>

                <Button variant="outline" className="w-full mt-4">
                  사용자 상세 정보
                </Button>
              </CardContent>
            </Card>

            {/* Related Reports */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <AlertTriangle className="w-5 h-5 text-orange-600" />
                  <span>관련 신고</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {reportDetail.relatedReports.map((report) => (
                    <div key={report.id} className="p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm font-medium text-gray-800">#{report.id}</span>
                        <span className="text-xs text-gray-500">{report.date}</span>
                      </div>
                      <p className="text-sm text-gray-600 mb-1">{report.reason}</p>
                      <p className="text-xs text-gray-500">신고자: {report.reporter}</p>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Processing History */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Calendar className="w-5 h-5 text-green-600" />
                  <span>처리 이력</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {reportDetail.history.map((item, index) => (
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
          </div>
        </div>
      </div>
    </div>
  )
}

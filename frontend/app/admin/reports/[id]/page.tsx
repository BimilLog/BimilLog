"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle,
  XCircle,
  Ban,
  Calendar,
  Flag,
} from "lucide-react";
import Link from "next/link";
import { adminApi, type Report } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";

export default function ReportDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { user, isAuthenticated } = useAuth();
  const [adminNote, setAdminNote] = useState("");
  const [actionTaken, setActionTaken] = useState("");
  const [report, setReport] = useState<Report | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [reportId, setReportId] = useState<string | null>(null);

  // params 추출
  useEffect(() => {
    params.then((resolvedParams) => {
      setReportId(resolvedParams.id);
    });
  }, [params]);

  // 권한 확인
  useEffect(() => {
    if (!isAuthenticated || user?.role !== "ADMIN") {
      window.location.href = "/";
    }
  }, [isAuthenticated, user]);

  // 신고 상세 조회
  useEffect(() => {
    const fetchReport = async () => {
      if (!reportId) return;

      try {
        setIsLoading(true);
        const response = await adminApi.getReport(Number(reportId));
        if (response.success && response.data) {
          setReport(response.data as Report);
        }
      } catch (error) {
        console.error("Failed to fetch report:", error);
      } finally {
        setIsLoading(false);
      }
    };

    if (reportId) {
      fetchReport();
    }
  }, [reportId]);

  const handleAction = (action: string) => {
    setActionTaken(action);
    // 실제 구현 시 API 호출 추가
    console.log(`Action taken: ${action}`);
  };

  const getReportTypeLabel = (type: string) => {
    switch (type) {
      case "POST":
        return "게시글";
      case "COMMENT":
        return "댓글";
      case "ERROR":
        return "오류";
      case "IMPROVEMENT":
        return "개선사항";
      default:
        return "기타";
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "pending":
        return "bg-yellow-100 text-yellow-800";
      case "investigating":
        return "bg-blue-100 text-blue-800";
      case "resolved":
        return "bg-green-100 text-green-800";
      case "rejected":
        return "bg-red-100 text-red-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  if (!isAuthenticated || user?.role !== "ADMIN") {
    return null;
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <main className="container mx-auto px-4 py-8">
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">신고 정보를 불러오는 중...</p>
          </div>
        </main>
      </div>
    );
  }

  if (!report) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <main className="container mx-auto px-4 py-8">
          <div className="text-center py-8">
            <p className="text-gray-600">신고 정보를 찾을 수 없습니다.</p>
            <Link href="/admin">
              <Button className="mt-4">관리자 페이지로 돌아가기</Button>
            </Link>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <main className="container mx-auto px-4 py-8">
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div>
              <div className="flex items-center space-x-2">
                <AlertTriangle className="w-6 h-6 text-red-600" />
                <h1 className="text-xl font-bold text-gray-800">
                  신고 상세 정보
                </h1>
                <Badge className="bg-red-100 text-red-800">
                  #{report.reportId}
                </Badge>
              </div>
            </div>
            <Link href="/admin">
              <Button variant="outline">
                <ArrowLeft className="w-4 h-4 mr-2" />
                목록으로
              </Button>
            </Link>
          </div>
        </div>

        <div className="grid lg:grid-cols-3 gap-6">
          {/* 신고 정보 */}
          <div className="lg:col-span-2 space-y-6">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle>신고 정보</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-gray-700">
                      신고 유형
                    </label>
                    <p className="text-sm text-gray-900">
                      {getReportTypeLabel(report.reportType)}
                    </p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700">
                      대상 ID
                    </label>
                    <p className="text-sm text-gray-900">{report.targetId}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700">
                      신고자 ID
                    </label>
                    <p className="text-sm text-gray-900">{report.userId}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700">
                      신고 일시
                    </label>
                    <p className="text-sm text-gray-900">
                      {report.createdAt || "날짜 미상"}
                    </p>
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700">
                    신고 내용
                  </label>
                  <div className="mt-1 p-3 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-900">{report.content}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 처리 내역 */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle>처리 내역</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-gray-700">
                    처리 메모
                  </label>
                  <Textarea
                    placeholder="처리 내용과 사유를 기록하세요..."
                    value={adminNote}
                    onChange={(e) => setAdminNote(e.target.value)}
                    className="mt-1"
                  />
                </div>

                {actionTaken && (
                  <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
                    <p className="text-sm text-green-800">
                      조치가 완료되었습니다:{" "}
                      {actionTaken === "approve"
                        ? "승인됨"
                        : actionTaken === "reject"
                        ? "반려됨"
                        : "사용자 차단됨"}
                    </p>
                  </div>
                )}

                <div className="flex space-x-2">
                  <Button
                    onClick={() => handleAction("approve")}
                    className="bg-green-600 hover:bg-green-700"
                  >
                    <CheckCircle className="w-4 h-4 mr-2" />
                    승인
                  </Button>
                  <Button
                    onClick={() => handleAction("reject")}
                    variant="outline"
                    className="border-red-200 text-red-600 hover:bg-red-50"
                  >
                    <XCircle className="w-4 h-4 mr-2" />
                    반려
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 사이드바 */}
          <div className="space-y-6">
            {/* 상태 정보 */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="text-sm">처리 상태</CardTitle>
              </CardHeader>
              <CardContent>
                <Badge
                  className={`text-sm ${getStatusColor(
                    report.status || "pending"
                  )}`}
                >
                  {report.status === "pending"
                    ? "대기중"
                    : report.status === "investigating"
                    ? "조사중"
                    : report.status === "resolved"
                    ? "해결됨"
                    : "반려됨"}
                </Badge>
              </CardContent>
            </Card>

            {/* 간단한 작업 */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="text-sm">빠른 작업</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <Button
                  variant="outline"
                  className="w-full justify-start text-blue-600 border-blue-200 hover:bg-blue-50"
                >
                  <Flag className="w-4 h-4 mr-2" />
                  추가 조사 요청
                </Button>
                <Button
                  variant="outline"
                  className="w-full justify-start text-orange-600 border-orange-200 hover:bg-orange-50"
                >
                  <Calendar className="w-4 h-4 mr-2" />
                  처리 일정 연기
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
}

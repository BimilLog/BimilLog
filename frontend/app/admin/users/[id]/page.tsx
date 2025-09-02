"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  ArrowLeft,
  Ban,
  CheckCircle,
  Users,
  AlertTriangle,
} from "lucide-react";
import Link from "next/link";
import { adminApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";

interface UserDetail {
  userId: number;
  nickname: string;
  joinDate?: string;
  status: "active" | "blocked" | "reported";
}

export default function UserDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { user, isAuthenticated } = useAuth();
  const [actionTaken, setActionTaken] = useState("");
  const [userDetail, setUserDetail] = useState<UserDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [userId, setUserId] = useState<string | null>(null);

  // params 추출
  useEffect(() => {
    params.then((resolvedParams) => {
      setUserId(resolvedParams.id);
    });
  }, [params]);

  // 권한 확인
  useEffect(() => {
    if (!isAuthenticated || user?.role !== "ADMIN") {
      window.location.href = "/";
    }
  }, [isAuthenticated, user]);

  // 사용자 정보는 현재 API에서 제공하지 않으므로 기본 정보만 표시
  useEffect(() => {
    const loadUserDetail = () => {
      if (!userId) return;

      setIsLoading(true);
      // 실제 API 구현시 사용자 정보를 가져오는 로직 추가
      setUserDetail({
        userId: Number(userId),
        nickname: `사용자${userId}`,
        joinDate: "2024-01-01",
        status: "active",
      });
      setIsLoading(false);
    };

    if (userId) {
      loadUserDetail();
    }
  }, [userId]);

  const handleBanUser = async () => {
    if (!userId) return;

    try {
      const response = await adminApi.banUser({
        reportType: "USER",
        targetId: Number(userId),
        content: "관리자에 의한 사용자 차단"
      });
      if (response.success) {
        setActionTaken("banned");
        setUserDetail((prev) => (prev ? { ...prev, status: "blocked" } : null));
      }
    } catch (error) {
      console.error("Failed to ban user:", error);
    }
  };

  const getUserStatusColor = (status: string) => {
    switch (status) {
      case "active":
        return "bg-green-100 text-green-800";
      case "reported":
        return "bg-yellow-100 text-yellow-800";
      case "blocked":
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
            <p className="mt-4 text-gray-600">사용자 정보를 불러오는 중...</p>
          </div>
        </main>
      </div>
    );
  }

  if (!userDetail) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <main className="container mx-auto px-4 py-8">
          <div className="text-center py-8">
            <p className="text-gray-600">사용자 정보를 찾을 수 없습니다.</p>
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
                <Users className="w-6 h-6 text-blue-600" />
                <h1 className="text-xl font-bold text-gray-800">
                  사용자 상세 정보
                </h1>
                <Badge className="bg-blue-100 text-blue-800">
                  #{userDetail.userId}
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
          {/* 사용자 정보 */}
          <div className="lg:col-span-2 space-y-6">
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle>기본 정보</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center space-x-4 mb-6">
                  <Avatar className="w-16 h-16">
                    <AvatarFallback className="bg-gradient-to-r from-blue-500 to-purple-600 text-white text-lg">
                      {userDetail.nickname?.charAt(0) || "?"}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-800">
                      {userDetail.nickname}
                    </h3>
                    <Badge
                      className={`text-sm ${getUserStatusColor(
                        userDetail.status
                      )}`}
                    >
                      {userDetail.status === "active"
                        ? "활성"
                        : userDetail.status === "reported"
                        ? "신고됨"
                        : "차단됨"}
                    </Badge>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-gray-700">
                      사용자 ID
                    </label>
                    <p className="text-sm text-gray-900">{userDetail.userId}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-700">
                      가입일
                    </label>
                    <p className="text-sm text-gray-900">
                      {userDetail.joinDate || "정보 없음"}
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 관리자 조치 */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle>관리자 조치</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {actionTaken && (
                  <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
                    <p className="text-sm text-green-800">
                      조치가 완료되었습니다:{" "}
                      {actionTaken === "banned"
                        ? "사용자가 차단되었습니다"
                        : actionTaken}
                    </p>
                  </div>
                )}

                <div className="flex space-x-2">
                  {userDetail.status !== "blocked" ? (
                    <Button
                      onClick={handleBanUser}
                      variant="outline"
                      className="border-red-200 text-red-600 hover:bg-red-50"
                    >
                      <Ban className="w-4 h-4 mr-2" />
                      사용자 차단
                    </Button>
                  ) : (
                    <Button
                      variant="outline"
                      className="border-green-200 text-green-600 hover:bg-green-50"
                      disabled
                    >
                      <CheckCircle className="w-4 h-4 mr-2" />
                      이미 차단됨
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 사이드바 */}
          <div className="space-y-6">
            {/* 상태 정보 */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="text-sm">계정 상태</CardTitle>
              </CardHeader>
              <CardContent>
                <Badge
                  className={`text-sm ${getUserStatusColor(userDetail.status)}`}
                >
                  {userDetail.status === "active"
                    ? "정상 계정"
                    : userDetail.status === "reported"
                    ? "신고된 계정"
                    : "차단된 계정"}
                </Badge>
              </CardContent>
            </Card>

            {/* 안내 */}
            <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
              <CardHeader>
                <CardTitle className="text-sm flex items-center space-x-2">
                  <AlertTriangle className="w-4 h-4 text-amber-600" />
                  <span>안내사항</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-gray-600">
                  사용자의 상세한 활동 내역 및 통계는 추후 구현 예정입니다.
                  현재는 기본적인 차단 기능만 제공됩니다.
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
}

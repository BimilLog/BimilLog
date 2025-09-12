"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AlertTriangle, TrendingUp } from "lucide-react";
import { adminApi, type Report, type PageResponse } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";

// 분리된 컴포넌트들 import
import { AdminHeader } from "./components/AdminHeader";
import { ReportList } from "./components/ReportList";
import { AdminStats } from "./components/AdminStats";

export default function AdminPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuth();
  const [searchTerm, setSearchTerm] = useState("");
  const [filterType, setFilterType] = useState("all");
  const [reports, setReports] = useState<PageResponse<Report> | null>(null);
  const [isLoadingReports, setIsLoadingReports] = useState(true);

  // 권한 확인
  useEffect(() => {
    if (!isLoading && (!isAuthenticated || user?.role !== "ADMIN")) {
      router.push("/");
    }
  }, [isLoading, isAuthenticated, user, router]);

  // 신고 목록 조회 함수
  const fetchReports = useCallback(async () => {
    try {
      setIsLoadingReports(true);
      const reportType = filterType === "all" ? undefined : filterType;
      const response = await adminApi.getReports(0, 20, reportType);
      if (response.success && response.data) {
        setReports(response.data as PageResponse<Report>);
      }
    } catch (error) {
      console.error("Failed to fetch reports:", error);
    } finally {
      setIsLoadingReports(false);
    }
  }, [filterType]);

  // 신고 목록 조회
  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <img
            src="/log.png"
            alt="비밀로그"
            className="h-12 object-contain mx-auto mb-4 animate-pulse"
          />
          <p className="text-gray-600">로딩 중...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || user?.role !== "ADMIN") {
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      <main className="container mx-auto px-4 py-8">
        <AdminHeader />

        <Tabs defaultValue="reports" className="space-y-6">
          <TabsList className="grid w-full grid-cols-2 bg-white/80 backdrop-blur-sm">
            <TabsTrigger
              value="reports"
              className="flex items-center space-x-2"
            >
              <AlertTriangle className="w-4 h-4" />
              <span>신고 관리</span>
            </TabsTrigger>
            <TabsTrigger value="stats" className="flex items-center space-x-2">
              <TrendingUp className="w-4 h-4" />
              <span>통계</span>
            </TabsTrigger>
          </TabsList>

          {/* 신고 관리 탭 */}
          <TabsContent value="reports" className="space-y-6">
            <ReportList
              reports={reports}
              isLoading={isLoadingReports}
              searchTerm={searchTerm}
              setSearchTerm={setSearchTerm}
              filterType={filterType}
              setFilterType={setFilterType}
              onReportUpdated={fetchReports}
            />
          </TabsContent>

          {/* 통계 탭 */}
          <TabsContent value="stats" className="space-y-6">
            <AdminStats />
          </TabsContent>
        </Tabs>
      </main>

      <HomeFooter />
    </div>
  );
}

"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { 
  Tabs, 
  TabsContent, 
  TabsList, 
  TabsTrigger, 
  AlertTriangle, 
  TrendingUp,
  AuthHeader,
  HomeFooter,
  AdminHeader,
  ReportList,
  AdminStats 
} from "@/components";
import { adminApi, type Report, type PageResponse } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";

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
          <TabsList className="grid w-full grid-cols-2 bg-white/90 backdrop-blur-sm border-0 shadow-md rounded-lg">
            <TabsTrigger
              value="reports"
              className="flex items-center gap-3 min-h-[48px] text-sm font-medium data-[state=active]:bg-gradient-to-r data-[state=active]:from-pink-500 data-[state=active]:to-purple-600 data-[state=active]:text-white transition-all duration-200"
            >
              <AlertTriangle className="w-4 h-4" />
              <span>신고 관리</span>
            </TabsTrigger>
            <TabsTrigger 
              value="stats" 
              className="flex items-center gap-3 min-h-[48px] text-sm font-medium data-[state=active]:bg-gradient-to-r data-[state=active]:from-green-500 data-[state=active]:to-emerald-600 data-[state=active]:text-white transition-all duration-200"
            >
              <TrendingUp className="w-4 h-4" />
              <span>통계</span>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="reports" className="space-y-6 focus:outline-none">
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

          <TabsContent value="stats" className="space-y-6 focus:outline-none">
            <AdminStats />
          </TabsContent>
        </Tabs>
      </main>

      <HomeFooter />
    </div>
  );
}

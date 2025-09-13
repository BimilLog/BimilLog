"use client";

import { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { 
  Tabs, 
  TabsContent, 
  TabsList, 
  TabsTrigger
} from "@/components";
import { AlertTriangle, TrendingUp } from "lucide-react";
import { AdminStats, ReportListContainer } from "@/components/organisms/admin";
import { useAdminAuth, useReports } from "@/hooks/features/admin";

export function AdminClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isAdmin, isLoading: isAuthLoading } = useAdminAuth();
  
  const initialTab = searchParams.get("tab") || "reports";
  const initialFilterType = searchParams.get("filter") || "all";
  const initialSearchTerm = searchParams.get("search") || "";
  
  const [activeTab, setActiveTab] = useState(initialTab);
  
  const {
    reports,
    isLoading: isReportsLoading,
    error,
    refetch
  } = useReports();

  useEffect(() => {
    if (!isAuthLoading && !isAdmin) {
      router.push("/");
    }
  }, [isAdmin, isAuthLoading, router]);

  if (isAuthLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-purple-600"></div>
      </div>
    );
  }

  if (!isAdmin) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-white to-purple-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* 헤더 */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-purple-600 to-pink-600 rounded-lg flex items-center justify-center">
              <AlertTriangle className="w-6 h-6 text-white" />
            </div>
            관리자 대시보드
          </h1>
          <p className="mt-2 text-gray-600">
            신고 관리 및 통계를 확인할 수 있습니다
          </p>
        </div>

        {/* 통계 카드 */}
        <AdminStats reports={reports} />

        {/* 탭 컨텐츠 */}
        <div className="mt-8">
          <Tabs value={activeTab} onValueChange={setActiveTab}>
            <TabsList className="grid w-full max-w-md grid-cols-2">
              <TabsTrigger value="reports" className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4" />
                신고 관리
              </TabsTrigger>
              <TabsTrigger value="stats" className="flex items-center gap-2">
                <TrendingUp className="w-4 h-4" />
                상세 통계
              </TabsTrigger>
            </TabsList>

            <TabsContent value="reports" className="mt-6">
              <ReportListContainer
                reports={reports}
                isLoading={isReportsLoading}
                refetch={refetch}
                initialFilterType={initialFilterType}
                initialSearchTerm={initialSearchTerm}
              />
            </TabsContent>

            <TabsContent value="stats" className="mt-6">
              <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-8">
                <div className="text-center text-gray-500">
                  <TrendingUp className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                  <p>상세 통계 기능은 준비 중입니다</p>
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}
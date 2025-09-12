"use client";

import { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { 
  Tabs, 
  TabsContent, 
  TabsList, 
  TabsTrigger, 
  AlertTriangle, 
  TrendingUp 
} from "@/components";
import { AdminStats } from "@/components/organisms/admin/AdminStats";
import { ReportListContainer } from "./ReportListContainer";
import { useAdminAuth } from "../hooks/useAdminAuth";
import { useReports } from "../hooks/useReports";

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
    filterType,
    setFilterType,
    searchTerm,
    setSearchTerm,
    refetch,
  } = useReports({
    initialFilterType,
    initialSearchTerm,
  });

  useEffect(() => {
    const params = new URLSearchParams();
    params.set("tab", activeTab);
    if (filterType !== "all") params.set("filter", filterType);
    if (searchTerm) params.set("search", searchTerm);
    
    const newUrl = `${window.location.pathname}?${params.toString()}`;
    router.replace(newUrl, { scroll: false });
  }, [activeTab, filterType, searchTerm, router]);

  if (isAuthLoading) {
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

  if (!isAdmin) {
    return null;
  }

  return (
    <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
      <TabsList className="grid w-full grid-cols-2 bg-white/90 backdrop-blur-sm border-0 shadow-md rounded-lg">
        <TabsTrigger
          value="reports"
          className="flex items-center gap-3 min-h-[48px] text-sm font-medium data-[state=active]:bg-gradient-to-r data-[state=active]:from-pink-500 data-[state=active]:to-purple-600 data-[state=active]:text-white transition-all duration-200 touch-manipulation"
        >
          <AlertTriangle className="w-4 h-4" />
          <span>신고 관리</span>
        </TabsTrigger>
        <TabsTrigger
          value="stats"
          className="flex items-center gap-3 min-h-[48px] text-sm font-medium data-[state=active]:bg-gradient-to-r data-[state=active]:from-green-500 data-[state=active]:to-emerald-600 data-[state=active]:text-white transition-all duration-200 touch-manipulation"
        >
          <TrendingUp className="w-4 h-4" />
          <span>통계</span>
        </TabsTrigger>
      </TabsList>

      <TabsContent value="reports" className="space-y-6 focus:outline-none">
        <ReportListContainer
          reports={reports}
          isLoading={isReportsLoading}
          error={error}
          searchTerm={searchTerm}
          onSearchChange={setSearchTerm}
          filterType={filterType}
          onFilterChange={setFilterType}
          onReportUpdated={refetch}
        />
      </TabsContent>

      <TabsContent value="stats" className="space-y-6 focus:outline-none">
        <AdminStats />
      </TabsContent>
    </Tabs>
  );
}
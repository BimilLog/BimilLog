"use client";

import { useEffect, useMemo, useRef } from "react";
import dynamic from "next/dynamic";
import { useSearchParams, useRouter } from "next/navigation";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger
} from "@/components";
import { AlertTriangle, TrendingUp } from "lucide-react";
import { Spinner as FlowbiteSpinner } from "flowbite-react";
import { useAdminAuth, useReports } from "@/hooks/features/admin";
import { useToastStore } from "@/stores/toast.store";

// Dynamic imports for heavy admin components — paper 토큰 (F-16-002 / F-16-011)
const AdminStats = dynamic(
  () => import("@/components/organisms/admin").then((mod) => ({ default: mod.AdminStats })),
  {
    ssr: false,
    loading: () => (
      <div className="mb-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="bg-paper-card border border-postal-navy/15 rounded-xl p-6 shadow-brand-sm"
            >
              <div className="flex items-center justify-center h-20">
                <FlowbiteSpinner color="failure" size="xl" aria-label="통계 불러오는 중..." />
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }
);

const ReportListContainer = dynamic(
  () => import("@/components/organisms/admin").then((mod) => ({ default: mod.ReportListContainer })),
  {
    ssr: false,
    loading: () => (
      <div className="bg-paper-card border border-postal-navy/15 rounded-xl shadow-brand-sm p-6">
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="flex flex-col items-center gap-3">
            <FlowbiteSpinner color="failure" size="xl" aria-label="신고 목록 불러오는 중..." />
            <p className="text-sm text-ink-soft dark:text-muted-foreground">신고 목록을 불러오는 중...</p>
          </div>
        </div>
      </div>
    )
  }
);

const VALID_TABS = ["reports", "stats"] as const;
type AdminTab = (typeof VALID_TABS)[number];

const isValidTab = (tab: string | null): tab is AdminTab =>
  tab !== null && (VALID_TABS as readonly string[]).includes(tab);

export function AdminClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isAdmin, isLoading: isAuthLoading } = useAdminAuth();

  const tabParam = searchParams.get("tab");
  const filterParam = searchParams.get("filter");

  // 라운드 16 F-16-005: URL `?tab=` 가 SSOT — activeTab 은 URL 에서 파생
  const activeTab: AdminTab = useMemo(
    () => (isValidTab(tabParam) ? tabParam : "reports"),
    [tabParam],
  );

  const {
    reports,
    isLoading: isReportsLoading,
    error: reportsError,
    filterType,
    setFilterType,
    page,
    setPage,
    totalElements,
    totalPages,
    refetch,
    removeResolvedReports,
  } = useReports({
    initialFilterType: filterParam || "all",
  });

  // 한 번만 토스트가 뜨도록 가드 (StrictMode + dev 두 번 mount 방어)
  const redirectedRef = useRef(false);

  // 라운드 13 P2 (F-13-BUG-1) + 라운드 16 F-16-001:
  // useAdminAuth 의 router.push 제거로 race 차단. 여기 한 곳에서 토스트 + redirect.
  useEffect(() => {
    if (!isAuthLoading && !isAdmin && !redirectedRef.current) {
      redirectedRef.current = true;
      useToastStore.getState().showWarning(
        "관리자 전용 페이지예요",
        "이 페이지는 관리자만 접근할 수 있어요. 홈으로 이동했어요.",
      );
      router.push("/");
    }
  }, [isAdmin, isAuthLoading, router]);

  // 탭 변경 → URL 갱신 (F-16-005)
  const handleTabChange = (value: string) => {
    if (!isValidTab(value) || value === activeTab) return;
    router.replace(`/admin?tab=${value}`, { scroll: false });
  };

  if (isAuthLoading) {
    // 라운드 16 F-16-002: app/admin/loading.tsx 와 같은 paper 톤으로 일관 (3중 깜박임 완화)
    return (
      <div className="flex items-center justify-center min-h-[40vh]" role="status" aria-live="polite">
        <FlowbiteSpinner color="failure" size="xl" aria-label="권한을 확인하는 중..." />
      </div>
    );
  }

  if (!isAdmin) {
    return null;
  }

  return (
    <div className="bg-paper" role="region" aria-label="관리자 대시보드 콘텐츠">
      <div className="max-w-7xl mx-auto py-4">
        {/* 통계 카드 — 라운드 16 F-16-007/008/010 reports 활용 */}
        <AdminStats
          reports={reports}
          totalElements={totalElements}
        />

        {/* 탭 컨텐츠 — 라운드 16 F-16-005/006/009 URL SSOT + 카운트 뱃지 + sticky */}
        <div className="mt-8">
          <Tabs value={activeTab} onValueChange={handleTabChange}>
            <div className="sticky top-0 z-10 -mx-4 px-4 mb-2 bg-paper-50/95 dark:bg-paper-900/95 backdrop-blur-sm border-b border-postal-navy/20">
              <TabsList className="grid w-full max-w-md grid-cols-2">
                <TabsTrigger
                  value="reports"
                  className="flex items-center gap-2 min-h-[44px]"
                >
                  <AlertTriangle
                    className="w-4 h-4 stroke-stamp-red"
                    aria-hidden="true"
                  />
                  <span>신고 관리</span>
                  {totalElements > 0 && (
                    <span
                      className="inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full text-xs font-semibold bg-postal-navy/15 text-postal-navy dark:bg-postal-navy/30 dark:text-paper-50"
                      aria-label={`${totalElements}건`}
                    >
                      {totalElements}
                    </span>
                  )}
                </TabsTrigger>
                <TabsTrigger
                  value="stats"
                  className="flex items-center gap-2 min-h-[44px]"
                >
                  <TrendingUp
                    className="w-4 h-4 stroke-postal-navy"
                    aria-hidden="true"
                  />
                  <span>상세 통계</span>
                </TabsTrigger>
              </TabsList>
            </div>

            <TabsContent value="reports" className="mt-6">
              <ReportListContainer
                reports={reports}
                isLoading={isReportsLoading}
                error={reportsError}
                refetch={refetch}
                filterType={filterType}
                setFilterType={setFilterType}
                page={page}
                setPage={setPage}
                totalElements={totalElements}
                totalPages={totalPages}
                removeResolvedReports={removeResolvedReports}
              />
            </TabsContent>

            <TabsContent value="stats" className="mt-6">
              {/* 라운드 16 F-16-040: stats 탭 placeholder → EmptyView 형태 (paper 토큰) */}
              <div
                className="bg-paper-card border border-postal-navy/15 rounded-xl shadow-brand-sm p-8"
                role="status"
                aria-live="polite"
              >
                <div className="flex flex-col items-center text-center">
                  <div className="mb-4 w-16 h-16 bg-paper-aged border-2 border-dashed border-stamp-red/40 dark:bg-stamp-red/15 rounded-3xl flex items-center justify-center">
                    <TrendingUp
                      className="w-7 h-7 stroke-stamp-red"
                      strokeWidth={1.6}
                      aria-hidden="true"
                    />
                  </div>
                  <h3 className="font-display text-lg font-bold text-ink dark:text-foreground mb-2 break-keep">
                    상세 통계는 곧 도착해요
                  </h3>
                  <p className="font-body text-sm text-ink-soft dark:text-muted-foreground break-keep">
                    사용 추이, 신고 추세 등 자세한 통계를 준비 중이에요.
                  </p>
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}

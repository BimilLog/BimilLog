import React from "react";
import { Button } from "@/components";
import { ShieldAlert } from "lucide-react";
import Link from "next/link";

/**
 * 관리자 대시보드 헤더 — 라운드 16 F-16-003 / F-16-004:
 * - 페이지 H1 SSOT (AdminClient 의 H1 제거 → 중복 헤딩 해결).
 * - paper 토큰 일괄 (gradient 텍스트 / bg-white 제거 → ink + paper-card).
 */
export const AdminHeader: React.FC = React.memo(() => {
  return (
    <header className="mb-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <div
              className="w-10 h-10 rounded-lg bg-stamp-red/15 dark:bg-stamp-red/25 flex items-center justify-center shrink-0"
              aria-hidden="true"
            >
              <ShieldAlert className="w-6 h-6 stroke-stamp-red" />
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold font-display text-ink dark:text-foreground break-keep">
              관리자 대시보드
            </h1>
          </div>
          <p className="mt-2 text-sm text-ink-soft dark:text-muted-foreground break-keep">
            신고를 살펴보고, 필요한 조치를 안전하게 처리할 수 있어요.
          </p>
        </div>
        <Link href="/" className="flex-shrink-0">
          <Button
            variant="outline"
            className="min-h-[48px] px-6 border-postal-navy/30 text-postal-navy hover:bg-postal-navy/10 dark:text-foreground font-medium"
          >
            홈으로 돌아가기
          </Button>
        </Link>
      </div>
    </header>
  );
});

AdminHeader.displayName = "AdminHeader";

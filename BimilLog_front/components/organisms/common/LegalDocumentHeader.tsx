import React from "react";

interface LegalDocumentHeaderProps {
  title: string;
}

/**
 * 정책/약관 문서 상단 헤더 (라운드 1 paper 메타포 적용 완료).
 *
 * 라운드 14 정리:
 *  - deprecated `gradientClassName` prop 제거 (사용처 0건)
 *  - server component (`"use client"` 제거 — 인터랙션 없음)
 *  - React.memo 제거 (server component 에서는 무의미)
 */
export function LegalDocumentHeader({ title }: LegalDocumentHeaderProps) {
  return (
    <div className="relative bg-paper-aged border-b border-stamp-red/30 px-8 py-8 text-ink dark:text-foreground">
      <div className="flex items-center justify-center gap-3">
        <span aria-hidden className="hidden sm:inline-flex stamp-circle">
          공식
        </span>
        <h1 className="font-display text-2xl sm:text-3xl font-bold text-center text-ink dark:text-foreground break-keep">
          {title}
        </h1>
      </div>
    </div>
  );
}

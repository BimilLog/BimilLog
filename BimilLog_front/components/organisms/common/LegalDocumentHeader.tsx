"use client";

import React from "react";

interface LegalDocumentHeaderProps {
  title: string;
  /**
   * @deprecated 종이/편지 정체성 통합으로 더 이상 사용되지 않음.
   * 호환성을 위해 prop 은 유지하지만 내부에서 무시됨.
   */
  gradientClassName?: string;
}

export const LegalDocumentHeader = React.memo(function LegalDocumentHeader({
  title,
}: LegalDocumentHeaderProps) {
  return (
    <div className="relative bg-paper-aged border-b border-stamp-red/30 px-8 py-8 text-ink">
      <div className="flex items-center justify-center gap-3">
        <span aria-hidden className="hidden sm:inline-flex stamp-circle">
          공식
        </span>
        <h1 className="font-display text-2xl sm:text-3xl font-bold text-center text-ink">
          {title}
        </h1>
      </div>
    </div>
  );
});

LegalDocumentHeader.displayName = "LegalDocumentHeader";

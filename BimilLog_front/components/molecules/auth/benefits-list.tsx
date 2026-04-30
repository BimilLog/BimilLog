"use client";

import type { ReactNode } from "react";

export interface BenefitItem {
  /** 베네핏 텍스트 */
  text: string;
  /** 강조 키워드 (옵션) */
  emphasis?: string;
}

export interface BenefitsListProps {
  /** 베네핏 항목 */
  items: BenefitItem[];
  /** 마커 종류: 체크 아이콘(검사 완료 톤) 또는 dot(엽서 라벨 톤) */
  variant?: "check" | "dot";
  /** 추가 클래스 */
  className?: string;
  /** 리스트 위 보조 라벨 */
  label?: ReactNode;
}

/**
 * 인증/온보딩 페이지에서 사용하는 공용 베네핏 리스트.
 *
 * - login/signup 두 페이지가 같은 컴포넌트를 사용하도록 분리.
 * - 라이트/다크 토큰 일관 (`text-stamp-red`, `text-ink-soft dark:text-muted-foreground`).
 */
export function BenefitsList({
  items,
  variant = "check",
  className = "",
  label,
}: BenefitsListProps) {
  return (
    <div className={className}>
      {label && (
        <p className="mb-3 text-xs font-display tracking-[0.18em] uppercase text-stamp-red">
          {label}
        </p>
      )}
      <ul className="space-y-3">
        {items.map((item, index) => (
          <li
            key={`${item.text}-${index}`}
            className="flex items-start gap-3 text-sm sm:text-base font-body text-ink-soft dark:text-foreground/85 leading-relaxed break-keep"
          >
            {variant === "check" ? (
              <svg
                aria-hidden="true"
                className="mt-0.5 h-5 w-5 shrink-0 text-stamp-red dark:text-stamp-red"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                  clipRule="evenodd"
                />
              </svg>
            ) : (
              <span
                aria-hidden="true"
                className="mt-2 inline-block h-1.5 w-1.5 shrink-0 rounded-full bg-stamp-red"
              />
            )}
            <span className="text-pretty">
              {item.emphasis && (
                <span className="font-semibold text-foreground dark:text-foreground">
                  {item.emphasis}
                </span>
              )}
              {item.emphasis ? " " : ""}
              {item.text}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

BenefitsList.displayName = "BenefitsList";

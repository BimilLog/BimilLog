"use client";

import React from "react";
import Link from "next/link";
import { MailOpen, RefreshCw } from "lucide-react";
import { Button } from "@/components/atoms/actions/button";
import { cn } from "@/lib/utils";

interface EmptyViewAction {
  label: string;
  href?: string;
  onClick?: () => void;
}

interface EmptyViewProps {
  /** 헤딩 카피 (필수) */
  title: string;
  /** 보조 설명 */
  description?: React.ReactNode;
  /** 커스텀 일러스트 (없으면 기본 MailOpen) */
  icon?: React.ReactNode;
  /** primary 액션 (CTA) */
  action?: EmptyViewAction;
  /** 다시 시도 콜백 (에러 회복 등) */
  onRetry?: () => void;
  /** "다시 시도" 라벨 (기본: "다시 시도") */
  retryLabel?: string;
  /** 컨테이너 className 확장 */
  className?: string;
  /** padding 압축 (모달/사이드바 등 좁은 영역) */
  compact?: boolean;
  /** assertive 안내 (에러 회복 영역 등). 기본 false → polite */
  assertive?: boolean;
}

/**
 * EmptyView — paper-aged 종이 + RETURN dashed 보더 메타포의 표준 빈 상태/에러 컴포넌트.
 *
 * MessageListModal / PopularPapersSection / ReportListContainer 등이 사용.
 * 일러스트는 prop 으로 커스터마이즈 가능 (도메인 특화 SVG 보존).
 */
export function EmptyView({
  title,
  description,
  icon,
  action,
  onRetry,
  retryLabel = "다시 시도",
  className,
  compact = false,
  assertive = false,
}: EmptyViewProps) {
  const role = assertive ? "alert" : "status";
  const ariaLive = assertive ? "assertive" : "polite";

  return (
    <div
      role={role}
      aria-live={ariaLive}
      className={cn(
        "flex flex-col items-center justify-center text-center",
        compact ? "p-6" : "p-8 md:p-12 min-h-[260px]",
        className
      )}
    >
      <div className="mb-6">
        <div
          className={cn(
            "mx-auto mb-4 bg-paper-aged border-2 border-dashed border-stamp-red/40 dark:bg-stamp-red/15 rounded-3xl flex items-center justify-center shadow-brand-sm",
            compact ? "w-16 h-16" : "w-20 h-20"
          )}
        >
          <div className="text-stamp-red dark:text-paper-50">
            {icon ?? (
              <MailOpen
                className={cn(compact ? "w-7 h-7" : "w-9 h-9")}
                strokeWidth={1.6}
                aria-hidden="true"
              />
            )}
          </div>
        </div>

        {/* 데코 도트 — paper / postal / seal */}
        <div className="flex items-center justify-center space-x-1.5">
          <span
            className="w-1.5 h-1.5 bg-stamp-red/40 rounded-full"
            aria-hidden="true"
          />
          <span
            className="w-2 h-2 bg-postal-navy/30 rounded-full"
            aria-hidden="true"
          />
          <span
            className="w-1.5 h-1.5 bg-seal-gold rounded-full"
            aria-hidden="true"
          />
        </div>
      </div>

      <div className="max-w-md mx-auto mb-6">
        <h3 className="font-display text-lg md:text-xl font-bold text-ink dark:text-foreground mb-2 break-keep">
          {title}
        </h3>
        {description && (
          <p className="font-body text-sm md:text-base text-ink-soft dark:text-muted-foreground leading-relaxed break-keep">
            {description}
          </p>
        )}
      </div>

      {(action || onRetry) && (
        <div className="flex flex-col sm:flex-row gap-3 w-full max-w-sm">
          {onRetry && (
            <Button
              variant="outline"
              size="default"
              onClick={onRetry}
              className="flex items-center justify-center min-h-touch"
            >
              <RefreshCw className="w-4 h-4 mr-2" aria-hidden="true" />
              {retryLabel}
            </Button>
          )}
          {action && (
            <Button
              asChild={!!action.href}
              onClick={action.onClick}
              size="default"
              className="flex items-center justify-center min-h-touch bg-stamp-red hover:bg-stamp-red/90"
            >
              {action.href ? <Link href={action.href}>{action.label}</Link> : action.label}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

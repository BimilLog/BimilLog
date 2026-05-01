"use client";

import React from "react";
import Link from "next/link";
import { Heart, Home, Search, ArrowLeft } from "lucide-react";
import { Button } from "@/components/atoms/actions/button";
import { BackButton } from "@/components/atoms/actions/back-button";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";

interface NotFoundAction {
  label: string;
  href?: string;
  onClick?: () => void;
  icon?: React.ReactNode;
  variant?: "primary" | "outline" | "ghost";
}

interface NotFoundViewProps {
  /** 도장 우표 안에 보일 코드 (404, PRIVATE, MISSING 등) */
  stampCode?: string;
  /** RETURN 라벨 (기본: "반송 RETURN") */
  stampLabel?: string;
  /** 메인 헤딩 */
  title?: string;
  /** 보조 설명 (string | ReactNode) */
  description?: React.ReactNode;
  /** 추가 액션 (홈/뒤로 외) */
  extraActions?: NotFoundAction[];
  /** 뒤로가기 버튼 노출 여부 (기본 true) */
  showBackButton?: boolean;
  /** 홈 버튼 노출 여부 (기본 true) */
  showHomeButton?: boolean;
  /** AuthHeader / HomeFooter 노출 여부 (기본 true) */
  withChrome?: boolean;
  /** 하단 도움말 영역 노출 여부 (기본 true) */
  showHelpLinks?: boolean;
}

/**
 * NotFoundView — 라운드 8 RETURN 도장 메타포의 표준 컴포넌트.
 *
 * 글로벌 404 (`app/not-found.tsx`) + 도메인 인라인 not-found
 * (RollingPaperContainer 의 isError / 비공개 페이퍼 등) 가 모두 사용.
 *
 * SVG 색상은 토큰 (currentColor 기반) 으로 다크 모드 자동 대응.
 */
export function NotFoundView({
  stampCode = "404",
  stampLabel = "반송 RETURN",
  title = "주소가 잘못된 편지예요",
  description = (
    <>
      요청하신 페이지가 삭제되었거나 주소가 변경되었을 수 있어요.
      <br />
      다른 페이지를 둘러보시거나 홈으로 돌아가보세요.
    </>
  ),
  extraActions,
  showBackButton = true,
  showHomeButton = true,
  withChrome = true,
  showHelpLinks = true,
}: NotFoundViewProps) {
  const defaultExtraActions: NotFoundAction[] = [
    { label: "게시판", href: "/board", icon: <Search className="w-4 h-4 mr-2" />, variant: "outline" },
    {
      label: "편지함",
      href: "/visit",
      icon: <Heart className="w-4 h-4 mr-2 text-stamp-red" />,
      variant: "outline",
    },
  ];
  const actions = extraActions ?? defaultExtraActions;

  const content = (
    <div
      className="flex items-center justify-center p-4 py-16"
      role="status"
      aria-live="polite"
    >
      <div className="text-center max-w-md mx-auto">
        {/* 봉투 + RETURN 도장 SVG (currentColor 기반) */}
        <div className="mb-8">
          <div className="relative">
            <div className="w-32 h-32 mx-auto mb-6 flex items-center justify-center relative text-ink dark:text-foreground">
              <svg viewBox="0 0 120 120" className="w-full h-full" aria-hidden="true">
                {/* 봉투 — fill 은 paper, stroke 는 ink (currentColor) */}
                <g transform="rotate(-8 60 60)">
                  <rect
                    x="14"
                    y="36"
                    width="92"
                    height="60"
                    rx="3"
                    className="fill-paper-50 dark:fill-paper-card stroke-current"
                    strokeWidth="2"
                  />
                  <polyline
                    points="14,36 60,76 106,36"
                    fill="none"
                    className="stroke-current"
                    strokeWidth="2"
                  />
                  {/* 우표 자리 — stamp-red dashed */}
                  <rect
                    x="78"
                    y="42"
                    width="22"
                    height="22"
                    fill="none"
                    className="stroke-stamp-red"
                    strokeWidth="1.5"
                    strokeDasharray="2 3"
                  />
                  <text
                    x="89"
                    y="56"
                    textAnchor="middle"
                    fontSize="9"
                    fontFamily="serif"
                    className="fill-stamp-red"
                    fontWeight="bold"
                  >
                    {stampCode}
                  </text>
                </g>
                {/* RETURN 도장 */}
                <g transform="rotate(12 60 100)">
                  <rect
                    x="20"
                    y="92"
                    width="80"
                    height="14"
                    fill="none"
                    className="stroke-stamp-red"
                    strokeWidth="1"
                    opacity="0.6"
                  />
                  <text
                    x="60"
                    y="102"
                    textAnchor="middle"
                    fontSize="7"
                    fontFamily="serif"
                    className="fill-stamp-red"
                    fontWeight="bold"
                    letterSpacing="1"
                  >
                    {stampLabel}
                  </text>
                </g>
              </svg>
            </div>

            {/* 코드 — 도장처럼 살짝 기울어진 stamp-red */}
            <div className="font-display text-5xl sm:text-6xl md:text-7xl font-bold text-stamp-red mb-4 inline-block transform -rotate-3 tracking-tight break-keep">
              {stampCode}
            </div>

            {/* 데코 도트 */}
            <div
              className="absolute -top-4 -left-4 w-3 h-3 bg-stamp-red/30 rounded-full opacity-60 animate-bounce"
              style={{ animationDelay: "0s" }}
              aria-hidden="true"
            />
            <div
              className="absolute -bottom-4 left-8 w-2 h-2 bg-postal-navy/30 rounded-full opacity-60 animate-bounce"
              style={{ animationDelay: "1s" }}
              aria-hidden="true"
            />
          </div>
        </div>

        {/* 텍스트 영역 */}
        <div className="mb-8">
          <h1 className="font-display text-2xl md:text-3xl font-bold text-ink dark:text-foreground mb-4 break-keep">
            {title}
          </h1>
          <p className="font-body text-ink-soft dark:text-muted-foreground leading-relaxed break-keep">
            {description}
          </p>
        </div>

        {/* 액션 버튼들 */}
        <div className="space-y-3">
          {showHomeButton && (
            <Button asChild size="lg" className="w-full bg-stamp-red hover:bg-stamp-red/90">
              <Link href="/">
                <Home className="w-5 h-5 mr-2" aria-hidden="true" />
                홈으로 돌아가기
              </Link>
            </Button>
          )}

          {actions.length > 0 && (
            <div className={actions.length === 1 ? "" : "grid grid-cols-2 gap-3"}>
              {actions.map((action, idx) => {
                const variant = action.variant ?? "outline";
                if (action.href) {
                  return (
                    <Button
                      key={`${action.label}-${idx}`}
                      asChild
                      variant={variant === "primary" ? "default" : variant}
                      size="lg"
                      className={action.label.length > 6 ? "min-w-0" : ""}
                    >
                      <Link href={action.href} className="truncate">
                        {action.icon}
                        <span className="truncate">{action.label}</span>
                      </Link>
                    </Button>
                  );
                }
                return (
                  <Button
                    key={`${action.label}-${idx}`}
                    onClick={action.onClick}
                    variant={variant === "primary" ? "default" : variant}
                    size="lg"
                    className={action.label.length > 6 ? "min-w-0" : ""}
                  >
                    {action.icon}
                    <span className="truncate">{action.label}</span>
                  </Button>
                );
              })}
            </div>
          )}

          {showBackButton && (
            <BackButton variant="ghost" size="lg" className="w-full">
              <ArrowLeft className="w-4 h-4 mr-2" aria-hidden="true" />
              이전 페이지로
            </BackButton>
          )}
        </div>

        {/* 하단 도움말 링크 */}
        {showHelpLinks && (
          <div className="mt-12 pt-8 border-t border-ink-soft">
            <p className="text-sm font-body text-ink-soft mb-4">도움이 필요하신가요?</p>
            <div className="flex justify-center space-x-6 text-sm">
              <Link
                href="/suggest"
                className="text-postal-navy hover:text-stamp-red hover:underline underline-offset-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2 rounded-sm"
              >
                건의하기
              </Link>
              <Link
                href="/help"
                className="text-postal-navy hover:text-stamp-red hover:underline underline-offset-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2 rounded-sm"
              >
                도움말
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );

  if (!withChrome) {
    return <div className="min-h-screen bg-paper">{content}</div>;
  }

  return (
    <div className="min-h-screen bg-paper">
      <AuthHeader />
      {content}
      <HomeFooter />
    </div>
  );
}

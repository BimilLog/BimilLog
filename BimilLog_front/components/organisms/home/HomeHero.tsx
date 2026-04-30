"use client";

import Link from "next/link";
import { Button } from "flowbite-react";
import { UserCheck } from "lucide-react";
import { KakaoShareButton } from "@/components";

import { SocialProvider } from "@/types/domains/auth";

interface HomeHeroProps {
  isAuthenticated: boolean;
  provider: SocialProvider | null;
  onOpenFriendsModal: () => void;
}

const PRIMARY_CTA_CLASS =
  "inline-flex items-center justify-center min-h-touch px-6 py-3 rounded-md text-base font-semibold text-white bg-paper-button hover:bg-paper-hover transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2 w-full sm:w-auto shadow-brand-sm";

const SECONDARY_CTA_CLASS =
  "inline-flex items-center justify-center min-h-touch px-6 py-3 rounded-md text-base font-semibold border border-ink bg-transparent text-ink dark:border-stamp-red dark:text-stamp-red dark:bg-transparent hover:bg-paper-soft dark:hover:bg-stamp-red/10 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2 w-full sm:w-auto";

export const HomeHero: React.FC<HomeHeroProps> = ({
  isAuthenticated,
  provider,
  onOpenFriendsModal,
}) => {
  return (
    <div className="text-center md:text-left relative">
      <div className="max-w-4xl mx-auto md:mx-0">
        {/* 우표 stamp accent */}
        <div className="hidden md:flex items-center gap-2 mb-4 text-xs font-display tracking-[0.18em] text-stamp-red uppercase">
          <span className="inline-block w-6 h-px bg-stamp-red" aria-hidden="true" />
          <span>From Bimillog · 익명 편지</span>
        </div>
        <h1 className="font-display text-4xl md:text-6xl font-bold mb-4 leading-tight text-ink dark:text-gray-100">
          익명으로 <span className="text-stamp-red">편지</span>를
          <br className="hidden md:block" />{" "}남겨보세요
        </h1>
        <p className="font-body text-base md:text-lg text-ink-soft dark:text-brand-secondary mb-6 leading-relaxed max-w-prose md:max-w-md">
          비밀로그에서 친구들에게 한 줄의 따뜻한 마음을 전해보세요. 익명이지만 진심을 담은 메시지가 누군가에게는 큰 힘이 됩니다.
        </p>

        <div className="flex flex-col gap-4 justify-center md:justify-start items-center md:items-start min-h-[160px]">
          {/* 비로그인 상태: primary CTA(만들기) + secondary CTA(둘러보기) */}
          {!isAuthenticated && (
            <div className="flex flex-col sm:flex-row gap-3 justify-center items-center w-full sm:w-auto">
              <Link
                href="/login"
                prefetch
                data-testid="home-hero-cta-primary"
                data-variant="primary"
                className={PRIMARY_CTA_CLASS}
              >
                롤링페이퍼 만들기
              </Link>
              <Link
                href="/visit"
                prefetch
                data-testid="home-hero-cta-secondary"
                data-variant="outline"
                className={SECONDARY_CTA_CLASS}
              >
                롤링페이퍼 둘러보기
              </Link>
            </div>
          )}

          {/* 로그인 상태 */}
          {isAuthenticated && (
            <>
              <div className="flex flex-col sm:flex-row gap-3 justify-center items-center w-full sm:w-auto">
                <Link
                  href="/rolling-paper"
                  prefetch
                  data-testid="home-hero-cta-primary"
                  data-variant="primary"
                  className={PRIMARY_CTA_CLASS}
                >
                  내 롤링페이퍼 보기
                </Link>
                <Link
                  href="/visit"
                  prefetch
                  data-testid="home-hero-cta-secondary"
                  data-variant="outline"
                  className={SECONDARY_CTA_CLASS}
                >
                  롤링페이퍼 둘러보기
                </Link>
              </div>

              {/* 카카오 친구 확인하기 - 카카오 사용자만 (secondary 위계) */}
              {provider === 'KAKAO' && (
                <Button
                  size="lg"
                  color="light"
                  onClick={onOpenFriendsModal}
                  className="border border-border text-foreground hover:bg-accent focus-visible:ring-2 focus-visible:ring-purple-400 focus-visible:ring-offset-2 min-h-touch"
                >
                  <UserCheck className="w-5 h-5 mr-2" />
                  카카오 친구 확인하기
                </Button>
              )}

              {/* 카카오톡 공유 - 로그인 시에만 노출 (비로그인은 인지부하 줄이기) */}
              <KakaoShareButton
                type="service"
                size="lg"
                className="px-8 py-3 text-lg font-semibold min-h-touch"
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
};

HomeHero.displayName = "HomeHero";

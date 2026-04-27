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
  "inline-flex items-center justify-center min-h-touch px-6 py-3 rounded-lg text-base font-semibold text-white bg-brand-button hover:bg-brand-hover transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-purple-400 focus-visible:ring-offset-2 w-full sm:w-auto";

const SECONDARY_CTA_CLASS =
  "inline-flex items-center justify-center min-h-touch px-6 py-3 rounded-lg text-base font-semibold border border-border bg-background text-foreground hover:bg-accent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-purple-400 focus-visible:ring-offset-2 w-full sm:w-auto";

export const HomeHero: React.FC<HomeHeroProps> = ({
  isAuthenticated,
  provider,
  onOpenFriendsModal,
}) => {
  return (
    <div className="text-center">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl md:text-5xl font-bold mb-4 bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 dark:from-pink-400 dark:via-purple-400 dark:to-indigo-400 bg-clip-text text-transparent">
          익명으로 메시지를 남겨보세요
        </h1>
        <p className="text-lg md:text-xl text-brand-secondary mb-6 leading-relaxed">
          비밀로그에서 친구들에게 익명으로 따뜻한 메시지를 남겨보세요
        </p>

        <div className="flex flex-col gap-4 justify-center items-center min-h-[160px]">
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

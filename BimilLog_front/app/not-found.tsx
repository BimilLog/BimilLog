import { Metadata } from "next";
import Link from "next/link";
import Image from "next/image";
import { Button } from "@/components";
import { Heart, Home, Search, ArrowLeft } from "lucide-react";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";
import { BackButton } from "@/components/atoms/actions/back-button";

export const metadata: Metadata = {
  title: "404 - 페이지를 찾을 수 없습니다",
  description: "요청하신 페이지가 삭제되었거나 주소가 변경되었을 수 있어요. 비밀로그 홈으로 돌아가보세요.",
  robots: {
    index: false,
    follow: true,
  },
  openGraph: {
    title: "404 - 페이지를 찾을 수 없습니다 | 비밀로그",
    description: "요청하신 페이지가 삭제되었거나 주소가 변경되었을 수 있어요.",
    siteName: "비밀로그",
    locale: "ko_KR",
    type: "website",
  },
};

export default function NotFoundPage() {
  return (
    <div className="min-h-screen bg-paper">
      <AuthHeader />

      <div className="flex items-center justify-center p-4 py-16">
        <div className="text-center max-w-md mx-auto">
          {/* 잘못 배달된 편지 일러스트 */}
          <div className="mb-8">
            <div className="relative">
              {/* 봉투 SVG 일러스트 (회전 + dashed 우표) */}
              <div className="w-32 h-32 mx-auto mb-6 flex items-center justify-center relative">
                <svg viewBox="0 0 120 120" className="w-full h-full" aria-hidden="true">
                  {/* 봉투 */}
                  <g transform="rotate(-8 60 60)">
                    <rect x="14" y="36" width="92" height="60" rx="3" fill="#FFFDF7" stroke="#2A1F1A" strokeWidth="2" />
                    <polyline points="14,36 60,76 106,36" fill="none" stroke="#2A1F1A" strokeWidth="2" />
                    {/* 우표 자리 (RETURN) */}
                    <rect x="78" y="42" width="22" height="22" fill="none" stroke="#C73E3E" strokeWidth="1.5" strokeDasharray="2 3" />
                    <text x="89" y="56" textAnchor="middle" fontSize="9" fontFamily="serif" fill="#C73E3E" fontWeight="bold">404</text>
                  </g>
                  {/* RETURN TO SENDER 도장 */}
                  <g transform="rotate(12 60 100)">
                    <rect x="20" y="92" width="80" height="14" fill="none" stroke="#C73E3E" strokeWidth="1" opacity="0.6" />
                    <text x="60" y="102" textAnchor="middle" fontSize="7" fontFamily="serif" fill="#C73E3E" fontWeight="bold" letterSpacing="1">반송 RETURN</text>
                  </g>
                </svg>
              </div>

              {/* 404 숫자 — 도장처럼 살짝 기울어진 */}
              <div className="font-display text-6xl md:text-7xl font-bold text-stamp-red mb-4 inline-block transform -rotate-3 tracking-tight">
                404
              </div>

              {/* 종이 데코 — 매우 절제 */}
              <div
                className="absolute -top-4 -left-4 w-3 h-3 bg-stamp-red/30 rounded-full opacity-60 animate-bounce"
                style={{ animationDelay: "0s" }}
              ></div>
              <div
                className="absolute -bottom-4 left-8 w-2 h-2 bg-postal-navy/30 rounded-full opacity-60 animate-bounce"
                style={{ animationDelay: "1s" }}
              ></div>
            </div>
            {/* 사용하지 않게 된 image — 숨김 처리 (SEO 깨지 않도록 유지) */}
            <div className="sr-only">
              <Image
                src="/log.png"
                alt="비밀로그"
                width={96}
                height={96}
                priority
                placeholder="blur"
                blurDataURL="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iOTYiIGhlaWdodD0iOTYiIHZpZXdCb3g9IjAgMCA5NiA5NiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9Ijk2IiBoZWlnaHQ9Ijk2IiBmaWxsPSIjRjNGNEY2Ii8+Cjwvc3ZnPgo="
              />
            </div>
          </div>

          {/* 텍스트 영역 */}
          <div className="mb-8">
            <h1 className="font-display text-2xl md:text-3xl font-bold text-ink dark:text-foreground mb-4">
              주소가 잘못된 편지예요
            </h1>
            <p className="font-body text-ink-soft dark:text-muted-foreground leading-relaxed">
              요청하신 페이지가 삭제되었거나 주소가 변경되었을 수 있어요.
              <br />
              다른 페이지를 둘러보시거나 홈으로 돌아가보세요.
            </p>
          </div>

          {/* 액션 버튼들 */}
          <div className="space-y-3">
            <Button asChild size="lg" className="w-full bg-stamp-red hover:bg-stamp-red/90">
              <Link href="/">
                <Home className="w-5 h-5 mr-2" />
                홈으로 돌아가기
              </Link>
            </Button>

            <div className="grid grid-cols-2 gap-3">
              <Button asChild variant="outline" size="lg">
                <Link href="/board">
                  <Search className="w-4 h-4 mr-2" />
                  게시판
                </Link>
              </Button>

              <Button asChild variant="outline" size="lg">
                <Link href="/visit">
                  <Heart className="w-4 h-4 mr-2 text-stamp-red" />
                  롤링페이퍼
                </Link>
              </Button>
            </div>

            <BackButton
              variant="ghost"
              size="lg"
              className="w-full"
            >
              <ArrowLeft className="w-4 h-4 mr-2" />
              이전 페이지로
            </BackButton>
          </div>

          {/* 하단 링크들 */}
          <div className="mt-12 pt-8 border-t border-ink-soft">
            <p className="text-sm font-body text-ink-soft mb-4">도움이 필요하신가요?</p>
            <div className="flex justify-center space-x-6 text-sm">
              <Link
                href="/suggest"
                className="text-postal-navy hover:text-stamp-red hover:underline underline-offset-2 transition-colors"
              >
                건의하기
              </Link>
              <Link
                href="/help"
                className="text-postal-navy hover:text-stamp-red hover:underline underline-offset-2 transition-colors"
              >
                도움말
              </Link>
            </div>
          </div>
        </div>
      </div>

      <HomeFooter />
    </div>
  );
}

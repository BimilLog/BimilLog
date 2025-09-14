"use client";

import { Button } from "@/components";
import { UserCheck } from "lucide-react";
import Link from "next/link";
import { KakaoShareButton } from "@/components";

interface HomeHeroProps {
  isAuthenticated: boolean;
  onOpenFriendsModal: () => void;
}

export const HomeHero: React.FC<HomeHeroProps> = ({
  isAuthenticated,
  onOpenFriendsModal,
}) => {
  return (
    <section className="container mx-auto px-4 py-8 md:py-12 text-center">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl md:text-5xl font-bold mb-4 bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent">
          익명으로 마음을 전해보세요
        </h1>
        <p className="text-lg md:text-xl text-brand-secondary mb-6 leading-relaxed">
          비밀로그에서 소중한 사람에게 익명의 따뜻한 메시지를 남겨보세요
        </p>

        <div className="flex flex-col gap-4 justify-center items-center">
          {/* 비로그인 상태 */}
          {!isAuthenticated && (
            <Button
              variant="default"
              size="lg"
              asChild
            >
              <Link href="/login">내 롤링페이퍼 만들기</Link>
            </Button>
          )}

          {/* 로그인 상태 */}
          {isAuthenticated && (
            <>
              {/* 모바일: 카카오 친구 확인하기 */}
              <div className="sm:hidden">
                <Button
                  variant="outline"
                  size="lg"
                  onClick={onOpenFriendsModal}
                >
                  <UserCheck className="w-5 h-5 mr-2" />
                  카카오 친구 확인하기
                </Button>
              </div>

              {/* PC: 카카오 친구 확인하기와 다른 롤링페이퍼 방문하기를 한 줄로 */}
              <div className="hidden sm:flex flex-row gap-4 justify-center items-center">
                <Button
                  variant="outline"
                  size="lg"
                  onClick={onOpenFriendsModal}
                >
                  <UserCheck className="w-5 h-5 mr-2" />
                  카카오 친구 확인하기
                </Button>
                <Button
                  variant="outline"
                  size="lg"
                  asChild
                >
                  <Link href="/visit">다른 롤링페이퍼 방문하기</Link>
                </Button>
              </div>

              {/* PC: 카카오톡 공유 버튼을 한 칸 아래에 */}
              <div className="hidden sm:block">
                <KakaoShareButton
                  type="service"
                  variant="outline"
                  size="lg"
                  className="px-8 py-3 text-lg font-semibold border-purple-200 text-purple-600 hover:bg-purple-50"
                />
              </div>
            </>
          )}

          {/* 모바일 또는 비로그인: 다른 롤링페이퍼 방문하기 */}
          <div className={isAuthenticated ? "sm:hidden" : ""}>
            <Button
              variant="outline"
              size="lg"
              asChild
            >
              <Link href="/visit">다른 롤링페이퍼 방문하기</Link>
            </Button>
          </div>

          {/* 모바일: 카카오톡 공유 버튼을 맨 아래에 */}
          {isAuthenticated && (
            <div className="sm:hidden">
              <KakaoShareButton
                type="service"
                variant="outline"
                size="lg"
                className="px-8 py-3 text-lg font-semibold border-purple-200 text-purple-600 hover:bg-purple-50"
              />
            </div>
          )}
        </div>
      </div>
    </section>
  );
};

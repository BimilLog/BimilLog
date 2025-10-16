"use client";

import { Button } from "flowbite-react";
import { UserCheck } from "lucide-react";
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
    <div className="text-center">
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
              color="purple"
              size="lg"
              onClick={() => window.location.href = '/login'}
            >
              내 롤링페이퍼 만들기
            </Button>
          )}

          {/* 로그인 상태 */}
          {isAuthenticated && (
            <>
              {/* 모바일: 카카오 친구 확인하기 */}
              <div className="sm:hidden">
                <Button
                  size="lg"
                  onClick={onOpenFriendsModal}
                  className="bg-gradient-to-r from-purple-500 to-pink-500 text-white hover:bg-gradient-to-l focus:ring-purple-200 dark:focus:ring-purple-800"
                >
                  <UserCheck className="w-5 h-5 mr-2 stroke-slate-600 fill-slate-100" />
                  카카오 친구 확인하기
                </Button>
              </div>

              {/* PC: 카카오 친구 확인하기와 다른 롤링페이퍼 방문하기를 한 줄로 */}
              <div className="hidden sm:flex flex-row gap-4 justify-center items-center">
                <Button
                  size="lg"
                  onClick={onOpenFriendsModal}
                  className="bg-gradient-to-r from-purple-500 to-pink-500 text-white hover:bg-gradient-to-l focus:ring-purple-200 dark:focus:ring-purple-800"
                >
                  <UserCheck className="w-5 h-5 mr-2 stroke-slate-600 fill-slate-100" />
                  카카오 친구 확인하기
                </Button>
                <Button
                  size="lg"
                  onClick={() => window.location.href = '/visit'}
                  className="bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:bg-gradient-to-bl focus:ring-cyan-300 dark:focus:ring-cyan-800"
                >
                  다른 롤링페이퍼 방문하기
                </Button>
              </div>

              {/* PC: 카카오톡 공유 버튼을 한 칸 아래에 */}
              <div className="hidden sm:block">
                <KakaoShareButton
                  type="service"
                  size="lg"
                  className="px-8 py-3 text-lg font-semibold"
                />
              </div>
            </>
          )}

          {/* 모바일 또는 비로그인: 다른 롤링페이퍼 방문하기 */}
          <div className={isAuthenticated ? "sm:hidden" : ""}>
            <Button
              size="lg"
              onClick={() => window.location.href = '/visit'}
              className="bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:bg-gradient-to-bl focus:ring-cyan-300 dark:focus:ring-cyan-800"
            >
              다른 롤링페이퍼 방문하기
            </Button>
          </div>

          {/* 모바일: 카카오톡 공유 버튼을 맨 아래에 */}
          {isAuthenticated && (
            <div className="sm:hidden">
              <KakaoShareButton
                type="service"
                size="lg"
                className="px-8 py-3 text-lg font-semibold"
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

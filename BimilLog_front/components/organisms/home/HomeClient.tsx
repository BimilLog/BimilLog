"use client";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { useAuth } from "@/hooks";
import { logger, isMobileOrTablet } from '@/lib/utils';
import { LazyKakaoFriendsModal } from "@/lib/utils/lazy-components";
import { NotificationPermissionModal } from "@/components/organisms/notification";

// 분리된 컴포넌트들 import - 직접 파일에서 import하여 circular dependency 방지
import { HomeHero } from "./HomeHero";
import { HomeFeatures } from "./HomeFeatures";
import { PopularPapersSection } from "./PopularPapersSection";

export default function HomeClient() {
  const { isAuthenticated, user } = useAuth();
  const [isFriendsModalOpen, setIsFriendsModalOpen] = useState(false);
  const [isNotificationModalOpen, setIsNotificationModalOpen] = useState(false);
  const router = useRouter();
  const searchParams = useSearchParams();

  // 프로토콜 URL 처리
  useEffect(() => {
    const url = searchParams.get("url");
    if (url) {
      try {
        const decodedUrl = decodeURIComponent(url);
        const path = decodedUrl.replace(/^web\+bimillog:\/\//, "");
        if (path) {
          router.replace(`/${path}`);
        }
      } catch (error) {
        logger.error("Failed to parse protocol URL:", error);
      }
    }
  }, [searchParams, router]);

  // 알림 권한 요청 모달 표시 로직
  useEffect(() => {
    // 로그인되어 있고, 모바일/태블릿이고, 스킵하지 않았을 때만 표시
    if (isAuthenticated && user && isMobileOrTablet()) {
      const skipUntil = localStorage.getItem("notification_permission_skipped");
      const shouldShow = !skipUntil || Date.now() > parseInt(skipUntil);

      if (shouldShow) {
        // 1초 후에 모달 표시 (페이지 로드 직후 바로 뜨는 것 방지)
        const timer = setTimeout(() => {
          setIsNotificationModalOpen(true);
        }, 1000);
        return () => clearTimeout(timer);
      }
    }
  }, [isAuthenticated, user]);

  const handleOpenFriendsModal = () => {
    if (!isAuthenticated) return;
    setIsFriendsModalOpen(true);
  };

  const handleNotificationSuccess = async (token: string) => {
    logger.log("FCM 토큰 획득 성공:", token.substring(0, 20) + "...");

    // FCM 토큰을 localStorage에 저장하여 다음 로그인 시 사용
    localStorage.setItem("fcm_token", token);
    // 스킵 기록 제거 (권한 허용했으므로 다시 물어보지 않음)
    localStorage.removeItem("notification_permission_skipped");

    // 참고: 현재 백엔드에는 FCM 토큰 등록 전용 API가 없습니다.
    // 토큰은 다음 로그인 시 auth/login API에 함께 전달됩니다.
    // 향후 실시간 토큰 등록을 위해서는 백엔드에 FCM 토큰 등록 API를 추가해야 합니다.
  };

  const handleNotificationSkip = () => {
    // 7일 후 다시 표시
    const skipUntil = Date.now() + 7 * 24 * 60 * 60 * 1000;
    localStorage.setItem("notification_permission_skipped", skipUntil.toString());
  };

  return (
    <MainLayout className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      {/* Hero Section with Popular Papers */}
      <div className="container mx-auto px-4 py-8 md:py-12">
 <div className="flex flex-col lg:flex-row gap-6 lg:gap-8">
           {/* Hero Section */}
         <div className="flex-1">
            <HomeHero
              isAuthenticated={isAuthenticated}
              onOpenFriendsModal={handleOpenFriendsModal}
            />
          </div>

          {/* Popular Papers Section - Hidden on mobile */}
          <div className="hidden lg:block">
            <PopularPapersSection />
          </div>
        </div>

        {/* Popular Papers Section - Visible on mobile only */}
        <div className="lg:hidden mt-8">
          <PopularPapersSection />
        </div>
      </div>

      {/* Features Section */}
      <HomeFeatures />

      {/* 카카오 친구 모달 */}
      <LazyKakaoFriendsModal
        isOpen={isFriendsModalOpen}
        onClose={() => setIsFriendsModalOpen(false)}
      />

      {/* 알림 권한 요청 모달 */}
      <NotificationPermissionModal
        show={isNotificationModalOpen}
        onClose={() => setIsNotificationModalOpen(false)}
        onSuccess={handleNotificationSuccess}
        onSkip={handleNotificationSkip}
      />
    </MainLayout>
  );
}

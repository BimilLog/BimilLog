"use client";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { useAuth } from "@/hooks";
import { useAuthStore } from "@/stores/auth.store";
import { notificationCommand } from "@/lib/api";
import { logger, isMobileOrTablet, isKakaoInAppBrowser } from '@/lib/utils';
import { LazyKakaoFriendsModal } from "@/lib/utils/lazy-components";
import { NotificationPermissionModal } from "@/components/organisms/notification";

// 분리된 컴포넌트들 import - 직접 파일에서 import하여 circular dependency 방지
import { HomeHero } from "./HomeHero";
import { HomeFeatures } from "./HomeFeatures";
import { PopularPapersSection } from "./PopularPapersSection";

export default function HomeClient() {
  const { isAuthenticated, user } = useAuth();
  const provider = useAuthStore((state) => state.provider);
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
    if (isAuthenticated && user && isMobileOrTablet() && !isKakaoInAppBrowser()) {
      // ✅ FIX: 이미 권한이 허용/거부된 경우 모달 표시 안 함
      if (typeof window !== 'undefined' && 'Notification' in window) {
        const currentPermission = Notification.permission;

        // 이미 허용됨 → 모달 표시 안 함
        if (currentPermission === 'granted') {
          return;
        }

        // 명시적으로 거부됨 → 모달 표시 안 함
        if (currentPermission === 'denied') {
          return;
        }
      }

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

  const handleNotificationSuccess = (token: string) => {
    logger.log("FCM 토큰 획득 성공:", token.substring(0, 20) + "...");

    localStorage.setItem("fcm_token", token);
    localStorage.removeItem("notification_permission_skipped");

    if (isAuthenticated) {
      notificationCommand.registerFcmToken(token).then(result => {
        if (!result.success) {
          logger.warn("FCM 토큰 서버 등록 실패:", result.error);
        }
      }).catch(error => {
        logger.error("FCM 토큰 서버 등록 중 오류:", error);
      });
    }
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
              provider={provider}
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

      {/* 카카오 친구 모달 - 카카오 로그인 사용자만 */}
      {provider === 'KAKAO' && (
        <LazyKakaoFriendsModal
          isOpen={isFriendsModalOpen}
          onClose={() => setIsFriendsModalOpen(false)}
        />
      )}

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

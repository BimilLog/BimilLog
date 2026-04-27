"use client";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { useAuth } from "@/hooks";
import { useAuthStore } from "@/stores/auth.store";
import { logger, isMobileOrTablet, isKakaoInAppBrowser } from '@/lib/utils';
import { LazyKakaoFriendsModal } from "@/lib/utils/lazy-components";
import { NotificationPermissionModal } from "@/components/organisms/notification";
import { registerFcmTokenAction } from "@/lib/actions/notification";
import type { CursorPageResponse } from "@/types/common";
import type { PopularPaperInfo } from "@/types/domains/paper";

// 분리된 컴포넌트들 import - 직접 파일에서 import하여 circular dependency 방지
import { HomeHero } from "./HomeHero";
import { HomeFeatures } from "./HomeFeatures";
import { PopularPapersSection } from "./PopularPapersSection";

interface HomeClientProps {
  popularPapers: CursorPageResponse<PopularPaperInfo> | null;
}

export default function HomeClient({ popularPapers }: HomeClientProps) {
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
  // - 첫 진입(첫 방문)에서는 노출하지 않고, 두 번째 세션 진입 또는 1시간 경과 시에만 노출
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
      if (!shouldShow) return;

      // 첫 방문 시각을 기록하고, 두 번째 세션 또는 1시간 이상 경과한 경우에만 노출
      const FIRST_VISIT_KEY = "notification_first_visit_at";
      const firstVisitRaw = localStorage.getItem(FIRST_VISIT_KEY);
      if (!firstVisitRaw) {
        // 첫 진입: 시간만 기록하고 모달은 절대 띄우지 않음
        localStorage.setItem(FIRST_VISIT_KEY, Date.now().toString());
        return;
      }

      const firstVisit = parseInt(firstVisitRaw, 10);
      const ONE_HOUR_MS = 60 * 60 * 1000;
      if (Number.isNaN(firstVisit) || Date.now() - firstVisit < ONE_HOUR_MS) {
        // 첫 방문 후 1시간 이내라면 노출 보류
        return;
      }

      // 두 번째 세션(또는 1시간 이상 경과) → 1.5초 뒤에 모달 표시
      const timer = setTimeout(() => {
        setIsNotificationModalOpen(true);
      }, 1500);
      return () => clearTimeout(timer);
    }
  }, [isAuthenticated, user]);

  const handleOpenFriendsModal = () => {
    if (!isAuthenticated) return;
    setIsFriendsModalOpen(true);
  };

  const handleNotificationSuccess = async (token: string) => {
    logger.log("FCM 토큰 획득 성공:", token.substring(0, 20) + "...");

    localStorage.setItem("fcm_token", token);
    localStorage.removeItem("notification_permission_skipped");

    if (isAuthenticated) {
      try {
        const result = await registerFcmTokenAction(token);
        if (!result.success) {
          logger.warn("FCM 토큰 서버 등록 실패:", result.error);
        }
      } catch (error) {
        logger.error("FCM 토큰 서버 등록 중 오류:", error);
      }
    }
  };

  const handleNotificationSkip = () => {
    // 7일 후 다시 표시
    const skipUntil = Date.now() + 7 * 24 * 60 * 60 * 1000;
    localStorage.setItem("notification_permission_skipped", skipUntil.toString());
  };

  return (
    <MainLayout className="bg-brand-gradient">
      {/* Hero Section with Popular Papers */}
      <div className="container mx-auto px-4 py-8 md:py-12">
        <div className="flex flex-col lg:flex-row gap-6 lg:gap-8">
          {/* Hero Section - 최소 높이 고정으로 CLS 방지 */}
          <div className="flex-1 min-h-[280px] md:min-h-[320px]">
            <HomeHero
              isAuthenticated={isAuthenticated}
              provider={provider}
              onOpenFriendsModal={handleOpenFriendsModal}
            />
          </div>

          <PopularPapersSection initialData={popularPapers} />
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

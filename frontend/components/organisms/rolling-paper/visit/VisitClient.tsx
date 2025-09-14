"use client";

import { useState } from "react";
import dynamic from "next/dynamic";
import { Heart, Mail } from "lucide-react";
import { useRouter } from "next/navigation";
import { useRollingPaperSearch } from "@/hooks";
import { KakaoShareButton } from "@/components/atoms/actions/kakao-share-button";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";
import { SearchSection } from "./SearchSection";

// Dynamic imports for heavy components
// 최근 방문 기록 컴포넌트를 동적 임포트로 로드하여 초기 페이지 로딩 속도 향상
const RecentVisits = dynamic(
  () => import("@/components/organisms/rolling-paper/RecentVisits").then(mod => ({ default: mod.RecentVisits })),
  {
    ssr: false, // 서버사이드 렌더링 비활성화 (로컬스토리지 사용으로 인해)
    loading: () => (
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 animate-pulse">
        <div className="h-6 bg-gray-200 rounded-lg mb-4 w-32"></div>
        <div className="space-y-3">
          <div className="h-16 bg-gray-200 rounded-lg"></div>
          <div className="h-16 bg-gray-200 rounded-lg"></div>
        </div>
      </div>
    )
  }
);

// 확인 다이얼로그를 동적 임포트 (사용자가 자신의 롤링페이퍼를 검색했을 때만 표시)
const ConfirmDialog = dynamic(
  () => import("./ConfirmDialog").then(mod => ({ default: mod.ConfirmDialog })),
  {
    ssr: false,
    loading: () => null // 다이얼로그는 로딩 상태 불필요
  }
);

export function VisitClient() {
  // 사용자가 자신의 롤링페이퍼를 검색했을 때 표시할 확인 다이얼로그 상태
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const router = useRouter();

  // 롤링페이퍼 검색 관련 상태와 핸들러들
  const {
    searchNickname,
    setSearchNickname,
    isSearching,
    searchError,
    handleSearch,
    handleKeyPress,
  } = useRollingPaperSearch();

  // 확인 다이얼로그에서 "내 롤링페이퍼 보기" 클릭 시 처리
  const handleGoToMyRollingPaper = () => {
    setShowConfirmDialog(false);
    router.push("/rolling-paper");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50">
      {/* Auth Header */}
      <AuthHeader />

      {/* Page Header - 모바일 최적화 */}
      <header className="sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b">
        <div className="container mx-auto px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 sm:w-8 sm:h-8 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center flex-shrink-0">
                <Heart className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
              </div>
              <h1 className="text-base sm:text-xl font-bold text-gray-800 whitespace-nowrap">
                롤링페이퍼 방문
              </h1>
            </div>
            <KakaoShareButton
              type="service"
              variant="outline"
              size="sm"
              className="px-2 sm:px-3 py-1 text-sm h-8"
            />
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8 max-w-md">
        <SearchSection
          searchNickname={searchNickname}
          setSearchNickname={setSearchNickname}
          isSearching={isSearching}
          searchError={searchError}
          onSearch={handleSearch}
          onKeyPress={handleKeyPress}
        />

        {/* 최근 방문한 롤링페이퍼 */}
        <div className="mb-8">
          <RecentVisits />
        </div>

        {/* Info Section */}
        <div className="mt-8 text-center">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start space-x-2">
              <Heart className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm text-blue-800">
                <p className="font-medium mb-1 flex items-center space-x-2">
                  <Mail className="w-4 h-4" />
                  <span>익명으로 메시지를 남겨보세요!</span>
                </p>
                <p>
                  로그인 없이도 누구나 따뜻한 메시지를 남길 수 있어요. 다양한
                  귀여운 디자인으로 메시지를 꾸며보세요!
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <ConfirmDialog
        isOpen={showConfirmDialog}
        onClose={() => setShowConfirmDialog(false)}
        onGoToMyRollingPaper={handleGoToMyRollingPaper}
      />

      {/* Footer */}
      <HomeFooter />
    </div>
  );
}

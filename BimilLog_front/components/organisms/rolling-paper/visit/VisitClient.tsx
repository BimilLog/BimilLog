"use client";
import dynamic from "next/dynamic";
import { Heart, Mail, Share2 } from "lucide-react";
import { Button as FlowbiteButton } from "flowbite-react";
import { useRollingPaperSearch, useToast } from "@/hooks";
import { KakaoShareButton } from "@/components";
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
      <div className="bg-paper-card rounded-xl shadow-brand-sm border border-ink-soft p-6 animate-pulse">
        <div className="h-6 bg-paper-200/40 rounded-lg mb-4 w-32"></div>
        <div className="space-y-3">
          <div className="h-16 bg-paper-200/40 rounded-lg"></div>
          <div className="h-16 bg-paper-200/40 rounded-lg"></div>
        </div>
      </div>
    )
  }
);

// 모든 사용자 목록 컴포넌트 동적 임포트
const AllUsersList = dynamic(
  () => import("./AllUsersList").then(mod => ({ default: mod.AllUsersList })),
  {
    ssr: false,
    loading: () => (
      <div className="bg-paper-card rounded-xl shadow-brand-sm border border-ink-soft p-6 animate-pulse">
        <div className="h-6 bg-paper-200/40 rounded-lg mb-4 w-32"></div>
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-16 bg-paper-200/40 rounded-lg"></div>
          ))}
        </div>
      </div>
    )
  }
);


export function VisitClient() {
  const { showSuccess, showError } = useToast();
  const {
    searchNickname,
    setSearchNickname,
    effectiveKeyword,
    isSearching,
    handleSearch,
  } = useRollingPaperSearch();

  // 검색 모드 — 디바운스 적용된 effectiveKeyword 기준 (입력만 한 상태도 포함하려면 searchNickname.trim())
  // 사용자가 타이핑 중이면 곧 검색 결과가 나올 것이므로 RecentVisits 를 미리 dim 처리
  const isSearchMode = searchNickname.trim().length > 0;

  const handleWebShare = async () => {
    const shareData = {
      title: '롤링페이퍼 방문 | 비밀로그',
      text: '익명으로 따뜻한 메시지를 남겨보세요!',
      url: window.location.href,
    };

    // 네이티브 공유 API 지원 여부 확인
    if (navigator.share && navigator.canShare && navigator.canShare(shareData)) {
      try {
        await navigator.share(shareData);
        showSuccess('공유 완료', '롤링페이퍼 방문 페이지가 공유되었습니다.');
      } catch (error) {
        // 사용자가 공유를 취소한 경우는 무시
        if ((error as Error).name !== 'AbortError') {
          showError('공유 실패', '공유하기에 실패했습니다.');
        }
      }
    } else {
      // 클립보드 복사 폴백
      try {
        await navigator.clipboard.writeText(shareData.url);
        showSuccess('복사 완료', '링크가 클립보드에 복사되었습니다.');
      } catch {
        showError('복사 실패', '링크 복사에 실패했습니다.');
      }
    }
  };

  return (
    <div className="min-h-screen bg-paper">
      {/* Auth Header — 라운드 17 F-17-BUG-2: 자체 sticky 헤더 동시 sticky 해소.
          AuthHeader 는 disableSticky 로 일반 흐름에 두고, 페이지 자체 헤더만 sticky. */}
      <AuthHeader disableSticky />

      {/* Page Header - 모바일 최적화. sticky 단독 사용. */}
      <header data-toast-anchor className="sticky top-0 z-sticky-header bg-paper-card/90 backdrop-blur-md border-b border-ink-soft">
        <div className="container mx-auto px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 sm:w-8 sm:h-8 bg-stamp-red rounded-lg flex items-center justify-center flex-shrink-0">
                <Heart className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
              </div>
              <h1 className="font-display text-base sm:text-xl font-bold text-ink dark:text-foreground whitespace-nowrap tracking-tight">
                롤링페이퍼 방문
              </h1>
            </div>
            <div className="flex items-center gap-1">
              <KakaoShareButton
                type="service"
                size="sm"
                mobileLabel="카톡"
                className="px-2 sm:px-3 py-1 text-sm h-8 whitespace-nowrap"
              />
              <FlowbiteButton
                onClick={handleWebShare}
                color="gray"
                size="sm"
                className="text-xs h-8 whitespace-nowrap"
              >
                <Share2 className="w-4 h-4 mr-1" />
                <span className="hidden sm:inline">링크 공유</span>
                <span className="sm:hidden">링크</span>
              </FlowbiteButton>
            </div>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8 max-w-md">
        {/* 최근 방문한 롤링페이퍼 — 검색 모드 시 dim 처리 */}
        <div className="mb-8">
          <RecentVisits dimmed={isSearchMode} />
        </div>

        {/* 통합 검색 섹션 (검색창 + 멤버 목록) */}
        <SearchSection
          searchNickname={searchNickname}
          setSearchNickname={setSearchNickname}
          isSearching={isSearching}
          onSearch={handleSearch}
        >
          {/* 디바운스 적용된 effectiveKeyword 를 AllUsersList 에 전달
              - Enter/돋보기 클릭 시 즉시 동기화, 그 외엔 300ms 디바운스 */}
          <AllUsersList searchKeyword={effectiveKeyword} />
        </SearchSection>

        {/* Info Section — 라운드 17 F-17-BUG-6: 라이트/다크 비대칭 토큰 정정.
            paper-aged 는 dark 시 paper-200 으로 자동 전환됨 (globals.css). */}
        <div className="mt-8 text-center">
          <div className="bg-paper-aged border border-postal-navy/30 dark:border-postal-navy/50 rounded-lg p-4">
            <div className="flex items-start space-x-2">
              <Heart className="w-5 h-5 text-stamp-red mt-0.5 flex-shrink-0" />
              <div className="text-sm font-body text-ink dark:text-ink-900">
                <p className="font-display font-semibold mb-1 flex items-center space-x-2 text-postal-navy dark:text-ink-900">
                  <Mail className="w-4 h-4" />
                  <span>익명으로 메시지를 남겨보세요!</span>
                </p>
                <p className="text-ink-soft dark:text-ink-500">
                  로그인 없이도 누구나 따뜻한 메시지를 남길 수 있어요. 다양한
                  귀여운 디자인으로 메시지를 꾸며보세요!
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Footer */}
      <HomeFooter />
    </div>
  );
}

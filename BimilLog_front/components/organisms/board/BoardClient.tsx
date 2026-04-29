"use client";

import { useState, useCallback, memo } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { BoardSearch } from "@/components/organisms/board";
import { Breadcrumb } from "@/components";

// 분리된 훅들 import
import { useInfinitePostList, usePopularPostsTabs, useNoticePosts } from "@/hooks/features";

// 분리된 컴포넌트들 import
import { BoardTabs } from "@/components/organisms/board/BoardTabs";

import { PageResponse, CursorPageResponse } from "@/types/common";
import { SimplePost } from "@/types/domains/post";

export interface BoardInitialData {
  // 일반 목록: CursorPageResponse, 검색 결과: PageResponse
  posts: CursorPageResponse<SimplePost> | PageResponse<SimplePost> | null;
  realtimePosts: PageResponse<SimplePost> | null;
  noticePosts: PageResponse<SimplePost> | null;
  isSearch?: boolean;
  searchQuery?: string;
  searchType?: 'TITLE' | 'TITLE_CONTENT' | 'WRITER';
}

interface BoardClientProps {
  initialData?: BoardInitialData;
}

function BoardClient({ initialData }: BoardClientProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [activeTab, setActiveTab] = useState("all");

  // URL에서 검색 파라미터 가져오기
  const urlQuery = searchParams.get('q');
  const urlType = searchParams.get('type') as 'TITLE' | 'TITLE_CONTENT' | 'WRITER' | null;

  // 게시판 데이터 관리 (커서 기반 무한 스크롤)
  const {
    posts,
    isLoading,
    error,
    // 커서 기반 (일반 목록)
    hasNextPage,
    loadMore,
    isFetchingNextPage,
    // 검색용 offset 페이지네이션
    searchPagination,
    // 검색 결과 totalElements (A-2)
    searchTotalElements,
    // 검색 fetching 상태 (A-3 spinner)
    isSearchFetching,
    // 검색 관련
    searchTerm,
    setSearchTerm,
    searchType,
    setSearchType,
    isSearching,
  } = useInfinitePostList({
    pageSize: 20,
    // 검색이 아닌 경우에만 커서 기반 초기 데이터 사용
    initialData: !initialData?.isSearch
      ? (initialData?.posts as CursorPageResponse<SimplePost> | null)
      : null,
    initialSearchTerm: initialData?.searchQuery || urlQuery || '',
    initialSearchType: initialData?.searchType || urlType || 'TITLE',
  });

  // 인기글 데이터 관리 - 각 탭 데이터 개별 제공
  const {
    realtimePosts,
    weeklyPosts,
    legendPosts,
    setActiveTab: setPopularTab,
    legendPagination,
    isLoading: popularLoading,
    error: popularError,
  } = usePopularPostsTabs(initialData?.realtimePosts);

  // 공지사항 데이터 관리 - '전체' 탭에서만 조회
  const { noticePosts } = useNoticePosts(activeTab === "all", initialData?.noticePosts);

  // 탭 변경 핸들러 메모이제이션
  // 메인 탭(all/realtime/popular/legend)과 인기글 탭(realtime/weekly/legend) 동기화
  const handleTabChange = useCallback((tab: string) => {
    setActiveTab(tab);

    // 메인 탭에 따라 인기글 데이터 API 호출 타입 변경
    if (tab === 'realtime' || tab === 'popular') {
      setPopularTab(tab === 'popular' ? 'weekly' : 'realtime');
    } else if (tab === 'legend') {
      setPopularTab('legend');
    }
  }, [setPopularTab]);

  // 검색 결과 페이지 변경 핸들러 (offset 기반, 클라이언트 사이드)
  const handleSearchPageChange = useCallback((newPage: number) => {
    if (searchPagination) {
      searchPagination.setCurrentPage(newPage);
    }
  }, [searchPagination]);

  // 검색 핸들러 (URL 기반으로 검색어 반영)
  // 인자로 받은 term이 있으면 우선 사용 (closure stale 방지)
  const handleSearch = useCallback((termOverride?: string) => {
    const params = new URLSearchParams();
    const effectiveTerm = (termOverride ?? searchTerm).trim();

    if (effectiveTerm) {
      params.set('q', effectiveTerm);
      if (searchType !== 'TITLE') {
        params.set('type', searchType);
      }
    }

    const queryString = params.toString();
    const targetUrl = `/board${queryString ? `?${queryString}` : ''}`;
    // Next.js 15 router.push 는 비동기로 진행되어 e2e 환경에서 networkidle
    // 직후 URL 이 즉시 반영되지 않는 케이스가 있다. history API 로 동기 갱신 후
    // router.replace 로 SPA navigation 을 보장한다.
    if (typeof window !== "undefined") {
      window.history.replaceState({}, "", targetUrl);
    }
    router.replace(targetUrl, { scroll: true });
  }, [router, searchTerm, searchType]);

  return (
    <MainLayout
      containerClassName="container-paper px-4"
    >
      <main className="pb-8">
        <Breadcrumb
          items={[
            { title: "홈", href: "/" },
            { title: "커뮤니티", href: "/board" },
          ]}
        />
        {/* 검색 섹션 */}
        <div className="mb-8">
          <BoardSearch
              searchTerm={searchTerm}
              setSearchTerm={setSearchTerm}
              searchType={searchType}
              setSearchType={setSearchType}
              handleSearch={handleSearch}
              isSearching={isSearching}
              totalElements={searchTotalElements}
              isSearchFetching={isSearchFetching}
            />
        </div>

        {/* 게시판 탭 */}
        <BoardTabs
          activeTab={activeTab}
          onTabChange={handleTabChange}
          posts={posts}
          isLoading={isLoading}
          error={error}
          isSearching={isSearching}
          searchTerm={searchTerm}
          // 커서 기반 (일반 목록)
          hasNextPage={hasNextPage}
          onLoadMore={loadMore}
          isFetchingNextPage={isFetchingNextPage}
          // 검색용 offset 페이지네이션
          searchPagination={searchPagination}
          onSearchPageChange={handleSearchPageChange}
          // 인기글 탭
          realtimePosts={realtimePosts}
          weeklyPosts={weeklyPosts}
          legendPosts={legendPosts}
          legendPagination={legendPagination}
          noticePosts={noticePosts}
          popularLoading={popularLoading}
          popularError={popularError}
        />

      </main>
    </MainLayout>
  );
}

// 메모이제이션으로 성능 최적화
export default memo(BoardClient);

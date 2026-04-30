"use client";

import { useState, useCallback, useEffect, useMemo, memo, useRef } from "react";
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
  /** F-BUG-3 (round-6): URL 의 page 파라미터(0-based) 를 SSR 에서 전달 */
  initialPage?: number;
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
  // F-BUG-3 (round-6): URL ?page= 를 0-based 로 변환 (URL 은 1-based)
  const urlPage = useMemo(() => {
    const raw = searchParams.get('page');
    if (!raw) return 0;
    const parsed = parseInt(raw, 10);
    return Number.isNaN(parsed) || parsed < 1 ? 0 : parsed - 1;
  }, [searchParams]);

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

  // 게시판 영역 ref (F-211: 검색 페이지 변경 후 스크롤 위치)
  const boardListRef = useRef<HTMLDivElement>(null);

  // F-BUG-3 (round-6): URL 의 page 파라미터를 검색 페이지네이션에 동기화 (마운트 + 뒤로가기/공유)
  useEffect(() => {
    if (!searchPagination) return;
    if (urlPage !== searchPagination.currentPage) {
      searchPagination.setCurrentPage(urlPage);
    }
    // searchPagination.currentPage 는 의도적으로 의존성 제외 — 사용자 페이지 클릭으로 변경된
    // currentPage 가 다시 마운트 effect 를 트리거해 무한 루프 발생 가능.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [urlPage, searchPagination?.setCurrentPage]);

  /**
   * 현재 검색 컨텍스트(질의/타입/페이지)를 URL 로 직렬화.
   * F-BUG-3, F-BUG-4: 페이지/타입 변경 시 URL 동기화.
   */
  const buildSearchUrl = useCallback((opts: {
    term: string;
    type: 'TITLE' | 'TITLE_CONTENT' | 'WRITER';
    page0: number;
  }) => {
    const params = new URLSearchParams();
    if (opts.term) {
      params.set('q', opts.term);
      if (opts.type !== 'TITLE') {
        params.set('type', opts.type);
      }
      if (opts.page0 > 0) {
        params.set('page', String(opts.page0 + 1)); // URL 은 1-based
      }
    }
    const queryString = params.toString();
    return `/board${queryString ? `?${queryString}` : ''}`;
  }, []);

  // 검색 결과 페이지 변경 핸들러 (offset 기반, 클라이언트 사이드)
  // F-BUG-3 (round-6): 페이지 변경 시 URL ?page= 도 동기화 → 새로고침/공유/뒤로가기 시 복원
  // F-211: 페이지 변경 후 게시글 영역 상단으로 스크롤 (사용자가 결과 첫 행을 인지)
  const handleSearchPageChange = useCallback((newPage: number) => {
    if (!searchPagination) return;
    searchPagination.setCurrentPage(newPage);
    const targetUrl = buildSearchUrl({
      term: searchTerm.trim(),
      type: searchType,
      page0: newPage,
    });
    if (typeof window !== "undefined") {
      window.history.replaceState({}, "", targetUrl);
    }
    router.replace(targetUrl, { scroll: false });

    // F-211: 페이지 클릭 후 결과 영역 상단으로 부드럽게 이동
    if (typeof window !== "undefined") {
      requestAnimationFrame(() => {
        boardListRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      });
    }
  }, [searchPagination, buildSearchUrl, searchTerm, searchType, router]);

  // 검색 핸들러 (URL 기반으로 검색어 반영)
  // 인자로 받은 term이 있으면 우선 사용 (closure stale 방지)
  const handleSearch = useCallback((termOverride?: string) => {
    const effectiveTerm = (termOverride ?? searchTerm).trim();
    // 새 검색은 항상 page=1 (URL 의 page 파라미터 제거 + 검색 페이지네이션 0 으로 리셋)
    if (searchPagination) {
      searchPagination.setCurrentPage(0);
    }
    const targetUrl = buildSearchUrl({
      term: effectiveTerm,
      type: searchType,
      page0: 0,
    });
    // Next.js 15 router.push 는 비동기로 진행되어 e2e 환경에서 networkidle
    // 직후 URL 이 즉시 반영되지 않는 케이스가 있다. history API 로 동기 갱신 후
    // router.replace 로 SPA navigation 을 보장한다.
    if (typeof window !== "undefined") {
      window.history.replaceState({}, "", targetUrl);
    }
    router.replace(targetUrl, { scroll: true });
  }, [router, searchTerm, searchType, searchPagination, buildSearchUrl]);

  // F-BUG-4 (round-6): searchType 토글 시 자동 재검색.
  // 사용자가 검색어가 있는 상태에서 type 을 토글하면 URL/결과 모두 새 type 으로 갱신.
  // 입력 중에는 트리거하지 않도록 debouncedSearchTerm == searchTerm 일 때만 동작.
  // (debounce 미해소 시점에 type 만 바뀌어도 의도한 결과)
  const lastTriggeredTypeRef = useRef(searchType);
  useEffect(() => {
    // 마운트 직후 동일 type 이면 무시
    if (lastTriggeredTypeRef.current === searchType) return;
    lastTriggeredTypeRef.current = searchType;
    const term = searchTerm.trim();
    if (!term) return; // 검색어가 비어 있으면 type 만 바뀌어도 fetch 불필요
    // URL 동기화 + 페이지 0 리셋 (usePostList 내부에서도 0 리셋하지만 URL 은 여기서만 동기화)
    const targetUrl = buildSearchUrl({ term, type: searchType, page0: 0 });
    if (typeof window !== "undefined") {
      window.history.replaceState({}, "", targetUrl);
    }
    router.replace(targetUrl, { scroll: false });
  }, [searchType, searchTerm, buildSearchUrl, router]);

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
        {/* F-211 (round-6): 검색 페이지 변경 후 결과 영역 상단으로 스크롤하기 위한 ref */}
        <div ref={boardListRef} aria-live="polite">
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
        </div>

      </main>
    </MainLayout>
  );
}

// 메모이제이션으로 성능 최적화
export default memo(BoardClient);

"use client";

import { useState, useEffect, useCallback, memo } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { BoardSearch } from "@/components/organisms/board";
import { Breadcrumb } from "@/components";

// 분리된 훅들 import
import { usePostList, usePopularPostsTabs, useNoticePosts } from "@/hooks/features";

// 분리된 컴포넌트들 import
import { BoardHeader } from "@/components/organisms/board/BoardHeader";
import { BoardTabs } from "@/components/organisms/board/BoardTabs";

import { PageResponse } from "@/types/common";
import { SimplePost } from "@/types/domains/post";

export interface BoardInitialData {
  posts: PageResponse<SimplePost> | null;
  realtimePosts: PageResponse<SimplePost> | null;
  noticePosts: PageResponse<SimplePost> | null;
  currentPage?: number;
  pageSize?: number;
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
  const [postsPerPage, setPostsPerPage] = useState("30");

  // URL에서 현재 페이지 가져오기 (1-based → 0-based 변환)
  const urlPage = searchParams.get('page');
  const urlQuery = searchParams.get('q');
  const urlType = searchParams.get('type') as 'TITLE' | 'TITLE_CONTENT' | 'WRITER' | null;
  const initialPage = urlPage ? parseInt(urlPage) - 1 : (initialData?.currentPage ?? 0);

  // 게시판 데이터 관리
  const {
    posts,
    isLoading,
    error,
    refetch: fetchPostsAndSearch,
    pagination,
    searchTerm,
    setSearchTerm,
    searchType,
    setSearchType,
  } = usePostList(
    Number(postsPerPage),
    initialData?.posts,
    initialPage,
    initialData?.searchQuery || urlQuery || '',
    initialData?.searchType || urlType || 'TITLE'
  );

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

  const { currentPage, setPageSize, setCurrentPage } = pagination;

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

  // URL 기반 페이지 변경 핸들러 (SSR 지원)
  const handlePageChange = useCallback((newPage: number) => {
    const params = new URLSearchParams();

    // 검색 중이면 검색 파라미터 유지
    if (searchTerm.trim()) {
      params.set('q', searchTerm.trim());
      if (searchType !== 'TITLE') {
        params.set('type', searchType);
      }
    }

    // 0-based → 1-based 변환 (URL은 1-based)
    const displayPage = newPage + 1;

    if (displayPage > 1) {
      params.set('page', String(displayPage));
    }

    const queryString = params.toString();
    router.push(`/board${queryString ? `?${queryString}` : ''}`, { scroll: false });
  }, [router, searchTerm, searchType]);

  // URL 기반 검색 핸들러 (SSR 지원)
  const handleSearch = useCallback(() => {
    const params = new URLSearchParams();

    if (searchTerm.trim()) {
      params.set('q', searchTerm.trim());
      if (searchType !== 'TITLE') {
        params.set('type', searchType);
      }
    }
    // 검색 시 페이지는 1로 리셋 (page 파라미터 제거)

    const queryString = params.toString();
    router.push(`/board${queryString ? `?${queryString}` : ''}`, { scroll: true });
  }, [router, searchTerm, searchType]);

  // 탭 변경 시 메인 데이터 조회 - '전체' 탭일 때만 일반 게시글 API 호출
  // 인기글 탭들은 usePopularPostsTabs 훅에서 별도로 관리됨
  useEffect(() => {
    if (activeTab === "all") {
      fetchPostsAndSearch();
    }
  }, [currentPage, activeTab, fetchPostsAndSearch]);

  // 페이지당 게시글 수 변경 시 처리 - 첫 페이지로 리셋 후 새로운 페이지 크기 적용
  useEffect(() => {
    setPageSize(Number(postsPerPage));
    setCurrentPage(0); // 페이지 크기 변경 시 첫 페이지로 이동
  }, [postsPerPage, setPageSize, setCurrentPage]);

  return (
    <MainLayout
      className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 dark:from-[#121327] dark:via-[#1a1030] dark:to-[#0b0c1c]"
      containerClassName="container mx-auto px-4"
    >
      {/* 게시판 헤더 */}
      <BoardHeader />

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
              postsPerPage={postsPerPage}
              setPostsPerPage={setPostsPerPage}
              handleSearch={handleSearch}
            />
        </div>

        {/* 게시판 탭 */}
        <BoardTabs
          activeTab={activeTab}
          onTabChange={handleTabChange}
          posts={posts}
          isLoading={isLoading}
          error={error}
          isSearching={!!searchTerm.trim()}
          searchTerm={searchTerm}
          currentPage={pagination.currentPage}
          totalPages={pagination.totalPages}
          onPageChange={handlePageChange}
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

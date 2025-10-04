"use client";

import { useState, useEffect, useCallback, memo } from "react";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { BoardSearch } from "@/components/organisms/board";
import {
  AdFitBanner,
  AD_SIZES,
  getAdUnit,
  Breadcrumb,
} from "@/components";

// 분리된 훅들 import
import { usePostList, usePopularPostsTabs } from "@/hooks/features";

// 분리된 컴포넌트들 import
import { BoardHeader } from "@/components/organisms/board/BoardHeader";
import { BoardTabs } from "@/components/organisms/board/BoardTabs";

function BoardClient() {
  const [activeTab, setActiveTab] = useState("all");
  const [postsPerPage, setPostsPerPage] = useState("30");

  // 게시판 데이터 관리
  const {
    posts,
    refetch: fetchPostsAndSearch,
    pagination,
    searchTerm,
    setSearchTerm,
    searchType,
    setSearchType,
    search: handleSearch,
  } = usePostList(Number(postsPerPage));

  // 인기글 데이터 관리
  const {
    posts: popularPostsData,
    activeTab: popularTab,
    setActiveTab: setPopularTab,
    legendPagination,
  } = usePopularPostsTabs();

  // 탭별 데이터 분리 - 인기글 API는 하나의 상태로 관리되지만 탭에 따라 다른 데이터를 표시
  const realtimePosts = popularTab === 'realtime' ? popularPostsData : [];
  const weeklyPosts = popularTab === 'weekly' ? popularPostsData : [];
  const legendPosts = popularTab === 'legend' ? popularPostsData : [];
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
      className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50"
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
          currentPage={pagination.currentPage}
          totalPages={pagination.totalPages}
          onPageChange={pagination.setCurrentPage}
          realtimePosts={realtimePosts}
          weeklyPosts={weeklyPosts}
          legendPosts={legendPosts}
          legendPagination={legendPagination}
        />

        {/* Mobile Advertisement - 게시판 아래로 이동 */}
        <div className="mt-6 mb-6">
          <div className="flex justify-center px-2">
            {getAdUnit("MOBILE_BANNER") && (
              <AdFitBanner
                adUnit={getAdUnit("MOBILE_BANNER")!}
                width={AD_SIZES.BANNER_320x50.width}
                height={AD_SIZES.BANNER_320x50.height}
              />
            )}
          </div>
        </div>

        {/* Bottom Mobile Advertisement */}
        <div className="mt-8 pt-6">
          <div className="flex justify-center px-2">
            {getAdUnit("MOBILE_BANNER") && (
              <AdFitBanner
                adUnit={getAdUnit("MOBILE_BANNER")!}
                width={AD_SIZES.BANNER_320x50.width}
                height={AD_SIZES.BANNER_320x50.height}
              />
            )}
          </div>
        </div>
      </main>
    </MainLayout>
  );
}

// 메모이제이션으로 성능 최적화
export default memo(BoardClient);

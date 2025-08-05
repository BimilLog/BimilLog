"use client";

import { useState, useEffect } from "react";
import { AuthHeader } from "@/components/organisms/auth-header";
import { BoardSearch } from "@/components/organisms/board/board-search";
import {
  ResponsiveAdFitBanner,
  AdFitBanner,
  AD_SIZES,
  getAdUnit,
  Breadcrumb,
} from "@/components";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";

// 분리된 훅들 import
import { useBoardData } from "./hooks/useBoardData";
import { usePopularPosts } from "./hooks/usePopularPosts";

// 분리된 컴포넌트들 import
import { BoardHeader } from "@/components/organisms/board/BoardHeader";
import { BoardTabs } from "@/components/organisms/board/BoardTabs";

export default function BoardClient() {
  const [activeTab, setActiveTab] = useState("all");

  // 게시판 데이터 관리
  const {
    searchTerm,
    setSearchTerm,
    searchType,
    setSearchType,
    postsPerPage,
    setPostsPerPage,
    handleSearch,
    posts,
    currentPage,
    setCurrentPage,
    totalPages,
    fetchPostsAndSearch,
  } = useBoardData();

  // 인기글 데이터 관리
  const { realtimePosts, weeklyPosts, legendPosts } =
    usePopularPosts(activeTab);

  // 탭 변경 시 메인 데이터 조회
  useEffect(() => {
    if (activeTab === "all") {
      fetchPostsAndSearch(currentPage);
    }
  }, [currentPage, activeTab, fetchPostsAndSearch]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      {/* Top Banner Advertisement */}
      <div className="container mx-auto px-4 py-2">
        <div className="flex justify-center">
          <ResponsiveAdFitBanner
            position="게시판 최상단"
            className="max-w-full"
          />
        </div>
      </div>

      {/* 게시판 헤더 */}
      <BoardHeader />

      <main className="container mx-auto px-4 pb-8">
        <Breadcrumb
          items={[
            { title: "홈", href: "/" },
            { title: "커뮤니티", href: "/board" },
          ]}
        />
        {/* 검색 섹션 */}
        <div className="mb-8">
          <div className="bg-white/80 backdrop-blur-sm rounded-lg border-0 shadow-lg p-6">
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
        </div>

        {/* 게시판 탭 */}
        <BoardTabs
          activeTab={activeTab}
          onTabChange={setActiveTab}
          posts={posts}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
          realtimePosts={realtimePosts}
          weeklyPosts={weeklyPosts}
          legendPosts={legendPosts}
        />

        {/* Mobile Advertisement - 게시판 아래로 이동 */}
        <div className="mt-6 mb-6">
          <div className="flex justify-center px-2">
            {(() => {
              const adUnit = getAdUnit("MOBILE_BANNER");
              return adUnit ? (
                <AdFitBanner
                  adUnit={adUnit}
                  width={AD_SIZES.BANNER_320x50.width}
                  height={AD_SIZES.BANNER_320x50.height}
                  onAdFail={() => {
                if (process.env.NODE_ENV === 'development') {
                  console.log("게시판 중간 광고 로딩 실패");
                }
              }}
                />
              ) : null;
            })()}
          </div>
        </div>

        {/* Bottom Mobile Advertisement */}
        <div className="mt-8 pt-6">
          <div className="flex justify-center px-2">
            {(() => {
              const adUnit = getAdUnit("MOBILE_BANNER");
              return adUnit ? (
                <AdFitBanner
                  adUnit={adUnit}
                  width={AD_SIZES.BANNER_320x50.width}
                  height={AD_SIZES.BANNER_320x50.height}
                  onAdFail={() => {
                if (process.env.NODE_ENV === 'development') {
                  console.log("게시판 하단 광고 로딩 실패");
                }
              }}
                />
              ) : null;
            })()}
          </div>
        </div>
      </main>

      <HomeFooter />
    </div>
  );
}

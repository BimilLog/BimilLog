"use client";

import { useState, useEffect, useCallback } from "react";
import { boardApi, type SimplePost } from "@/lib/api";
import { AuthHeader } from "@/components/organisms/auth-header";
import { BoardSearch } from "@/components/organisms/board/board-search";
import { PostList } from "@/components/organisms/board/post-list";
import { PopularPostList } from "@/components/organisms/board/popular-post-list";
import { BoardPagination } from "@/components/organisms/board/board-pagination";
import { NoticeList } from "@/components/organisms/board/notice-list";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MessageSquare, TrendingUp, Crown, Edit, Zap } from "lucide-react";
import Link from "next/link";

export default function BoardClient() {
  // State declarations
  const [searchTerm, setSearchTerm] = useState("");
  const [searchType, setSearchType] = useState<
    "TITLE" | "TITLE_CONTENT" | "AUTHOR"
  >("TITLE");
  const [postsPerPage, setPostsPerPage] = useState("30");

  const [posts, setPosts] = useState<SimplePost[]>([]);
  const [realtimePosts, setRealtimePosts] = useState<SimplePost[]>([]);
  const [weeklyPosts, setWeeklyPosts] = useState<SimplePost[]>([]);
  const [legendPosts, setLegendPosts] = useState<SimplePost[]>([]);

  const [, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("all");

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Data fetching functions
  const fetchPostsAndSearch = useCallback(
    async (page = 0) => {
      setIsLoading(true);
      try {
        const response = searchTerm.trim()
          ? await boardApi.searchPosts(
              searchType,
              searchTerm.trim(),
              page,
              Number(postsPerPage)
            )
          : await boardApi.getPosts(page, Number(postsPerPage));

        if (response.success && response.data) {
          setPosts(response.data.content);
          setTotalPages(response.data.totalPages);
          setCurrentPage(response.data.number);
        } else {
          setPosts([]);
          setTotalPages(0);
          setCurrentPage(0);
        }
      } catch (error) {
        console.error("Failed to fetch posts:", error);
      } finally {
        setIsLoading(false);
      }
    },
    [searchType, searchTerm, postsPerPage]
  );

  useEffect(() => {
    if (activeTab === "all") {
      fetchPostsAndSearch(currentPage);
    }
  }, [currentPage, activeTab, fetchPostsAndSearch]);

  useEffect(() => {
    // 페이지당 게시글 수가 바뀌면 첫 페이지로 이동하여 검색
    fetchPostsAndSearch(0);
  }, [postsPerPage]);

  // 실시간/주간/레전드 인기글 데이터 지연 로딩
  useEffect(() => {
    const fetchRealtime = async () => {
      // 데이터가 이미 있으면 다시 호출하지 않음
      if (realtimePosts.length > 0) return;
      try {
        const res = await boardApi.getRealtimePosts();
        if (res.success && res.data) setRealtimePosts(res.data);
      } catch (error) {
        console.error("Failed to fetch realtime posts:", error);
      }
    };

    const fetchWeekly = async () => {
      // 데이터가 이미 있으면 다시 호출하지 않음
      if (weeklyPosts.length > 0) return;
      try {
        const res = await boardApi.getWeeklyPosts();
        if (res.success && res.data) setWeeklyPosts(res.data);
      } catch (error) {
        console.error("Failed to fetch weekly posts:", error);
      }
    };

    const fetchLegend = async () => {
      // 데이터가 이미 있으면 다시 호출하지 않음
      if (legendPosts.length > 0) return;
      try {
        const res = await boardApi.getLegendPosts();
        if (res.success && res.data) setLegendPosts(res.data);
      } catch (error) {
        console.error("Failed to fetch legend posts:", error);
      }
    };

    if (activeTab === "realtime") {
      fetchRealtime();
    } else if (activeTab === "popular") {
      fetchWeekly();
    } else if (activeTab === "legend") {
      fetchLegend();
    }
  }, [activeTab, legendPosts.length, realtimePosts.length, weeklyPosts.length]);

  const handleSearch = () => {
    // 검색 시 항상 첫 페이지부터 결과 표시
    setCurrentPage(0);
    fetchPostsAndSearch(0);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      <header className="py-8">
        <div className="container mx-auto px-4 text-center">
          <div className="flex items-center justify-center space-x-3 mb-4">
            <div className="w-12 h-12 bg-gradient-to-r from-purple-500 to-indigo-500 rounded-xl flex items-center justify-center">
              <MessageSquare className="w-7 h-7 text-white" />
            </div>
            <h1 className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent">
              커뮤니티 게시판
            </h1>
          </div>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto mb-6">
            다른 사용자들과 소통하고 생각을 나누어보세요
          </p>
          <Button
            asChild
            size="lg"
            className="bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700"
          >
            <Link href="/board/write">
              <Edit className="w-5 h-5 mr-2" />
              글쓰기
            </Link>
          </Button>
        </div>
      </header>

      <main className="container mx-auto px-4 pb-8">
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

        <Tabs
          value={activeTab}
          onValueChange={setActiveTab}
          className="space-y-6"
        >
          <TabsList className="grid w-full grid-cols-4 bg-white/80 backdrop-blur-sm shadow-lg">
            <TabsTrigger
              value="all"
              className="flex items-center space-x-2 data-[state=active]:bg-gradient-to-r data-[state=active]:from-pink-500 data-[state=active]:to-purple-600 data-[state=active]:text-white"
            >
              <MessageSquare className="w-4 h-4" />
              <span className="hidden sm:inline">전체글</span>
              <span className="sm:hidden">전체</span>
            </TabsTrigger>
            <TabsTrigger
              value="realtime"
              className="flex items-center space-x-2 data-[state=active]:bg-gradient-to-r data-[state=active]:from-pink-500 data-[state=active]:to-purple-600 data-[state=active]:text-white"
            >
              <Zap className="w-4 h-4" />
              <span className="hidden sm:inline">실시간</span>
              <span className="sm:hidden">실시간</span>
            </TabsTrigger>
            <TabsTrigger
              value="popular"
              className="flex items-center space-x-2 data-[state=active]:bg-gradient-to-r data-[state=active]:from-pink-500 data-[state=active]:to-purple-600 data-[state=active]:text-white"
            >
              <TrendingUp className="w-4 h-4" />
              <span className="hidden sm:inline">주간</span>
              <span className="sm:hidden">주간</span>
            </TabsTrigger>
            <TabsTrigger
              value="legend"
              className="flex items-center space-x-2 data-[state=active]:bg-gradient-to-r data-[state=active]:from-pink-500 data-[state=active]:to-purple-600 data-[state=active]:text-white"
            >
              <Crown className="w-4 h-4" />
              <span className="hidden sm:inline">레전드</span>
              <span className="sm:hidden">레전드</span>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="all" className="space-y-4">
            <NoticeList posts={posts} />
            <PostList posts={posts} />
          </TabsContent>

          <TabsContent value="realtime">
            <PopularPostList
              posts={realtimePosts}
              title="실시간 인기글"
              icon={<Zap className="w-5 h-5 text-red-500" />}
            />
          </TabsContent>

          <TabsContent value="popular">
            <PopularPostList
              posts={weeklyPosts}
              title="주간 인기글"
              icon={<TrendingUp className="w-5 h-5 text-orange-500" />}
            />
          </TabsContent>

          <TabsContent value="legend">
            <PopularPostList
              posts={legendPosts}
              title="레전드 글"
              icon={<Crown className="w-5 h-5 text-purple-500" />}
            />
          </TabsContent>
        </Tabs>

        {activeTab === "all" && (
          <BoardPagination
            currentPage={currentPage}
            totalPages={totalPages}
            setCurrentPage={setCurrentPage}
          />
        )}
      </main>
    </div>
  );
}

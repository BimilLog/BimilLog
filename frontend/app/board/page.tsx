"use client";

import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/hooks/useAuth";
import { boardApi, type SimplePost } from "@/lib/api";

import { AuthHeader } from "@/components/auth-header";
import { BoardSearch } from "@/components/board/board-search";
import { PostList } from "@/components/board/post-list";
import { PopularPostList } from "@/components/board/popular-post-list";
import { BoardPagination } from "@/components/board/board-pagination";
import { NoticeList } from "@/components/board/notice-list";

import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MessageSquare, TrendingUp, Clock, Crown, Edit } from "lucide-react";
import Link from "next/link";

// 공지사항 데이터는 변하지 않으므로 컴포넌트 외부로 이동
const notices = [
  {
    id: 1,
    title: "비밀로그 서비스 이용약관 안내",
    author: "관리자",
    date: "2024.01.20",
    views: 1234,
    isPinned: true,
  },
  {
    id: 2,
    title: "새로운 기능 업데이트 안내",
    author: "관리자",
    date: "2024.01.18",
    views: 856,
    isPinned: true,
  },
];

export default function BoardPage() {
  const { isAuthenticated } = useAuth();

  // State declarations
  const [searchTerm, setSearchTerm] = useState("");
  const [searchType, setSearchType] = useState<"title" | "content" | "author">(
    "title"
  );
  const [postsPerPage, setPostsPerPage] = useState("30");

  const [posts, setPosts] = useState<SimplePost[]>([]);
  const [weeklyPosts, setWeeklyPosts] = useState<SimplePost[]>([]);
  const [legendPosts, setLegendPosts] = useState<SimplePost[]>([]);

  const [isLoading, setIsLoading] = useState(true);
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
    if (activeTab === "all" || activeTab === "recent") {
      fetchPostsAndSearch(currentPage);
    }
  }, [currentPage, activeTab, fetchPostsAndSearch]);

  useEffect(() => {
    // 페이지당 게시글 수가 바뀌면 첫 페이지로 이동하여 검색
    fetchPostsAndSearch(0);
  }, [postsPerPage]);

  // 주간/레전드 인기글 데이터 지연 로딩
  useEffect(() => {
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

    if (activeTab === "popular") {
      fetchWeekly();
    } else if (activeTab === "legend") {
      fetchLegend();
    }
  }, [activeTab]); // weeklyPosts, legendPosts는 의존성 배열에서 제거하여 탭 이동마다 재호출되지 않도록 함

  const handleSearch = () => {
    // 검색 시 항상 첫 페이지부터 결과 표시
    setCurrentPage(0);
    fetchPostsAndSearch(0);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      <header className="py-4">
        <div className="container mx-auto px-4 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <MessageSquare className="w-6 h-6 text-purple-600" />
            <h1 className="text-xl font-bold text-gray-800">커뮤니티 게시판</h1>
          </div>
          <Button
            asChild
            className="bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700"
          >
            <Link href="/board/write">
              <Edit className="w-4 h-4 mr-2" />
              글쓰기
            </Link>
          </Button>
        </div>
      </header>

      <main className="container mx-auto px-4 py-8">
        <BoardSearch
          searchTerm={searchTerm}
          setSearchTerm={setSearchTerm}
          searchType={searchType}
          setSearchType={setSearchType}
          postsPerPage={postsPerPage}
          setPostsPerPage={setPostsPerPage}
          handleSearch={handleSearch}
        />

        <Tabs
          value={activeTab}
          onValueChange={setActiveTab}
          className="space-y-6"
        >
          <TabsList className="grid w-full grid-cols-4 bg-white/80 backdrop-blur-sm">
            <TabsTrigger value="all" className="flex items-center space-x-2">
              <MessageSquare className="w-4 h-4" />
              <span>전체글</span>
            </TabsTrigger>
            <TabsTrigger
              value="popular"
              className="flex items-center space-x-2"
            >
              <TrendingUp className="w-4 h-4" />
              <span>주간 인기</span>
            </TabsTrigger>
            <TabsTrigger value="recent" className="flex items-center space-x-2">
              <Clock className="w-4 h-4" />
              <span>최신글</span>
            </TabsTrigger>
            <TabsTrigger value="legend" className="flex items-center space-x-2">
              <Crown className="w-4 h-4" />
              <span>레전드</span>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="all" className="space-y-4">
            <NoticeList notices={notices} />
            <PostList posts={posts} />
          </TabsContent>

          <TabsContent value="recent" className="space-y-4">
            <NoticeList notices={notices} />
            <PostList posts={posts} />
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

        {(activeTab === "all" || activeTab === "recent") && (
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

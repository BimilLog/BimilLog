"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MessageSquare, TrendingUp, Crown, Zap } from "lucide-react";
import { NoticeList } from "./notice-list";
import { PostList } from "./post-list";
import { PopularPostList } from "./popular-post-list";
import { BoardPagination } from "./board-pagination";
import type { SimplePost } from "@/lib/api";

interface BoardTabsProps {
  activeTab: string;
  onTabChange: (value: string) => void;

  // 전체글 탭 데이터
  posts: SimplePost[];
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;

  // 인기글 탭 데이터
  realtimePosts: SimplePost[];
  weeklyPosts: SimplePost[];
  legendPosts: SimplePost[];
}

export const BoardTabs: React.FC<BoardTabsProps> = ({
  activeTab,
  onTabChange,
  posts,
  currentPage,
  totalPages,
  onPageChange,
  realtimePosts,
  weeklyPosts,
  legendPosts,
}) => {
  return (
    <Tabs value={activeTab} onValueChange={onTabChange} className="space-y-6">
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

      {activeTab === "all" && (
        <BoardPagination
          currentPage={currentPage}
          totalPages={totalPages}
          setCurrentPage={onPageChange}
        />
      )}
    </Tabs>
  );
};

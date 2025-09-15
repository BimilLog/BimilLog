"use client";

import React, { memo, useMemo } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components";
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

const BoardTabsComponent: React.FC<BoardTabsProps> = ({
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
  // 탭 설정 메모화
  const tabConfig = useMemo(() => ([
    {
      value: "all",
      icon: <MessageSquare className="w-4 h-4" />,
      label: { desktop: "전체글", mobile: "전체" }
    },
    {
      value: "realtime",
      icon: <Zap className="w-4 h-4" />,
      label: { desktop: "실시간", mobile: "실시간" }
    },
    {
      value: "popular",
      icon: <TrendingUp className="w-4 h-4" />,
      label: { desktop: "주간", mobile: "주간" }
    },
    {
      value: "legend",
      icon: <Crown className="w-4 h-4" />,
      label: { desktop: "레전드", mobile: "레전드" }
    }
  ]), []);

  // 인기글 설정 메모화
  const popularPostsConfig = useMemo(() => ({
    realtime: {
      posts: realtimePosts,
      title: "실시간 인기글",
      icon: <Zap className="w-5 h-5 text-red-500" />
    },
    popular: {
      posts: weeklyPosts,
      title: "주간 인기글",
      icon: <TrendingUp className="w-5 h-5 text-orange-500" />
    },
    legend: {
      posts: legendPosts,
      title: "레전드 글",
      icon: <Crown className="w-5 h-5 text-purple-500" />
    }
  }), [realtimePosts, weeklyPosts, legendPosts]);

  // 페이지네이션 표시 조건 메모화
  const showPagination = useMemo(() => activeTab === "all" && totalPages > 0, [activeTab, totalPages]);

  return (
    <Tabs value={activeTab} onValueChange={onTabChange} className="space-y-6">
      <TabsList className="grid w-full grid-cols-4 bg-gray-50 rounded-lg p-1 border border-gray-200">
        {tabConfig.map(({ value, icon, label }) => (
          <TabsTrigger
            key={value}
            value={value}
            className="flex items-center justify-center space-x-2 text-gray-500 hover:text-gray-700 data-[state=active]:bg-white data-[state=active]:text-brand-primary data-[state=active]:shadow-sm data-[state=active]:border data-[state=active]:border-gray-200 rounded-md transition-all duration-200 font-medium"
          >
            {icon}
            <span className="hidden sm:inline">{label.desktop}</span>
            <span className="sm:hidden">{label.mobile}</span>
          </TabsTrigger>
        ))}
      </TabsList>

      <TabsContent value="all" className="space-y-4">
        <NoticeList posts={posts} />
        <PostList posts={posts} />
      </TabsContent>

      {Object.entries(popularPostsConfig).map(([key, config]) => (
        <TabsContent key={key} value={key}>
          <PopularPostList
            posts={config.posts}
            title={config.title}
            icon={config.icon}
          />
        </TabsContent>
      ))}

      {showPagination && (
        <BoardPagination
          currentPage={currentPage}
          totalPages={totalPages}
          setCurrentPage={onPageChange}
        />
      )}
    </Tabs>
  );
};

export const BoardTabs = memo(BoardTabsComponent);
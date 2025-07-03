import { useState, useEffect } from "react";
import { boardApi, type SimplePost } from "@/lib/api";

export const usePopularPosts = (activeTab: string) => {
  const [realtimePosts, setRealtimePosts] = useState<SimplePost[]>([]);
  const [weeklyPosts, setWeeklyPosts] = useState<SimplePost[]>([]);
  const [legendPosts, setLegendPosts] = useState<SimplePost[]>([]);

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

  return {
    realtimePosts,
    weeklyPosts,
    legendPosts,
  };
}; 
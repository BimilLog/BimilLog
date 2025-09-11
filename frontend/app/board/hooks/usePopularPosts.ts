import { useState, useEffect } from "react";
import { boardApi, type SimplePost } from "@/lib/api";

export const usePopularPosts = (activeTab: string) => {
  const [realtimePosts, setRealtimePosts] = useState<SimplePost[]>([]);
  const [weeklyPosts, setWeeklyPosts] = useState<SimplePost[]>([]);
  const [legendPosts, setLegendPosts] = useState<SimplePost[]>([]);
  const [popularDataFetched, setPopularDataFetched] = useState(false);

  // 실시간/주간/레전드 인기글 데이터 지연 로딩
  useEffect(() => {
    // 실시간 + 주간 인기글 한 번에 조회 (v2 API 통합)
    const fetchPopular = async () => {
      // 데이터가 이미 있으면 다시 호출하지 않음
      if (popularDataFetched) return;
      
      try {
        const res = await boardApi.getPopularPosts();
        if (res.success && res.data) {
          setRealtimePosts(res.data.realtime || []);
          setWeeklyPosts(res.data.weekly || []);
          setPopularDataFetched(true);
        }
      } catch (error) {
        console.error("Failed to fetch popular posts:", error);
      }
    };

    const fetchLegend = async () => {
      // 데이터가 이미 있으면 다시 호출하지 않음
      if (legendPosts.length > 0) return;
      try {
        const res = await boardApi.getLegendPosts(0, 10); // 첫 페이지 10개 조회
        if (res.success && res.data) {
          // PageResponse의 content 배열 사용
          setLegendPosts(res.data.content || []);
        }
      } catch (error) {
        console.error("Failed to fetch legend posts:", error);
      }
    };

    // 실시간/주간 탭이 활성화되면 한 번에 조회
    if (activeTab === "realtime" || activeTab === "popular") {
      fetchPopular();
    } else if (activeTab === "legend") {
      fetchLegend();
    }
  }, [activeTab, legendPosts.length, popularDataFetched]);

  return {
    realtimePosts,
    weeklyPosts,
    legendPosts,
  };
}; 
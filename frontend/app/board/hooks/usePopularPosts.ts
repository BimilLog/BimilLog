import { useState, useEffect, useCallback } from "react";
import { boardApi, type SimplePost } from "@/lib/api";

export const usePopularPosts = (activeTab: string) => {
  const [realtimePosts, setRealtimePosts] = useState<SimplePost[]>([]);
  const [weeklyPosts, setWeeklyPosts] = useState<SimplePost[]>([]);
  const [legendPosts, setLegendPosts] = useState<SimplePost[]>([]);
  const [popularDataFetched, setPopularDataFetched] = useState(false);
  const [legendDataFetched, setLegendDataFetched] = useState(false);
  const [lastPopularFetchTime, setLastPopularFetchTime] = useState<Date | null>(null);
  const [lastLegendFetchTime, setLastLegendFetchTime] = useState<Date | null>(null);

  // 실시간 + 주간 인기글 한 번에 조회 (v2 API 통합)
  const fetchPopular = useCallback(async (forceRefresh = false) => {
    // 강제 갱신이 아니고 데이터가 이미 있으면 5분 이내 재호출 방지
    if (!forceRefresh && popularDataFetched && lastPopularFetchTime) {
      const timeSinceLastFetch = Date.now() - lastPopularFetchTime.getTime();
      if (timeSinceLastFetch < 5 * 60 * 1000) return; // 5분
    }
    
    try {
      const res = await boardApi.getPopularPosts();
      if (res.success && res.data) {
        setRealtimePosts(res.data.realtime || []);
        setWeeklyPosts(res.data.weekly || []);
        setPopularDataFetched(true);
        setLastPopularFetchTime(new Date());
      }
    } catch {
      setRealtimePosts([]);
      setWeeklyPosts([]);
    }
  }, [popularDataFetched, lastPopularFetchTime]);

  const fetchLegend = useCallback(async (forceRefresh = false) => {
    // 강제 갱신이 아니고 데이터가 이미 있으면 5분 이내 재호출 방지
    if (!forceRefresh && legendDataFetched && lastLegendFetchTime) {
      const timeSinceLastFetch = Date.now() - lastLegendFetchTime.getTime();
      if (timeSinceLastFetch < 5 * 60 * 1000) return; // 5분
    }
    
    try {
      const res = await boardApi.getLegendPosts(0, 10); // 첫 페이지 10개 조회
      if (res.success && res.data) {
        // PageResponse의 content 배열 사용
        setLegendPosts(res.data.content || []);
        setLegendDataFetched(true);
        setLastLegendFetchTime(new Date());
      }
    } catch {
      setLegendPosts([]);
    }
  }, [legendDataFetched, lastLegendFetchTime]);

  // 탭별 데이터 지연 로딩
  useEffect(() => {
    // 실시간/주간 탭이 활성화되면 한 번에 조회
    if (activeTab === "realtime" || activeTab === "popular") {
      fetchPopular();
    } else if (activeTab === "legend") {
      fetchLegend();
    }
  }, [activeTab, fetchPopular, fetchLegend]);

  // 데이터 강제 갱신 함수
  const refreshPopularPosts = useCallback(() => {
    setPopularDataFetched(false);
    setLastPopularFetchTime(null);
    fetchPopular(true);
  }, [fetchPopular]);

  const refreshLegendPosts = useCallback(() => {
    setLegendDataFetched(false);
    setLastLegendFetchTime(null);
    fetchLegend(true);
  }, [fetchLegend]);

  return {
    realtimePosts,
    weeklyPosts,
    legendPosts,
    refreshPopularPosts,
    refreshLegendPosts,
  };
}; 
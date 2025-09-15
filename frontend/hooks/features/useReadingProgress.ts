"use client";

import { useState, useEffect, useCallback, useRef } from 'react';
import {
  saveReadingProgress,
  getPostProgress,
  getReadingProgress,
  getReadPosts,
  isPostRead,
  startReadingSession,
  endReadingSession,
  getReadingStats,
  getInProgressReading,
  getRecentlyCompleted,
  removeReadingProgress,
  type ReadingProgress,
  type ReadingStats,
} from '@/lib/utils/reading-progress';

interface UseReadingProgressOptions {
  postId?: number;
  autoTrack?: boolean;
}

export function useReadingProgress(options: UseReadingProgressOptions = {}) {
  const { postId, autoTrack = true } = options;
  const [progress, setProgress] = useState<number>(0);
  const [isRead, setIsRead] = useState<boolean>(false);
  const [readingTime, setReadingTime] = useState<number>(0);
  const [allProgress, setAllProgress] = useState<ReadingProgress[]>([]);
  const [stats, setStats] = useState<ReadingStats | null>(null);
  const [inProgress, setInProgress] = useState<ReadingProgress[]>([]);
  const [recentlyCompleted, setRecentlyCompleted] = useState<ReadingProgress[]>([]);

  const scrollTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const lastScrollPosition = useRef<number>(0);

  // 스크롤 핸들러
  const handleScroll = useCallback(() => {
    if (!postId || !autoTrack) return;

    const scrollPosition = window.scrollY;
    const totalHeight = document.documentElement.scrollHeight;
    const viewportHeight = window.innerHeight;

    // 진행률 계산
    const currentProgress = Math.min(
      100,
      ((scrollPosition + viewportHeight) / totalHeight) * 100
    );

    setProgress(currentProgress);

    // 디바운스된 저장
    if (scrollTimeoutRef.current) {
      clearTimeout(scrollTimeoutRef.current);
    }

    scrollTimeoutRef.current = setTimeout(() => {
      saveReadingProgress(postId, scrollPosition, totalHeight);

      // 읽음 상태 업데이트
      if (currentProgress >= 90) {
        setIsRead(true);
      }
    }, 500);

    lastScrollPosition.current = scrollPosition;
  }, [postId, autoTrack]);

  // 초기 데이터 로드
  const loadData = useCallback(() => {
    if (postId) {
      // 특정 게시글 진행률
      const postProgress = getPostProgress(postId);
      if (postProgress) {
        setProgress(postProgress.progress);
        setReadingTime(postProgress.readTime);
      }
      setIsRead(isPostRead(postId));

      // 읽기 세션 시작
      if (autoTrack) {
        startReadingSession(postId);
      }
    }

    // 전체 데이터
    setAllProgress(getReadingProgress());
    setStats(getReadingStats());
    setInProgress(getInProgressReading());
    setRecentlyCompleted(getRecentlyCompleted());
  }, [postId, autoTrack]);

  // 진행률 제거
  const removeProgress = useCallback((targetPostId: number) => {
    removeReadingProgress(targetPostId);
    loadData();
  }, [loadData]);

  // 읽기 진행률 수동 저장
  const saveProgress = useCallback((targetPostId: number, scrollPos: number, totalHeight: number) => {
    saveReadingProgress(targetPostId, scrollPos, totalHeight);
    loadData();
  }, [loadData]);

  // 페이지 가시성 변경 감지
  const handleVisibilityChange = useCallback(() => {
    if (!postId || !autoTrack) return;

    if (document.hidden) {
      // 페이지가 숨겨지면 세션 종료
      endReadingSession();
    } else {
      // 페이지가 다시 보이면 세션 시작
      startReadingSession(postId);
    }
  }, [postId, autoTrack]);

  // 스크롤 이벤트 리스너 등록
  useEffect(() => {
    if (!postId || !autoTrack) return;

    window.addEventListener('scroll', handleScroll);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    // 초기 스크롤 위치 확인
    handleScroll();

    return () => {
      window.removeEventListener('scroll', handleScroll);
      document.removeEventListener('visibilitychange', handleVisibilityChange);

      // 세션 종료
      endReadingSession();

      if (scrollTimeoutRef.current) {
        clearTimeout(scrollTimeoutRef.current);
      }
    };
  }, [postId, autoTrack, handleScroll, handleVisibilityChange]);

  // 초기 로드
  useEffect(() => {
    loadData();
  }, [loadData]);

  return {
    // 현재 게시글 관련
    progress,
    isRead,
    readingTime,

    // 전체 데이터
    allProgress,
    stats,
    inProgress,
    recentlyCompleted,
    readPosts: getReadPosts(),

    // 메서드
    saveProgress,
    removeProgress,
    loadData,
  };
}

/**
 * 게시글 목록에서 읽음 상태 표시용 훅
 */
export function usePostReadStatus(postIds: number[]) {
  const [readStatus, setReadStatus] = useState<Record<number, boolean>>({});
  const [progressStatus, setProgressStatus] = useState<Record<number, number>>({});

  useEffect(() => {
    const readPosts = getReadPosts();
    const allProgress = getReadingProgress();

    const status: Record<number, boolean> = {};
    const progress: Record<number, number> = {};

    postIds.forEach(id => {
      status[id] = readPosts.includes(id);
      const postProgress = allProgress.find(p => p.postId === id);
      if (postProgress) {
        progress[id] = postProgress.progress;
      }
    });

    setReadStatus(status);
    setProgressStatus(progress);
  }, [postIds]);

  return { readStatus, progressStatus };
}
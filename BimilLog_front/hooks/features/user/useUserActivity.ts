"use client";

import { useState, useEffect, useMemo, useCallback } from 'react';
import { useMyPageInfo } from '@/hooks/api/useMyPageQueries';
import { usePagination } from '@/hooks/common/usePagination';

// ============ USER ACTIVITY HOOKS ============

// 사용자 활동 탭 조회 - TanStack Query 통합 (마이페이지 통합 API 사용)
export function useUserActivityTabs(pageSize = 10) {
  const [activeTab, setActiveTab] = useState<'my-posts' | 'my-comments' | 'liked-posts' | 'liked-comments'>('my-posts');

  // 페이지네이션
  const pagination = usePagination({ pageSize });

  // 마이페이지 통합 API 호출 (1번의 호출로 모든 활동 데이터 조회)
  const {
    data: mypageData,
    isLoading,
    error,
    refetch,
  } = useMyPageInfo(pagination.currentPage, pagination.pageSize);

  // 각 탭의 실제 데이터 반환
  const myPosts = useMemo(() => {
    return mypageData?.data?.memberActivityPost.writePosts.content || [];
  }, [mypageData]);

  const myComments = useMemo(() => {
    return mypageData?.data?.memberActivityComment.writeComments.content || [];
  }, [mypageData]);

  const likedPosts = useMemo(() => {
    return mypageData?.data?.memberActivityPost.likedPosts.content || [];
  }, [mypageData]);

  const likedComments = useMemo(() => {
    return mypageData?.data?.memberActivityComment.likedComments.content || [];
  }, [mypageData]);

  // 현재 활성 탭의 데이터 반환
  const currentData = useMemo(() => {
    switch (activeTab) {
      case 'my-posts':
        return myPosts;
      case 'my-comments':
        return myComments;
      case 'liked-posts':
        return likedPosts;
      case 'liked-comments':
        return likedComments;
      default:
        return [];
    }
  }, [activeTab, myPosts, myComments, likedPosts, likedComments]);

  // 현재 탭의 totalElements 가져오기
  const currentTotalElements = useMemo(() => {
    if (!mypageData?.data) return 0;

    const { memberActivityPost, memberActivityComment } = mypageData.data;

    switch (activeTab) {
      case 'my-posts':
        return memberActivityPost.writePosts.totalElements;
      case 'my-comments':
        return memberActivityComment.writeComments.totalElements;
      case 'liked-posts':
        return memberActivityPost.likedPosts.totalElements;
      case 'liked-comments':
        return memberActivityComment.likedComments.totalElements;
      default:
        return 0;
    }
  }, [activeTab, mypageData]);

  // setter 안정화 — pagination 객체 전체가 매 렌더 새로 생성되므로,
  // setter 함수 참조만 추출해 effect/callback 의존성으로 사용한다.
  const { setTotalItems, setCurrentPage } = pagination;

  // 데이터 변경 시 페이지네이션 업데이트
  useEffect(() => {
    if (currentTotalElements !== undefined) {
      setTotalItems(currentTotalElements);
    }
  }, [currentTotalElements, setTotalItems]);

  // 탭 변경 시 페이지를 0으로 리셋
  // useCallback 으로 안정화 — UserActivitySection 의 URL→상태 동기화 useEffect 의존성 안정성 확보
  const handleTabChange = useCallback(
    (tab: typeof activeTab) => {
      setActiveTab((prev) => {
        if (prev === tab) return prev;
        return tab;
      });
      setCurrentPage(0);
    },
    [setCurrentPage],
  );

  return {
    // 현재 탭 데이터
    items: currentData,
    isLoading,
    error,
    // B-310: 에러 회복 시 페이지 reload 대신 SPA refetch
    refetch,

    // 탭 상태
    activeTab,
    setActiveTab: handleTabChange,

    // 페이지네이션
    pagination,

    // 각 탭별 개별 데이터 (필요시 사용)
    myPosts,
    myComments,
    likedPosts,
    likedComments,
  };
}

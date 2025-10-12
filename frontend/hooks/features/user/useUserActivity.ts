"use client";

import { useState, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { userQuery } from '@/lib/api';
import { usePagination } from '@/hooks/common/usePagination';
import { queryKeys } from '@/lib/tanstack-query/keys';

// ============ USER ACTIVITY HOOKS ============

// 사용자 활동 탭 조회 - TanStack Query 통합
export function useUserActivityTabs(pageSize = 10) {
  const [activeTab, setActiveTab] = useState<'my-posts' | 'my-comments' | 'liked-posts' | 'liked-comments'>('my-posts');

  // 페이지네이션
  const pagination = usePagination({ pageSize });

  // 작성한 게시글 조회
  const { data: myPostsData, isLoading: myPostsLoading, error: myPostsError } = useQuery({
    queryKey: queryKeys.user.posts(pagination.currentPage, pagination.pageSize),
    queryFn: () => userQuery.getUserPosts(pagination.currentPage, pagination.pageSize),
    enabled: activeTab === 'my-posts',
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000,
  });

  // 작성한 댓글 조회
  const { data: myCommentsData, isLoading: myCommentsLoading, error: myCommentsError } = useQuery({
    queryKey: queryKeys.user.comments(pagination.currentPage, pagination.pageSize),
    queryFn: () => userQuery.getUserComments(pagination.currentPage, pagination.pageSize),
    enabled: activeTab === 'my-comments',
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  // 추천한 게시글 조회
  const { data: likedPostsData, isLoading: likedPostsLoading, error: likedPostsError } = useQuery({
    queryKey: queryKeys.user.likePosts(pagination.currentPage, pagination.pageSize),
    queryFn: () => userQuery.getUserLikedPosts(pagination.currentPage, pagination.pageSize),
    enabled: activeTab === 'liked-posts',
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  // 추천한 댓글 조회
  const { data: likedCommentsData, isLoading: likedCommentsLoading, error: likedCommentsError } = useQuery({
    queryKey: queryKeys.user.likeComments(pagination.currentPage, pagination.pageSize),
    queryFn: () => userQuery.getUserLikedComments(pagination.currentPage, pagination.pageSize),
    enabled: activeTab === 'liked-comments',
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  // 각 탭의 실제 데이터 반환 (활성 탭만 데이터 반환, 비활성 탭은 빈 배열)
  const myPosts = useMemo(() =>
    activeTab === 'my-posts' ? (myPostsData?.data?.content || []) : [],
    [activeTab, myPostsData]
  );
  const myComments = useMemo(() =>
    activeTab === 'my-comments' ? (myCommentsData?.data?.content || []) : [],
    [activeTab, myCommentsData]
  );
  const likedPosts = useMemo(() =>
    activeTab === 'liked-posts' ? (likedPostsData?.data?.content || []) : [],
    [activeTab, likedPostsData]
  );
  const likedComments = useMemo(() =>
    activeTab === 'liked-comments' ? (likedCommentsData?.data?.content || []) : [],
    [activeTab, likedCommentsData]
  );

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

  // 현재 활성 탭의 로딩/에러 상태 반환
  const isLoading = useMemo(() => {
    switch (activeTab) {
      case 'my-posts':
        return myPostsLoading;
      case 'my-comments':
        return myCommentsLoading;
      case 'liked-posts':
        return likedPostsLoading;
      case 'liked-comments':
        return likedCommentsLoading;
      default:
        return false;
    }
  }, [activeTab, myPostsLoading, myCommentsLoading, likedPostsLoading, likedCommentsLoading]);

  const error = useMemo(() => {
    switch (activeTab) {
      case 'my-posts':
        return myPostsError;
      case 'my-comments':
        return myCommentsError;
      case 'liked-posts':
        return likedPostsError;
      case 'liked-comments':
        return likedCommentsError;
      default:
        return null;
    }
  }, [activeTab, myPostsError, myCommentsError, likedPostsError, likedCommentsError]);

  // 현재 탭의 totalElements 가져오기
  const currentTotalElements = useMemo(() => {
    switch (activeTab) {
      case 'my-posts':
        return myPostsData?.data?.totalElements || 0;
      case 'my-comments':
        return myCommentsData?.data?.totalElements || 0;
      case 'liked-posts':
        return likedPostsData?.data?.totalElements || 0;
      case 'liked-comments':
        return likedCommentsData?.data?.totalElements || 0;
      default:
        return 0;
    }
  }, [activeTab, myPostsData, myCommentsData, likedPostsData, likedCommentsData]);

  // 데이터 변경 시 페이지네이션 업데이트
  useEffect(() => {
    if (currentTotalElements !== undefined) {
      pagination.setTotalItems(currentTotalElements);
    }
  }, [currentTotalElements, pagination.setTotalItems]);

  // 탭 변경 시 페이지를 0으로 리셋
  const handleTabChange = (tab: typeof activeTab) => {
    setActiveTab(tab);
    pagination.setCurrentPage(0);
  };

  return {
    // 현재 탭 데이터
    items: currentData,
    isLoading,
    error,

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

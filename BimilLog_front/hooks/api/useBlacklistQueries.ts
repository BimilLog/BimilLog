"use client";

import { useQuery } from '@tanstack/react-query';
import { blacklistQuery } from '@/lib/api/user/blacklist/query';

/**
 * 블랙리스트 조회 hooks
 */

/**
 * 블랙리스트 목록 조회 (페이징)
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 */
export const useBlacklist = (page = 0, size = 20) => {
  return useQuery({
    queryKey: ['user', 'blacklist', page, size],
    queryFn: () => blacklistQuery.getBlacklist(page, size),
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });
};

"use client";

import { useMemo, useEffect } from 'react';
import { useAuth } from '@/hooks';
import { useMyRollingPaper } from '@/hooks/api/useMyRollingPaper';
import { useRollingPaper } from '@/hooks/api/useRollingPaperQueries';
import { localStorage } from '@/lib/utils/storage';
import type { RollingPaperMessage, VisitMessage } from '@/types/domains/paper';

/**
 * 롤링페이퍼 데이터 조회 훅
 * 본인의 롤링페이퍼인지 타인의 롤링페이퍼인지에 따라 적절한 API를 호출
 */
export function useRollingPaperData(targetNickname?: string) {
  const { user, isAuthenticated } = useAuth();

  // 소유자 여부 확인
  const isOwner = useMemo(() => {
    if (!targetNickname) {
      // nickname이 없으면 본인의 롤링페이퍼 페이지
      return true;
    }
    return isAuthenticated && user?.memberName === targetNickname;
  }, [isAuthenticated, user?.memberName, targetNickname]);

  // 조건부로 적절한 쿼리 선택 및 실행
  // 본인 롤링페이퍼 조회 (isOwner일 때만 실행)
  const myPaperQuery = useMyRollingPaper(isOwner);

  // 타인 롤링페이퍼 조회 (!isOwner일 때만 실행)
  const visitPaperQuery = useRollingPaper(targetNickname || '', !isOwner);

  // 적절한 쿼리 선택
  const activeQuery = isOwner ? myPaperQuery : visitPaperQuery;

  // 메시지 타입 정규화
  const messages: (RollingPaperMessage | VisitMessage)[] = useMemo(() => {
    if (!activeQuery.data?.data) return [];
    return activeQuery.data.data;
  }, [activeQuery.data]);

  // 타인의 롤링페이퍼 방문 성공 시 최근 방문 기록에 저장
  useEffect(() => {
    if (!isOwner && targetNickname && visitPaperQuery.isSuccess && visitPaperQuery.data?.success) {
      localStorage.addRecentVisit(targetNickname);
    }
  }, [isOwner, targetNickname, visitPaperQuery.isSuccess, visitPaperQuery.data?.success]);

  return {
    messages,
    isLoading: activeQuery.isLoading,
    isError: activeQuery.isError,
    error: activeQuery.error,
    refetch: activeQuery.refetch,
    isOwner,
  };
}
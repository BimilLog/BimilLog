"use client";

import { useState, useEffect } from "react";
import { logger } from '@/lib/utils/logger';

// ===== ACTIVITY DATA =====
interface PaginatedData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

interface UseActivityDataOptions<T> {
  fetchData: (page?: number, size?: number) => Promise<PaginatedData<T>>;
}

export function useActivityData<T>({ fetchData }: UseActivityDataOptions<T>) {
  const [items, setItems] = useState<T[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  // 활동 데이터 로드: 일반 로드와 더보기(무한 스크롤) 로드를 구분하여 처리
  const loadData = async (page = 0, append = false) => {
    try {
      if (!append) {
        setIsLoading(true);  // 첫 로드 시 전체 로딩 상태
      } else {
        setIsLoadingMore(true);  // 더보기 시 추가 로딩 상태
      }
      setError(null);

      const result = await fetchData(page, 10);

      // append가 true면 기존 데이터에 추가, false면 새로 교체
      if (append) {
        setItems((prev) => [...prev, ...result.content]);
      } else {
        setItems(result.content);
      }

      setCurrentPage(result.currentPage);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (err: unknown) {
      logger.error("Failed to fetch activity data:", err);

      // 에러 타입별로 구체적인 메시지 제공
      let errorMessage = "데이터를 불러오는 중 오류가 발생했습니다.";

      if (err instanceof Error) {
        // 네트워크 에러
        if (err.message.includes("Network") || err.message.includes("fetch")) {
          errorMessage = "인터넷 연결을 확인해주세요.";
        }
        // 타임아웃 에러
        else if (err.message.includes("timeout")) {
          errorMessage = "요청 시간이 초과되었습니다. 다시 시도해주세요.";
        }
        // 서버 에러
        else if (err.message.includes("500") || err.message.includes("Internal Server Error")) {
          errorMessage = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
        // 권한 에러
        else if (err.message.includes("401") || err.message.includes("Unauthorized")) {
          errorMessage = "로그인이 필요합니다. 다시 로그인해주세요.";
        }
        else if (err.message.includes("403") || err.message.includes("Forbidden")) {
          errorMessage = "접근 권한이 없습니다.";
        }
      }

      setError(errorMessage);
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  };

  useEffect(() => {
    loadData(0);
    // fetchData는 useCallback으로 메모화되어 있어 안전함
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 무한 스크롤을 위한 더보기 처리: 다음 페이지가 있을 때만 실행
  const handleLoadMore = () => {
    if (currentPage < totalPages - 1) {
      loadData(currentPage + 1, true);
    }
  };

  // 페이지 직접 변경: 스크롤을 맨 위로 이동하여 UX 개선
  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      loadData(page);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const retry = () => {
    loadData(currentPage);
  };

  // 데스크톱→모바일 전환 시 0페이지부터 현재 페이지까지 모두 로드
  // 무한 스크롤 모드로 전환 시 데이터 불연속 방지
  const loadAllPagesForMobile = async (targetPage: number) => {
    try {
      setIsLoading(true);
      setError(null);

      const allContent: T[] = [];
      let lastResult: PaginatedData<T> | null = null;

      // 0페이지부터 targetPage까지 순차적으로 로드
      for (let page = 0; page <= targetPage; page++) {
        const result = await fetchData(page, 10);
        allContent.push(...result.content);
        lastResult = result;
      }

      // 모든 페이지 데이터를 한 번에 설정
      setItems(allContent);
      setCurrentPage(targetPage);

      // totalPages와 totalElements는 마지막 응답 기준
      if (lastResult) {
        setTotalPages(lastResult.totalPages);
        setTotalElements(lastResult.totalElements);
      }
    } catch (err: unknown) {
      logger.error("Failed to load all pages for mobile:", err);

      // 실패 시 0페이지로 리셋
      setError("데이터를 불러오는 중 오류가 발생했습니다. 페이지를 새로고침합니다.");
      loadData(0);
    } finally {
      setIsLoading(false);
    }
  };

  return {
    items,
    isLoading,
    error,
    currentPage,
    totalPages,
    totalElements,
    isLoadingMore,
    handleLoadMore,
    handlePageChange,
    retry,
    loadAllPagesForMobile,
  };
}

export type { PaginatedData, UseActivityDataOptions };
"use client";

import { useState, useCallback, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks";
import { paperQuery } from "@/lib/api";
import { ErrorHandler } from "@/lib/api/helpers";

interface UseRollingPaperSearchReturn {
  searchNickname: string;
  setSearchNickname: (nickname: string) => void;
  isSearching: boolean;
  searchError: string;
  handleSearch: () => Promise<void>;
  handleKeyPress: (e: React.KeyboardEvent) => void;
  clearError: () => void;
}

/**
 * 롤링페이퍼 검색 및 navigation을 담당하는 Hook
 */
export function useRollingPaperSearch(): UseRollingPaperSearchReturn {
  const { user } = useAuth();
  const router = useRouter();
  const [searchNickname, setSearchNickname] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState("");

  const clearError = useCallback(() => {
    setSearchError("");
  }, []);

  const handleSearch = useCallback(async () => {
    if (!searchNickname.trim()) return;

    const trimmedNickname = searchNickname.trim();
    setIsSearching(true);
    setSearchError("");

    try {
      // 로그인한 사용자의 경우 자신의 닉네임인지 먼저 확인 (API 호출 전)
      if (user && user.userName === trimmedNickname) {
        // 자신의 롤링페이퍼로 리다이렉트
        router.push("/rolling-paper");
        setIsSearching(false);
        return;
      }

      // 다른 사용자의 롤링페이퍼 조회 시도
      const response = await paperQuery.getByUserName(trimmedNickname);

      if (response.success) {
        // 성공적으로 조회된 경우 방문 페이지로 이동
        router.push(`/rolling-paper/${encodeURIComponent(trimmedNickname)}`);
      } else {
        // 사용자를 찾을 수 없는 경우
        const appError = ErrorHandler.mapApiError(new Error(response.error || "사용자를 찾을 수 없습니다"));
        setSearchError(
          appError.userMessage || 
          "해당 닉네임의 롤링페이퍼를 찾을 수 없어요. 회원가입한 사용자의 롤링페이퍼만 존재합니다."
        );
      }
    } catch (error) {
      console.error("Search error:", error);
      const appError = ErrorHandler.mapApiError(error);
      setSearchError(appError.userMessage || "롤링페이퍼를 찾을 수 없어요.");
    } finally {
      setIsSearching(false);
    }
  }, [searchNickname, user, router]);

  const handleKeyPress = useCallback((e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  }, [handleSearch]);

  const setSearchNicknameWithClear = useCallback((nickname: string) => {
    setSearchNickname(nickname);
    if (searchError) {
      setSearchError("");
    }
  }, [searchError]);

  const memoizedReturn = useMemo(() => ({
    searchNickname,
    setSearchNickname: setSearchNicknameWithClear,
    isSearching,
    searchError,
    handleSearch,
    handleKeyPress,
    clearError,
  }), [searchNickname, setSearchNicknameWithClear, isSearching, searchError, handleSearch, handleKeyPress, clearError]);

  return memoizedReturn;
}
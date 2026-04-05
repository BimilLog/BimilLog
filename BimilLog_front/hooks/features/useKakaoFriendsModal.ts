"use client";

import { useState, useEffect, useRef, useMemo } from "react";
import { KakaoFriend } from "@/types/domains/user";
import { logger } from "@/lib/utils/logger";
import { redirectToKakaoConsentOnly } from "@/lib/auth/kakao";
import { useRouter } from "next/navigation";
import { useInfiniteUserFriendList } from "@/hooks/api/useUserQueries";
import { useDebounce } from "@/hooks/common/useDebounce";

export function useKakaoFriendsModal(isOpen: boolean, onClose: () => void) {
  const {
    data,
    isLoading,
    isError,
    error,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    refetch,
  } = useInfiniteUserFriendList(20, isOpen);

  const [needsConsent, setNeedsConsent] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const debouncedSearchQuery = useDebounce(searchQuery, 300);
  const router = useRouter();
  const observerTarget = useRef<HTMLDivElement>(null);

  const allFriends: KakaoFriend[] = useMemo(() => {
    return (
      data?.pages.flatMap((page) =>
        page.success && page.data ? page.data.elements : []
      ) || []
    );
  }, [data?.pages]);

  const filteredFriends = useMemo(() => {
    if (!debouncedSearchQuery.trim()) return allFriends;
    const query = debouncedSearchQuery.toLowerCase();
    return allFriends.filter(
      (friend) =>
        friend.profile_nickname.toLowerCase().includes(query) ||
        (friend.memberName && friend.memberName.toLowerCase().includes(query))
    );
  }, [allFriends, debouncedSearchQuery]);

  const totalCount = data?.pages[0]?.data?.total_count || 0;

  // 카카오 API 에러 분류 처리
  useEffect(() => {
    if (isError && error) {
      const err = error as { message?: string };
      const errMsg = err.message || "친구 목록을 가져올 수 없습니다.";
      const normalizedMessage = errMsg.replace(/[\s.]/g, "");
      const consentKeywords = [
        "카카오친구추가동의를해야합니다",
        "insufficientscopes",
        "friendsconsent",
      ];
      if (consentKeywords.some((keyword) => normalizedMessage.toLowerCase().includes(keyword))) {
        setNeedsConsent(true);
        setErrorMessage("카카오 친구 목록 접근을 위해 추가 동의가 필요합니다.");
      } else {
        setErrorMessage(errMsg);
      }
    }
  }, [isError, error]);

  // 모달이 닫힐 때 상태 초기화
  useEffect(() => {
    if (!isOpen) {
      setSearchQuery("");
      setNeedsConsent(false);
      setErrorMessage(null);
    }
  }, [isOpen]);

  // 무한 스크롤 IntersectionObserver
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 1.0 }
    );
    const target = observerTarget.current;
    if (target) observer.observe(target);
    return () => {
      if (target) observer.unobserve(target);
    };
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const handleConsentClick = () => {
    try {
      const confirmed = window.confirm(
        "카카오 친구 목록 접근 권한을 받기 위해 카카오 동의 페이지로 이동합니다.\n\n" +
          "동의 완료 후 자동으로 이 페이지로 돌아옵니다."
      );
      if (confirmed) {
        redirectToKakaoConsentOnly();
      }
    } catch (err) {
      logger.error("카카오 동의 처리 실패:", err);
      setErrorMessage(
        err instanceof Error
          ? err.message
          : "동의 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
      );
    }
  };

  const handleVisitRollingPaper = (memberName: string) => {
    if (memberName) {
      router.push(`/rolling-paper/${encodeURIComponent(memberName)}`);
      onClose();
    }
  };

  return {
    // 쿼리 상태
    isLoading,
    isFetchingNextPage,
    hasNextPage,
    refetch,
    // 데이터
    allFriends,
    filteredFriends,
    totalCount,
    // 상태
    needsConsent,
    errorMessage,
    searchQuery,
    setSearchQuery,
    debouncedSearchQuery,
    // ref
    observerTarget,
    // 핸들러
    handleConsentClick,
    handleVisitRollingPaper,
  };
}

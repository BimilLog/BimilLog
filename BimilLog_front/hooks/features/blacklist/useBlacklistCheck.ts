"use client";

import { useMemo } from "react";
import { useBlacklist } from "@/hooks/api/useBlacklistQueries";

/**
 * 특정 사용자가 블랙리스트에 있는지 확인하는 hook
 * @param memberName - 확인할 사용자 이름
 * @returns 블랙리스트 여부와 블랙리스트 ID
 */
export const useBlacklistCheck = (memberName: string) => {
  // 전체 블랙리스트 조회 (첫 페이지만, 대부분의 사용자는 많지 않을 것으로 예상)
  const { data: blacklistResponse, isLoading } = useBlacklist(0, 100);

  const result = useMemo(() => {
    if (!blacklistResponse?.data?.content || !memberName) {
      return { isBlacklisted: false, blacklistId: null };
    }

    const foundItem = blacklistResponse.data.content.find(
      (item) => item.memberName === memberName
    );

    return {
      isBlacklisted: !!foundItem,
      blacklistId: foundItem?.id ?? null,
    };
  }, [blacklistResponse, memberName]);

  return {
    ...result,
    isLoading,
  };
};

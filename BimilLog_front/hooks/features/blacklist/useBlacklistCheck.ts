"use client";

import { useMemo } from "react";
import { useBlacklist } from "@/hooks/api/useBlacklistQueries";

/**
 * 특정 사용자가 블랙리스트에 있는지 확인하는 hook
 *
 * 라운드 15 F-15-BUG-15 한계 명시:
 * 첫 페이지 100건만 조회하므로 100명 초과 차단 시 false negative 가능 →
 * UserActionPopover 에서 "블랙리스트 추가" 버튼이 노출돼 사용자가 클릭하면
 * 백엔드 unique 제약 위반(409) 으로 fallback. useAddToBlacklistAction 이
 * 409 응답을 감지해 친화 카피 + "블랙리스트 열기" 액션 토스트로 안내.
 *
 * @param memberName - 확인할 사용자 이름
 * @param enabled - API 호출 활성화 여부 (기본값: true)
 * @returns 블랙리스트 여부와 블랙리스트 ID
 */
export const useBlacklistCheck = (memberName: string, enabled: boolean = true) => {
  // 첫 페이지 100건만 조회 (대부분의 사용자는 100명 미만)
  const { data: blacklistResponse, isLoading } = useBlacklist(0, 100, enabled);

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

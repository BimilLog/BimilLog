"use client";

import { memo } from 'react';
import { Button } from '@/components/atoms/actions/button';

interface LoadMoreButtonProps {
  onClick: () => void;
  isLoading: boolean;
  hasMore: boolean;
}

/**
 * 더보기 버튼 컴포넌트
 * 커서 기반 무한 스크롤에서 다음 페이지를 로드하는 버튼
 *
 * 라운드 6:
 * - 한국어 카피 + 종이/편지 메타포 ("마지막 편지까지 도착했어요")
 * - 토큰화: text-gray-500 → text-muted-foreground (F-209)
 * - aria-busy 로 로딩 상태 announce (WAI-ARIA APG Feed pattern)
 */
export const LoadMoreButton = memo(function LoadMoreButton({
  onClick,
  isLoading,
  hasMore
}: LoadMoreButtonProps) {
  if (!hasMore) {
    return (
      <p
        className="text-center py-8 text-sm text-muted-foreground"
        data-testid="load-more-end"
      >
        마지막 편지까지 도착했어요
      </p>
    );
  }

  return (
    <div className="flex justify-center py-8">
      <Button
        onClick={onClick}
        disabled={isLoading}
        variant="outline"
        aria-busy={isLoading}
        aria-controls="board-list"
        className="min-h-[44px]"
      >
        {isLoading ? '불러오는 중…' : '더보기'}
      </Button>
    </div>
  );
});

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
 */
export const LoadMoreButton = memo(function LoadMoreButton({
  onClick,
  isLoading,
  hasMore
}: LoadMoreButtonProps) {
  if (!hasMore) {
    return (
      <p className="text-center py-8 text-gray-500">
        더 이상 게시글이 없습니다.
      </p>
    );
  }

  return (
    <div className="flex justify-center py-8">
      <Button
        onClick={onClick}
        disabled={isLoading}
        variant="outline"
      >
        {isLoading ? '불러오는 중...' : '더보기'}
      </Button>
    </div>
  );
});

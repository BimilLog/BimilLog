"use client";

import { useCallback } from 'react';
import { useToast } from '@/hooks';
import { logger } from '@/lib/utils/logger';
import { copyRollingPaperLink } from '@/lib/utils/clipboard';
import { getRollingPaperShareData, getRollingPaperShareUrl } from '@/lib/utils/rolling-paper';

interface UseRollingPaperShareProps {
  nickname: string;
  messageCount: number;
  isOwner?: boolean;
}

interface UseRollingPaperShareReturn {
  handleKakaoShare: () => Promise<void>;
  handleWebShare: () => Promise<void>;
  getShareUrl: () => string;
}

/**
 * 롤링페이퍼 공유 기능 훅
 * 카카오톡 공유, 웹 공유 API, 클립보드 복사 등을 처리
 */
export function useRollingPaperShare({
  nickname,
  messageCount,
  isOwner = false,
}: UseRollingPaperShareProps): UseRollingPaperShareReturn {
  const { showSuccess, showError } = useToast();

  // 카카오톡 공유
  const handleKakaoShare = useCallback(async () => {
    if (!nickname) return;

    try {
      const { shareRollingPaper, fallbackShare } = await import('@/lib/auth/kakao');
      const success = await shareRollingPaper(nickname, messageCount);

      if (!success) {
        // 카카오 공유 실패 시 브라우저 내장 공유 API로 폴백
        const url = getRollingPaperShareUrl(nickname);
        fallbackShare(
          url,
          `${nickname}님의 롤링페이퍼`,
          `${nickname}님에게 따뜻한 메시지를 남겨보세요!`
        );
      }
    } catch (error) {
      logger.error('카카오 공유 중 오류 발생:', error);
      // 최종 폴백: 클립보드 복사
      await copyRollingPaperLink(nickname, messageCount);
    }
  }, [nickname, messageCount]);

  // 웹 공유 API 또는 클립보드 복사
  const handleWebShare = useCallback(async () => {
    const shareData = getRollingPaperShareData(nickname, messageCount);

    // 네이티브 공유 API 지원 여부 확인
    if (navigator.share && navigator.canShare && navigator.canShare(shareData)) {
      try {
        await navigator.share(shareData);
        showSuccess('공유 완료', '롤링페이퍼 링크가 공유되었습니다.');
      } catch (error) {
        // 사용자가 공유를 취소한 경우는 무시
        if ((error as Error).name !== 'AbortError') {
          logger.error('공유 실패:', error);
          showError('공유 실패', '공유하기에 실패했습니다.');
        }
      }
    } else {
      // 클립보드 복사 폴백
      try {
        await navigator.clipboard.writeText(shareData.url);
        showSuccess('복사 완료', '링크가 클립보드에 복사되었습니다.');
      } catch {
        showError('복사 실패', '링크 복사에 실패했습니다.');
      }
    }
  }, [nickname, messageCount, showSuccess, showError]);

  // 공유 URL 가져오기
  const getShareUrl = useCallback(() => {
    return getRollingPaperShareUrl(nickname);
  }, [nickname]);

  return {
    handleKakaoShare,
    handleWebShare,
    getShareUrl,
  };
}
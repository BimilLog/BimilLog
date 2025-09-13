"use client";

import { useCallback } from "react";
import { useToast } from "@/hooks/useToast";
import { copyRollingPaperLink } from "@/lib/utils/clipboard";

interface UseRollingPaperShareProps {
  nickname: string;
  messageCount: number;
  isOwner?: boolean;
}

export function useRollingPaperShare({
  nickname,
  messageCount,
  isOwner = false,
}: UseRollingPaperShareProps) {
  const { showSuccess } = useToast();

  const handleKakaoShare = useCallback(async () => {
    if (!nickname) return;

    const { shareRollingPaper, fallbackShare } = await import(
      "@/lib/kakao-share"
    );

    try {
      const success = await shareRollingPaper(nickname, messageCount);
      if (!success) {
        const url = `${
          window.location.origin
        }/rolling-paper/${encodeURIComponent(nickname)}`;
        fallbackShare(
          url,
          `${nickname}님의 롤링페이퍼`,
          `${nickname}님에게 따뜻한 메시지를 남겨보세요!`
        );
      }
    } catch (error) {
      console.error("카카오 공유 중 오류 발생:", error);
      await copyRollingPaperLink(nickname, messageCount);
    }
  }, [nickname, messageCount, showSuccess]);

  const fallbackShare = useCallback(
    () => {
      copyRollingPaperLink(nickname, messageCount);
    },
    [nickname, messageCount]
  );

  const handleWebShare = useCallback(async () => {
    const url = isOwner
      ? `${window.location.origin}/rolling-paper/${encodeURIComponent(
          nickname
        )}`
      : window.location.href;

    const shareData = {
      title: `${nickname}님의 롤링페이퍼`,
      text: `${nickname}님에게 익명으로 따뜻한 메시지를 남겨보세요! 현재 ${messageCount}개의 메시지가 있어요`,
      url: url,
    };

    // 네이티브 공유 API 사용 가능한지 확인
    if (
      navigator.share &&
      navigator.canShare &&
      navigator.canShare(shareData)
    ) {
      try {
        await navigator.share(shareData);
      } catch (error) {
        // 사용자가 공유를 취소한 경우는 무시
        if ((error as Error).name !== "AbortError") {
          console.error("공유 실패:", error);
          fallbackShare();
        }
      }
    } else {
      // 폴백: 클립보드에 복사
      fallbackShare();
    }
  }, [nickname, messageCount, isOwner, fallbackShare]);

  const getShareUrl = useCallback(() => {
    return isOwner
      ? `${window.location.origin}/rolling-paper/${encodeURIComponent(
          nickname
        )}`
      : window.location.href;
  }, [nickname, isOwner]);

  return {
    handleKakaoShare,
    handleWebShare,
    getShareUrl,
  };
}

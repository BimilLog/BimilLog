"use client";

import { useCallback } from "react";

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
      try {
        const url = `${
          window.location.origin
        }/rolling-paper/${encodeURIComponent(nickname)}`;
        await navigator.clipboard.writeText(url);
        alert("링크가 클립보드에 복사되었습니다!");
      } catch (clipboardError) {
        console.error("클립보드 복사 실패:", clipboardError);
      }
    }
  }, [nickname, messageCount]);

  const handleWebShare = useCallback(async () => {
    const url = isOwner
      ? `${window.location.origin}/rolling-paper/${encodeURIComponent(
          nickname
        )}`
      : window.location.href;

    if (navigator.share) {
      try {
        await navigator.share({
          title: `${nickname}님의 롤링페이퍼`,
          text: "익명으로 따뜻한 메시지를 남겨보세요!",
          url: url,
        });
      } catch {
        console.log("Share cancelled");
      }
    } else {
      try {
        await navigator.clipboard.writeText(url);
        alert("링크가 클립보드에 복사되었습니다!");
      } catch (error) {
        console.error("Failed to copy to clipboard:", error);
      }
    }
  }, [nickname, isOwner]);

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

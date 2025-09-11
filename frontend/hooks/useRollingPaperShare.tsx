"use client";

import { useCallback } from "react";
import { useToast } from "@/hooks/useToast";

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
      try {
        const url = `${
          window.location.origin
        }/rolling-paper/${encodeURIComponent(nickname)}`;
        await navigator.clipboard.writeText(url);
        showSuccess("링크 복사 완료", "링크가 클립보드에 복사되었습니다!");
      } catch (clipboardError) {
        console.error("클립보드 복사 실패:", clipboardError);
      }
    }
  }, [nickname, messageCount, showSuccess]);

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
          fallbackShare(url);
        }
      }
    } else {
      // 폴백: 클립보드에 복사
      fallbackShare(url);
    }
  }, [nickname, messageCount, isOwner, fallbackShare]);

  const fallbackShare = useCallback(
    (url: string) => {
      navigator.clipboard
        .writeText(url)
        .then(() => {
          showSuccess("링크 복사 완료", "링크가 클립보드에 복사되었습니다!");
        })
        .catch((error) => {
          console.error("클립보드 복사 실패:", error);
          // 클립보드 API도 실패한 경우 텍스트 선택으로 폴백
          const textArea = document.createElement("textarea");
          textArea.value = url;
          document.body.appendChild(textArea);
          textArea.select();
          try {
            document.execCommand("copy");
            showSuccess("링크 복사 완료", "링크가 클립보드에 복사되었습니다!");
          } catch (fallbackError) {
            console.error("폴백 복사도 실패:", fallbackError);
          }
          document.body.removeChild(textArea);
        });
    },
    [showSuccess]
  );

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

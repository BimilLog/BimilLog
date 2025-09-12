"use client";

import { useState, useCallback, useMemo } from "react";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";

interface UseMessagePositionProps {
  messages: (RollingPaperMessage | VisitMessage)[];
  onPositionSelect?: (position: { x: number; y: number }) => void;
}

interface UseMessagePositionReturn {
  highlightedPosition: { x: number; y: number } | null;
  setHighlightedPosition: (position: { x: number; y: number } | null) => void;
  clearHighlight: () => void;
  handlePositionClick: (x: number, y: number) => void;
}

/**
 * 메시지 위치 관련 상태와 로직을 담당하는 Hook
 */
export function useMessagePosition({
  messages,
  onPositionSelect,
}: UseMessagePositionProps): UseMessagePositionReturn {
  const [highlightedPosition, setHighlightedPosition] = useState<{
    x: number;
    y: number;
  } | null>(null);

  const clearHighlight = useCallback(() => {
    setHighlightedPosition(null);
  }, []);

  const handlePositionClick = useCallback(
    (x: number, y: number) => {
      const position = { x, y };
      
      // 해당 위치에 메시지가 있는지 확인
      const hasMessage = messages.some(
        (message) => message.x === x + 1 && message.y === y + 1 // 백엔드는 1-based
      );

      if (hasMessage) {
        // 메시지가 있으면 하이라이트
        setHighlightedPosition(position);
      } else {
        // 메시지가 없으면 위치 선택 콜백 호출
        onPositionSelect?.(position);
      }
    },
    [messages, onPositionSelect]
  );

  const memoizedReturn = useMemo(() => ({
    highlightedPosition,
    setHighlightedPosition,
    clearHighlight,
    handlePositionClick,
  }), [highlightedPosition, setHighlightedPosition, clearHighlight, handlePositionClick]);

  return memoizedReturn;
}
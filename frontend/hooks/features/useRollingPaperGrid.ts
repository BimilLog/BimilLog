"use client";

import { useMemo, useCallback } from 'react';
import {
  createMessageGrid,
  calculateTotalPages,
  getAbsoluteCoords,
  isMobileDevice,
  type GridPosition,
} from '@/lib/utils/rolling-paper';
import type { RollingPaperMessage, VisitMessage } from '@/types/domains/paper';

interface UseRollingPaperGridProps {
  messages: (RollingPaperMessage | VisitMessage)[];
}

interface UseRollingPaperGridReturn {
  gridData: (RollingPaperMessage | VisitMessage | null)[][];
  totalPages: number;
  isMobile: boolean;
  getMessageAt: (x: number, y: number) => RollingPaperMessage | VisitMessage | null;
  getCoordsFromPageAndGrid: (page: number, gridX: number, gridY: number) => GridPosition;
}

/**
 * 롤링페이퍼 그리드 관리 훅
 * 메시지 그리드 생성, 페이지 계산, 좌표 변환 등의 로직을 담당
 */
export function useRollingPaperGrid({
  messages,
}: UseRollingPaperGridProps): UseRollingPaperGridReturn {
  // 모바일 체크
  const isMobile = useMemo(() => isMobileDevice(), []);

  // 그리드 데이터 생성
  const gridData = useMemo(() => {
    return createMessageGrid(messages);
  }, [messages]);

  // 전체 페이지 수 계산
  const totalPages = useMemo(() => {
    return calculateTotalPages(messages.length);
  }, [messages.length]);

  // 특정 위치의 메시지 가져오기 (1-based 좌표)
  const getMessageAt = useCallback(
    (x: number, y: number): RollingPaperMessage | VisitMessage | null => {
      const rowIndex = y - 1; // y 좌표를 행 인덱스로 변환
      const colIndex = x - 1; // x 좌표를 열 인덱스로 변환

      if (gridData[rowIndex] && gridData[rowIndex][colIndex]) {
        return gridData[rowIndex][colIndex];
      }
      return null;
    },
    [gridData]
  );

  // 페이지와 그리드 위치에서 실제 좌표 계산
  const getCoordsFromPageAndGrid = useCallback(
    (page: number, gridX: number, gridY: number): GridPosition => {
      return getAbsoluteCoords(page, gridX, gridY);
    },
    []
  );

  return {
    gridData,
    totalPages,
    isMobile,
    getMessageAt,
    getCoordsFromPageAndGrid,
  };
}
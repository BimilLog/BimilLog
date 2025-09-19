"use client";

import { useMemo, useCallback, useEffect, useState } from 'react';
import {
  createMessageGrid,
  calculateTotalPages,
  getAbsoluteCoords,
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
  // 모바일 체크 - SSR 호환성을 위해 초기값은 false, 클라이언트에서 실제 크기 측정
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkMobile(); // 초기 실행
    window.addEventListener('resize', checkMobile);

    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  // 그리드 데이터 생성
  const gridData = useMemo(() => {
    return createMessageGrid(messages, isMobile);
  }, [messages, isMobile]);

  // 전체 페이지 수 계산 (고정값)
  const totalPages = useMemo(() => {
    return calculateTotalPages(isMobile);
  }, [isMobile]);

  // 특정 위치의 메시지 가져오기 (0-based 좌표)
  const getMessageAt = useCallback(
    (x: number, y: number): RollingPaperMessage | VisitMessage | null => {
      // 이미 0-based이므로 그대로 사용
      if (gridData[y] && gridData[y][x]) {
        return gridData[y][x];
      }
      return null;
    },
    [gridData]
  );

  // 페이지와 그리드 위치에서 실제 좌표 계산
  const getCoordsFromPageAndGrid = useCallback(
    (page: number, gridX: number, gridY: number): GridPosition => {
      return getAbsoluteCoords(page, gridX, gridY, isMobile);
    },
    [isMobile]
  );

  return {
    gridData,
    totalPages,
    isMobile,
    getMessageAt,
    getCoordsFromPageAndGrid,
  };
}
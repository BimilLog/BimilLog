"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import {
  rollingPaperApi,
  type RollingPaperMessage,
  type VisitMessage,
} from "@/lib/api";

interface UseRollingPaperProps {
  nickname?: string;
  isPublic?: boolean;
}

interface UseRollingPaperReturn {
  messages: (RollingPaperMessage | VisitMessage)[];
  messageCount: number;
  recentMessages: (RollingPaperMessage | VisitMessage)[];
  isLoading: boolean;
  isOwner: boolean;
  isMobile: boolean;
  // 좌표 기반 시스템
  canvasWidth: number;
  canvasHeight: number;
  totalPages: number;
  // 페이지네이션
  currentPage: number;
  setCurrentPage: (page: number) => void;
  refetchMessages: () => Promise<void>;
  // 좌표 유틸리티
  isPositionOccupied: (x: number, y: number) => boolean;
  getMessageAt: (
    x: number,
    y: number
  ) => RollingPaperMessage | VisitMessage | null;
  findEmptyPosition: () => { x: number; y: number } | null;
  findNearbyEmptyPosition: (
    centerX: number,
    centerY: number,
    radius?: number
  ) => { x: number; y: number } | null;
  // 좌표 변환 함수
  getPageFromCoords: (x: number, y: number) => number;
  getGridPosFromCoords: (
    x: number,
    y: number
  ) => { gridX: number; gridY: number };
  getCoordsFromPageAndGrid: (
    page: number,
    gridX: number,
    gridY: number
  ) => { x: number; y: number };
}

export function useRollingPaper({
  nickname,
  isPublic = false,
}: UseRollingPaperProps = {}): UseRollingPaperReturn {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const [messages, setMessages] = useState<
    (RollingPaperMessage | VisitMessage)[]
  >([]);
  const [messageCount, setMessageCount] = useState(0);
  const [recentMessages, setRecentMessages] = useState<
    (RollingPaperMessage | VisitMessage)[]
  >([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isOwner, setIsOwner] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);

  // 모바일 감지
  useEffect(() => {
    const checkIsMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkIsMobile();
    window.addEventListener("resize", checkIsMobile);
    return () => window.removeEventListener("resize", checkIsMobile);
  }, []);

  // 좌표 기반 설정
  const pageWidth = isMobile ? 4 : 6; // 페이지당 가로 칸 수
  const pageHeight = 10; // 페이지당 세로 칸 수 (고정)
  const maxPages = isMobile ? 3 : 2; // 최대 페이지 수
  const canvasWidth = pageWidth * maxPages; // 전체 캔버스 가로 크기 (PC: 12, 모바일: 12)
  const canvasHeight = pageHeight; // 전체 캔버스 세로 크기 (고정 10)
  const totalPages = maxPages;

  // 좌표 변환 함수들
  const getPageFromCoords = (x: number, y: number): number => {
    // width를 기준으로 페이지 계산
    return Math.floor(x / pageWidth) + 1;
  };

  const getGridPosFromCoords = (
    x: number,
    y: number
  ): { gridX: number; gridY: number } => {
    // 백엔드 좌표를 그리드 내 위치로 변환
    const gridX = x % pageWidth;
    const gridY = y;
    return { gridX, gridY };
  };

  const getCoordsFromPageAndGrid = (
    page: number,
    gridX: number,
    gridY: number
  ): { x: number; y: number } => {
    // 페이지와 그리드 위치를 백엔드 좌표로 변환
    const x = (page - 1) * pageWidth + gridX;
    const y = gridY;
    return { x, y };
  };

  // 좌표 유틸리티 함수들
  const isPositionOccupied = (x: number, y: number): boolean => {
    return messages.some(
      (message) => message.width === x && message.height === y
    );
  };

  const getMessageAt = (
    x: number,
    y: number
  ): RollingPaperMessage | VisitMessage | null => {
    return (
      messages.find((message) => message.width === x && message.height === y) ||
      null
    );
  };

  // 빈 좌표 찾기 유틸리티
  const findEmptyPosition = (): { x: number; y: number } | null => {
    // 랜덤하게 빈 좌표를 찾아서 반환
    const maxAttempts = 100;
    for (let i = 0; i < maxAttempts; i++) {
      const x = Math.floor(Math.random() * canvasWidth);
      const y = Math.floor(Math.random() * canvasHeight);
      if (!isPositionOccupied(x, y)) {
        return { x, y };
      }
    }

    // 랜덤으로 못 찾으면 순차적으로 찾기
    for (let y = 0; y < canvasHeight; y++) {
      for (let x = 0; x < canvasWidth; x++) {
        if (!isPositionOccupied(x, y)) {
          return { x, y };
        }
      }
    }

    return null; // 모든 좌표가 차있음
  };

  // 인근 빈 좌표 찾기
  const findNearbyEmptyPosition = (
    centerX: number,
    centerY: number,
    radius: number = 3
  ): { x: number; y: number } | null => {
    // 중심점 주변의 빈 좌표를 찾기
    for (let r = 1; r <= radius; r++) {
      for (let dx = -r; dx <= r; dx++) {
        for (let dy = -r; dy <= r; dy++) {
          if (Math.abs(dx) === r || Math.abs(dy) === r) {
            // 원의 경계선만 체크
            const x = centerX + dx;
            const y = centerY + dy;
            if (
              x >= 0 &&
              x < canvasWidth &&
              y >= 0 &&
              y < canvasHeight &&
              !isPositionOccupied(x, y)
            ) {
              return { x, y };
            }
          }
        }
      }
    }
    return findEmptyPosition(); // 인근에 없으면 아무데나
  };

  // 소유자 확인
  useEffect(() => {
    if (authLoading) {
      return; // 인증 정보 로딩 중에는 아무것도 하지 않음
    }

    if (!isPublic) {
      // 내 롤링페이퍼 페이지 (/rolling-paper)
      setIsOwner(isAuthenticated);
    } else {
      // 다른 사람의 롤링페이퍼 페이지 (/[nickname])
      if (isAuthenticated && user && nickname) {
        setIsOwner(user.userName === decodeURIComponent(nickname));
      } else {
        // 비로그인 상태에서는 절대 소유자가 아님
        setIsOwner(false);
      }
    }
  }, [isAuthenticated, user, nickname, isPublic, authLoading]);

  // 메시지 가져오기 함수
  const fetchMessages = async () => {
    if (isPublic && !nickname) return;
    if (!isPublic && (!isAuthenticated || !user)) return;

    try {
      let response;

      if (isPublic) {
        // 공개 롤링페이퍼
        const currentNickname = decodeURIComponent(nickname!);
        const isOwnerCheck =
          isAuthenticated && user && user.userName === currentNickname;

        if (isOwnerCheck) {
          response = await rollingPaperApi.getMyRollingPaper();
        } else {
          response = await rollingPaperApi.getRollingPaper(currentNickname);
        }
      } else {
        // 내 롤링페이퍼
        response = await rollingPaperApi.getMyRollingPaper();
      }

      if (response.success && response.data) {
        // 생성일 기준으로 정렬 (최신순)
        const sortedMessages = response.data.sort((a: any, b: any) => {
          return (
            new Date(b.createdAt || 0).getTime() -
            new Date(a.createdAt || 0).getTime()
          );
        });

        setMessages(sortedMessages);
        setMessageCount(sortedMessages.length);
        // 최신순 상위 3개를 최근 메시지로 설정
        setRecentMessages(sortedMessages.slice(0, 3));
      }
    } catch (error) {
      console.error("Failed to fetch messages:", error);
    } finally {
      setIsLoading(false);
    }
  };

  // 메시지 가져오기
  useEffect(() => {
    if (!authLoading) {
      fetchMessages();
    }
  }, [isAuthenticated, user, nickname, authLoading]);

  return {
    messages,
    messageCount,
    recentMessages,
    isLoading: isLoading || authLoading,
    isOwner,
    isMobile,
    // 좌표 기반 시스템
    canvasWidth,
    canvasHeight,
    totalPages,
    // 페이지네이션
    currentPage,
    setCurrentPage,
    refetchMessages: fetchMessages,
    // 좌표 유틸리티
    isPositionOccupied,
    getMessageAt,
    findEmptyPosition,
    findNearbyEmptyPosition,
    // 좌표 변환 함수
    getPageFromCoords,
    getGridPosFromCoords,
    getCoordsFromPageAndGrid,
  };
}

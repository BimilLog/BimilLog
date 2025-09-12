"use client";

import { useState, useEffect, useMemo } from "react";
import { useAuth } from "@/hooks/useAuth";
import { useDevice } from "@/hooks/useDevice";
import { paperQuery } from "@/lib/api";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import {
  getCoordinateConfig,
  createMessagePositionMap,
  isPositionOccupied as checkPositionOccupied,
  getMessageAt as getMessageAtPosition,
  findEmptyPosition as findEmpty,
  findNearbyEmptyPosition as findNearbyEmpty,
  frontendToBackend,
  backendToFrontend,
  getPageFromCoords as getPage,
  getGridPosFromCoords as getGridPos,
  getCoordsFromPageAndGrid as getCoordsFromGrid,
} from "@/lib/utils/coordinate";

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
  // 좌표 변환 함수 (백엔드 ↔ 프론트엔드)
  frontendToBackend: (x: number, y: number) => { x: number; y: number };
  backendToFrontend: (x: number, y: number) => { x: number; y: number };
  // 페이지 좌표 변환 함수
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
  const [currentPage, setCurrentPage] = useState(1);
  const { isMobile } = useDevice();

  // 좌표 기반 설정
  const config = getCoordinateConfig(isMobile);
  const { pageWidth, pageHeight, canvasWidth, canvasHeight, totalPages } = config;

  // 좌표 변환 함수들
  const getPageFromCoords = (x: number, y: number): number => {
    return getPage(x, y, pageWidth);
  };

  const getGridPosFromCoords = (
    x: number,
    y: number
  ): { gridX: number; gridY: number } => {
    return getGridPos(x, y, pageWidth);
  };

  const getCoordsFromPageAndGrid = (
    page: number,
    gridX: number,
    gridY: number
  ): { x: number; y: number } => {
    return getCoordsFromGrid(page, gridX, gridY, pageWidth);
  };

  // 메시지 위치 Map 생성 (성능 최적화)
  const messagePositionMap = useMemo(() => {
    return createMessagePositionMap(messages);
  }, [messages]);

  // 좌표 유틸리티 함수들
  const isPositionOccupied = (x: number, y: number): boolean => {
    return checkPositionOccupied(x, y, messagePositionMap);
  };

  const getMessageAt = (
    x: number,
    y: number
  ): RollingPaperMessage | VisitMessage | null => {
    return getMessageAtPosition(x, y, messagePositionMap);
  };

  const findEmptyPosition = (): { x: number; y: number } | null => {
    return findEmpty(canvasWidth, canvasHeight, messagePositionMap);
  };

  const findNearbyEmptyPosition = (
    centerX: number,
    centerY: number,
    radius: number = 3
  ): { x: number; y: number } | null => {
    return findNearbyEmpty(centerX, centerY, canvasWidth, canvasHeight, messagePositionMap, radius);
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
          response = await paperQuery.getMy();
        } else {
          response = await paperQuery.getByUserName(currentNickname);
        }
      } else {
        // 내 롤링페이퍼
        response = await paperQuery.getMy();
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
    // 좌표 변환 함수 (백엔드 ↔ 프론트엔드)
    frontendToBackend,
    backendToFrontend,
    // 페이지 좌표 변환 함수
    getPageFromCoords,
    getGridPosFromCoords,
    getCoordsFromPageAndGrid,
  };
}

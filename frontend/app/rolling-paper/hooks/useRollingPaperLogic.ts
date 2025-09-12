"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { useRollingPaper } from "@/hooks/useRollingPaper";
import { useRollingPaperShare } from "@/hooks/useRollingPaperShare";
import { useMessagePosition } from "@/hooks/useMessagePosition";
import { useToast } from "@/hooks/useToast";
import { paperCommand } from "@/lib/api";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import { addRecentVisit } from "@/lib/cookies";
import { ErrorHandler } from "@/lib/error-handler";

interface UseRollingPaperLogicProps {
  nickname?: string;
}

export function useRollingPaperLogic({ nickname }: UseRollingPaperLogicProps) {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const { showSuccess, showError, toasts, removeToast } = useToast();
  const isPublic = !!nickname;
  const targetNickname = nickname
    ? decodeURIComponent(nickname)
    : user?.userName || "";

  const [isMessageListOpen, setIsMessageListOpen] = useState(false);

  const {
    messages,
    messageCount,
    recentMessages,
    isLoading,
    isOwner,
    isMobile,
    totalPages,
    currentPage,
    setCurrentPage,
    refetchMessages,
    isPositionOccupied,
    getMessageAt,
    getCoordsFromPageAndGrid,
    getPageFromCoords,
    frontendToBackend,
    backendToFrontend,
  } = useRollingPaper({
    nickname,
    isPublic,
  });

  const { handleWebShare } = useRollingPaperShare({
    nickname: targetNickname,
    messageCount,
    isOwner,
  });

  const {
    highlightedPosition,
    setHighlightedPosition,
    clearHighlight,
    handlePositionClick,
  } = useMessagePosition({
    messages,
    onPositionSelect: !isOwner ? (position) => {
      // 메시지 작성을 위한 위치 선택 (방문자용)
      // 실제 구현은 RollingPaperGrid에서 처리
    } : undefined,
  });

  // 인증 체크 (내 롤링페이퍼인 경우)
  useEffect(() => {
    if (!isPublic && !authLoading && !isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, authLoading, router, isPublic]);

  // 소유자 리다이렉트 (공개 롤링페이퍼인 경우)
  useEffect(() => {
    if (isPublic && nickname && isAuthenticated && user) {
      const currentNickname = decodeURIComponent(nickname);
      
      if (user.userName === currentNickname) {
        router.push("/rolling-paper");
        return;
      }
    }
  }, [isAuthenticated, user, nickname, isPublic, router]);

  // 방문 기록 저장 (다른 사람의 롤링페이퍼인 경우)
  useEffect(() => {
    if (isPublic && nickname && !authLoading) {
      const currentNickname = decodeURIComponent(nickname);
      const isOwner = isAuthenticated && user?.userName === currentNickname;

      if (!isOwner) {
        addRecentVisit(nickname);
      }
    }
  }, [nickname, isAuthenticated, user, isPublic, authLoading]);

  // 메시지 작성 핸들러
  const handleMessageSubmit = useCallback(async (
    position: { x: number; y: number },
    data: any
  ) => {
    if (!isPublic || !nickname) return;

    // 위치가 이미 사용 중인지 확인
    if (isPositionOccupied(position.x, position.y)) {
      showError(
        "위치 중복",
        "이미 다른 메시지가 있는 위치입니다. 다른 위치를 선택해주세요."
      );
      return;
    }

    try {
      // 프론트엔드 좌표를 백엔드 좌표로 변환 (0-based → 1-based)
      const backendPosition = frontendToBackend(position.x, position.y);
      
      const response = await paperCommand.createMessage(
        decodeURIComponent(nickname),
        {
          decoType: data.decoType,
          anonymity: data.anonymousNickname,
          content: data.content,
          x: backendPosition.x,
          y: backendPosition.y,
        }
      );

      if (response.success) {
        await refetchMessages();
        showSuccess("메시지 전송 성공", "메시지가 성공적으로 작성되었습니다!");
      } else {
        showError(
          "메시지 전송 실패",
          "메시지 작성에 실패했습니다. 다시 시도해주세요."
        );
      }
    } catch (error) {
      console.error("Failed to create message:", error);
      const appError = ErrorHandler.mapApiError(error);
      showError(
        "메시지 전송 실패",
        appError.userMessage || "메시지 작성에 실패했습니다. 다시 시도해주세요."
      );
    }
  }, [isPublic, nickname, isPositionOccupied, frontendToBackend, refetchMessages, showSuccess, showError]);

  // 메시지 클릭 핸들러 (그리드 하이라이트)
  const handleMessageClick = useCallback((message: RollingPaperMessage | VisitMessage) => {
    // 백엔드 좌표를 프론트엔드 좌표로 변환 (1-based → 0-based)
    const frontendPosition = backendToFrontend(message.x, message.y);
    setHighlightedPosition(frontendPosition);

    // 해당 메시지가 있는 페이지로 이동 (프론트엔드 좌표 사용)
    const messagePage = getPageFromCoords(frontendPosition.x, frontendPosition.y);
    if (messagePage !== currentPage) {
      setCurrentPage(messagePage);
    }

    // 3초 후 하이라이트 제거
    setTimeout(() => {
      clearHighlight();
    }, 3000);
  }, [backendToFrontend, setHighlightedPosition, getPageFromCoords, currentPage, setCurrentPage, clearHighlight]);

  return {
    // 상태
    targetNickname,
    isPublic,
    isLoading: isLoading || authLoading,
    isOwner,
    isMobile,
    messages,
    messageCount,
    recentMessages,
    totalPages,
    currentPage,
    setCurrentPage,
    
    // UI 상태
    isMessageListOpen,
    setIsMessageListOpen,
    
    // 좌표 관련
    highlightedPosition,
    clearHighlight,
    getMessageAt,
    getCoordsFromPageAndGrid,
    getPageFromCoords,
    frontendToBackend,
    backendToFrontend,
    
    // 핸들러
    handleWebShare,
    handleMessageSubmit,
    handleMessageClick,
    refetchMessages,
    
    // Toast
    toasts,
    removeToast,
    showSuccess,
    showError,
    
    // 인증 관련
    user,
    isAuthenticated,
  };
}
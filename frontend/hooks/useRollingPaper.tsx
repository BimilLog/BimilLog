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
  messages: { [key: number]: RollingPaperMessage | VisitMessage };
  messageCount: number;
  recentMessages: (RollingPaperMessage | VisitMessage)[];
  isLoading: boolean;
  isOwner: boolean;
  isMobile: boolean;
  currentPage: number;
  totalPages: number;
  colsPerPage: number;
  rowsPerPage: number;
  slotsPerPage: number;
  setCurrentPage: (page: number) => void;
  refetchMessages: () => Promise<void>;
}

export function useRollingPaper({
  nickname,
  isPublic = false,
}: UseRollingPaperProps = {}): UseRollingPaperReturn {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const [messages, setMessages] = useState<{
    [key: number]: RollingPaperMessage | VisitMessage;
  }>({});
  const [messageCount, setMessageCount] = useState(0);
  const [recentMessages, setRecentMessages] = useState<
    (RollingPaperMessage | VisitMessage)[]
  >([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isOwner, setIsOwner] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);

  // 반응형 설정
  const totalPages = isMobile ? 3 : 2;
  const colsPerPage = isMobile ? 4 : 6;
  const rowsPerPage = 10;
  const slotsPerPage = colsPerPage * rowsPerPage;

  // 모바일 감지
  useEffect(() => {
    const checkIsMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkIsMobile();
    window.addEventListener("resize", checkIsMobile);
    return () => window.removeEventListener("resize", checkIsMobile);
  }, []);

  // 소유자 확인
  useEffect(() => {
    if (isAuthenticated && user && nickname) {
      const isOwnerCheck = user.userName === decodeURIComponent(nickname);
      setIsOwner(isOwnerCheck);
    } else if (!isPublic && isAuthenticated && user) {
      setIsOwner(true);
    }
  }, [isAuthenticated, user, nickname, isPublic]);

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
        setMessageCount(response.data.length);
        setRecentMessages(response.data.slice(0, 3));

        const messageMap: {
          [key: number]: RollingPaperMessage | VisitMessage;
        } = {};

        response.data.forEach((message: RollingPaperMessage | VisitMessage) => {
          const pageWidth = message.width % colsPerPage;
          const page = Math.floor(message.width / colsPerPage) + 1;
          const position = message.height * colsPerPage + pageWidth;

          const messageWithPage = { ...message, page };
          messageMap[position] = messageWithPage;
        });

        setMessages(messageMap);
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
  }, [isAuthenticated, user, nickname, colsPerPage, authLoading]);

  return {
    messages,
    messageCount,
    recentMessages,
    isLoading: isLoading || authLoading,
    isOwner,
    isMobile,
    currentPage,
    totalPages,
    colsPerPage,
    rowsPerPage,
    slotsPerPage,
    setCurrentPage,
    refetchMessages: fetchMessages,
  };
}

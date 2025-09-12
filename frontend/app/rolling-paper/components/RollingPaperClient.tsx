"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { MessageSquare } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { useRollingPaper } from "@/hooks/useRollingPaper";
import { useRollingPaperShare } from "@/hooks/useRollingPaperShare";
import { useToast } from "@/hooks/useToast";
import {
  rollingPaperApi,
  type RollingPaperMessage,
  type VisitMessage,
} from "@/lib/api";
import { addRecentVisit } from "@/lib/cookies";
import { ErrorHandler } from "@/lib/error-handler";
import { RollingPaperLayout } from "./RollingPaperLayout";
import { RollingPaperHeader } from "./RollingPaperHeader";
import { InfoCard } from "./InfoCard";
import { RollingPaperGrid } from "./RollingPaperGrid";
import { RecentMessages } from "./RecentMessages";
import { MessageListModal } from "./MessageListModal";
import { ResponsiveAdFitBanner } from "@/components";
import { Button } from "@/components/ui/button";
import { ToastContainer } from "@/components/molecules/toast";

interface RollingPaperClientProps {
  nickname?: string; // 공개 롤링페이퍼인 경우
}

export const RollingPaperClient: React.FC<RollingPaperClientProps> = ({
  nickname,
}) => {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const { showSuccess, showError, toasts, removeToast } = useToast();
  const isPublic = !!nickname;
  const targetNickname = nickname
    ? decodeURIComponent(nickname)
    : user?.userName || "";

  // 메시지 목록 모달 상태
  const [isMessageListOpen, setIsMessageListOpen] = useState(false);
  const [highlightedPosition, setHighlightedPosition] = useState<{
    x: number;
    y: number;
  } | null>(null);

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
  const handleMessageSubmit = async (
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
      
      const response = await rollingPaperApi.createMessage(
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
  };

  // 메시지 클릭 핸들러 (그리드 하이라이트)
  const handleMessageClick = (message: RollingPaperMessage | VisitMessage) => {
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
      setHighlightedPosition(null);
    }, 3000);
  };

  // 하이라이트 제거
  const clearHighlight = () => {
    setHighlightedPosition(null);
  };

  // 로딩 상태
  if (isLoading || authLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50 flex items-center justify-center px-4">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-gray-600 font-medium">
            롤링페이퍼를 불러오는 중...
          </p>
        </div>
      </div>
    );
  }

  // 인증되지 않은 상태 (내 롤링페이퍼인 경우)
  if (!isPublic && (!isAuthenticated || !user)) {
    return null;
  }

  return (
    <RollingPaperLayout
      adPosition={isPublic ? "타인 롤링페이퍼 상단" : "내 롤링페이퍼 상단"}
    >
      {/* 헤더 */}
      <RollingPaperHeader
        nickname={targetNickname}
        messageCount={messageCount}
        isOwner={isOwner}
        onShowMessages={isOwner ? () => setIsMessageListOpen(true) : undefined}
        className="sticky top-[68px] sm:top-[80px] z-40"
      />

      <div className="container mx-auto px-2 md:px-4">
        {/* 헤더 하단 광고 */}
        <div className="py-6">
          <div className="flex justify-center">
            <ResponsiveAdFitBanner
              position={
                isPublic ? "타인 롤링페이퍼 상단" : "내 롤링페이퍼 상단"
              }
              className="max-w-full"
            />
          </div>
        </div>

        {/* 정보 카드 */}
        <div className="mb-8">
          <InfoCard isOwner={isOwner} nickname={targetNickname} />
        </div>

        {/* 롤링페이퍼 그리드 */}
        <div className="mb-8">
          <RollingPaperGrid
            messages={messages}
            nickname={targetNickname}
            isOwner={isOwner}
            isMobile={isMobile}
            totalPages={totalPages}
            currentPage={currentPage}
            setCurrentPage={setCurrentPage}
            onMessageSubmit={!isOwner ? handleMessageSubmit : undefined}
            getMessageAt={getMessageAt}
            getCoordsFromPageAndGrid={getCoordsFromPageAndGrid}
            highlightedPosition={highlightedPosition}
            onHighlightClear={clearHighlight}
            onSuccess={(message) => showSuccess("성공", message)}
            onError={(message) => showError("오류", message)}
            onRefresh={refetchMessages}
          />
        </div>

        {/* 최근 메시지들 - 소유자만 볼 수 있음 */}
        {isOwner && (
          <RecentMessages
            messages={recentMessages}
            isOwner={isOwner}
            onShare={handleWebShare}
            onMessageClick={handleMessageClick}
          />
        )}
      </div>

      {/* 메시지 목록 모달 */}
      <MessageListModal
        isOpen={isMessageListOpen}
        onClose={() => setIsMessageListOpen(false)}
        messages={messages.filter(
          (msg): msg is RollingPaperMessage =>
            "content" in msg && "createdAt" in msg
        )}
        onMessageClick={handleMessageClick}
      />

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </RollingPaperLayout>
  );
};

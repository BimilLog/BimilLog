"use client";

import React, { useState, useCallback } from "react";
import { MessageSquare, Heart, Search, LogIn, User as UserIcon } from "lucide-react";
import { useRollingPaperData } from "@/hooks/features/useRollingPaperData";
import { useRollingPaperActions } from "@/hooks/features/useRollingPaper";
import { useRollingPaperGrid } from "@/hooks/features/useRollingPaperGrid";
import { useToast, useAuth } from "@/hooks";
import { RollingPaperView } from "@/components/organisms/rolling-paper/RollingPaperView";
import { NotFoundView } from "@/components/molecules/feedback";
import type { RollingPaperMessage, VisitMessage, VisitPaperResult, DecoType } from "@/types/domains/paper";
import { BlockedToastRedirect } from "@/components/molecules/alerts/BlockedToastRedirect";

interface RollingPaperContainerProps {
  nickname?: string;
  initialPaperData?: VisitPaperResult;
}

export const RollingPaperContainer: React.FC<RollingPaperContainerProps> = React.memo(({
  nickname,
  initialPaperData,
}) => {
  const targetNickname = nickname || "";
  const { toasts, removeToast, showSuccess, showError } = useToast();
  const { isAuthenticated } = useAuth();

  // 롤링페이퍼 데이터 조회 (본인/타인 구분)
  const {
    messages,
    ownerId,
    isLoading,
    isError,
    blockedMessage,
    isOwner,
    refetch
  } = useRollingPaperData(nickname, initialPaperData);

  // 롤링페이퍼 액션 (작성, 선택)
  const {
    handleCreateMessage,
    toggleMessageSelection
  } = useRollingPaperActions(targetNickname);

  // 그리드 관련 로직
  const {
    totalPages,
    isMobile,
    getMessageAt,
    getCoordsFromPageAndGrid
  } = useRollingPaperGrid({ messages });

  // UI 상태
  const [currentPage, setCurrentPage] = useState(1);
  const [isMessageListOpen, setIsMessageListOpen] = useState(false);
  const [highlightedPosition, setHighlightedPosition] = useState<{ x: number; y: number } | null>(null);

  // 공개 여부 (현재는 모든 롤링페이퍼가 공개)
  const isPublic = true;

  // 하이라이트 지우기
  const clearHighlight = useCallback(() => {
    setHighlightedPosition(null);
  }, []);

  // 메시지 클릭 핸들러
  const handleMessageClick = useCallback((message: RollingPaperMessage | VisitMessage) => {
    // 메시지 상세 보기 또는 선택 토글
    if ('id' in message) {
      toggleMessageSelection(message.id);
    }
  }, [toggleMessageSelection]);

  // 메시지 제출 핸들러 - 0-based 좌표 직접 전달
  const handleMessageSubmit = useCallback(async (position: { x: number; y: number }, data: unknown) => {
    // 타입 검증을 통해 필요한 필드들이 있는지 확인
    if (data && typeof data === 'object' && 'content' in data && 'anonymousNickname' in data && 'decoType' in data) {
      if (!ownerId) {
        throw new Error('Owner ID is not available');
      }
      // handleCreateMessage가 Promise를 반환하도록 수정
      return await handleCreateMessage({
        ownerId: ownerId,
        ownerName: targetNickname,
        content: data.content as string,
        anonymity: data.anonymousNickname as string,
        decoType: data.decoType as DecoType,
        x: position.x,
        y: position.y
      });
    }
    throw new Error('Invalid message data');
  }, [handleCreateMessage, targetNickname, ownerId]);

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="min-h-screen bg-paper flex items-center justify-center px-4">
        <div className="text-center">
          <div
            className="w-12 h-12 bg-stamp-red rounded-xl flex items-center justify-center mx-auto mb-4 shadow-brand-md"
            aria-hidden="true"
          >
            <MessageSquare className="w-7 h-7 stroke-white fill-white/30 animate-pulse" />
          </div>
          <p className="text-ink-soft dark:text-muted-foreground font-medium">
            롤링페이퍼를 불러오는 중...
          </p>
        </div>
      </div>
    );
  }

  // 에러 상태 (존재하지 않는 사용자 등)
  if (blockedMessage) {
    return (
      <BlockedToastRedirect
        message={blockedMessage}
        redirectTo="/visit"
        fallbackText="차단 상태로 인해 롤링페이퍼를 볼 수 없습니다. 방문 페이지로 이동합니다."
      />
    );
  }

  if (isError) {
    // 도메인 인라인 not-found 도 글로벌 RETURN 도장 메타포 사용 (F-13-BUG-6)
    return (
      <NotFoundView
        stampCode="404"
        stampLabel="수신인 없음"
        title="롤링페이퍼를 찾을 수 없어요"
        description={
          <>
            <span className="font-semibold text-stamp-red">{targetNickname}</span>
            님의 롤링페이퍼가 존재하지 않거나 삭제되었어요.
            <br />
            닉네임을 다시 확인하거나 다른 친구를 찾아보세요.
          </>
        }
        extraActions={[
          {
            label: "친구 찾기",
            href: "/visit",
            icon: <Search className="w-4 h-4 mr-2" aria-hidden="true" />,
            variant: "outline",
          },
          {
            label: "내 페이지",
            href: "/mypage",
            icon: <Heart className="w-4 h-4 mr-2 text-stamp-red" aria-hidden="true" />,
            variant: "outline",
          },
        ]}
      />
    );
  }

  // 인증 체크: 비공개 롤링페이퍼는 소유자만 볼 수 있음
  if (!isPublic && !isOwner) {
    // 비공개 풀화면 — PRIVATE 도장 메타포 + 회복 액션 명시 (F-13-BUG-7)
    return (
      <NotFoundView
        stampCode="비공개"
        stampLabel="PRIVATE"
        title="이 편지는 주인만 열어볼 수 있어요"
        description={
          isAuthenticated ? (
            <>
              <span className="font-semibold text-stamp-red">{targetNickname}</span>
              님의 롤링페이퍼는 비공개로 설정되어 있어요.
              <br />
              내 페이지에서 내가 받은 편지를 확인해 보세요.
            </>
          ) : (
            <>
              <span className="font-semibold text-stamp-red">{targetNickname}</span>
              님의 롤링페이퍼는 비공개로 설정되어 있어요.
              <br />
              로그인하시면 내 페이지에서 내가 받은 편지를 확인할 수 있어요.
            </>
          )
        }
        extraActions={
          isAuthenticated
            ? [
                {
                  label: "내 페이지",
                  href: "/mypage",
                  icon: <UserIcon className="w-4 h-4 mr-2" aria-hidden="true" />,
                  variant: "outline",
                },
                {
                  label: "친구 찾기",
                  href: "/visit",
                  icon: <Search className="w-4 h-4 mr-2" aria-hidden="true" />,
                  variant: "outline",
                },
              ]
            : [
                {
                  label: "로그인",
                  href: "/login",
                  icon: <LogIn className="w-4 h-4 mr-2" aria-hidden="true" />,
                  variant: "outline",
                },
                {
                  label: "친구 찾기",
                  href: "/visit",
                  icon: <Search className="w-4 h-4 mr-2" aria-hidden="true" />,
                  variant: "outline",
                },
              ]
        }
      />
    );
  }

  const viewProps = {
    targetNickname,
    isPublic,
    isOwner,
    isMobile,
    messages,
    ownerId,
    messageCount: messages.length,
    totalPages,
    currentPage,
    setCurrentPage,
    isMessageListOpen,
    setIsMessageListOpen,
    highlightedPosition,
    clearHighlight,
    getMessageAt,
    getCoordsFromPageAndGrid,
    handleMessageSubmit,
    handleMessageClick,
    refetchMessages: async () => {
      await refetch();
    },
    toasts,
    removeToast,
    showSuccess,
    showError,
  };

  return <RollingPaperView {...viewProps} />;
});

RollingPaperContainer.displayName = "RollingPaperContainer";

"use client";

import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import { MessageSquare } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { useRollingPaper } from "@/hooks/useRollingPaper";
import { useRollingPaperShare } from "@/hooks/useRollingPaperShare";
import { rollingPaperApi } from "@/lib/api";
import { addRecentVisit } from "@/lib/cookies";
import { RollingPaperLayout } from "./RollingPaperLayout";
import { RollingPaperHeader } from "./RollingPaperHeader";
import { InfoCard } from "./InfoCard";
import { RollingPaperGrid } from "./RollingPaperGrid";
import { RecentMessages } from "./RecentMessages";

interface RollingPaperClientProps {
  nickname?: string; // 공개 롤링페이퍼인 경우
}

export const RollingPaperClient: React.FC<RollingPaperClientProps> = ({
  nickname,
}) => {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const isPublic = !!nickname;
  const targetNickname = nickname
    ? decodeURIComponent(nickname)
    : user?.userName || "";

  const {
    messages,
    messageCount,
    recentMessages,
    isLoading,
    isOwner,
    currentPage,
    totalPages,
    colsPerPage,
    rowsPerPage,
    slotsPerPage,
    setCurrentPage,
    refetchMessages,
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
    if (isPublic && isAuthenticated && user && nickname) {
      const currentNickname = decodeURIComponent(nickname);
      const isOwnerCheck = user.userName === currentNickname;

      if (isOwnerCheck) {
        window.location.href = "/rolling-paper";
        return;
      }
    }
  }, [isAuthenticated, user, nickname, isPublic]);

  // 방문 기록 저장 (다른 사람의 롤링페이퍼인 경우)
  useEffect(() => {
    if (isPublic && nickname && isAuthenticated !== null && user !== null) {
      const currentNickname = decodeURIComponent(nickname);
      const isOwnerCheck =
        isAuthenticated && user && user.userName === currentNickname;

      if (!isOwnerCheck) {
        addRecentVisit(nickname);
      }
    }
  }, [nickname, isAuthenticated, user, isPublic]);

  // 메시지 작성 핸들러
  const handleMessageSubmit = async (
    position: { x: number; y: number },
    data: any
  ) => {
    if (!isPublic || !nickname) return;

    try {
      const response = await rollingPaperApi.createMessage(
        decodeURIComponent(nickname),
        {
          decoType: data.decoType,
          anonymity: data.anonymousNickname,
          content: data.content,
          width: position.x,
          height: position.y,
        }
      );

      if (response.success) {
        await refetchMessages();
        alert("메시지가 성공적으로 작성되었습니다!");
      } else {
        alert("메시지 작성에 실패했습니다. 다시 시도해주세요.");
      }
    } catch (error) {
      console.error("Failed to create message:", error);
      alert("메시지 작성에 실패했습니다. 다시 시도해주세요.");
    }
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
      />

      {/* 정보 카드 */}
      <InfoCard isOwner={isOwner} nickname={targetNickname} />

      {/* 롤링페이퍼 그리드 */}
      <RollingPaperGrid
        messages={messages}
        nickname={targetNickname}
        currentPage={currentPage}
        totalPages={totalPages}
        colsPerPage={colsPerPage}
        rowsPerPage={rowsPerPage}
        slotsPerPage={slotsPerPage}
        isOwner={isOwner}
        onPageChange={setCurrentPage}
        onMessageSubmit={!isOwner ? handleMessageSubmit : undefined}
      />

      {/* 최근 메시지들 */}
      <RecentMessages
        messages={recentMessages}
        isOwner={isOwner}
        onShare={handleWebShare}
      />
    </RollingPaperLayout>
  );
};

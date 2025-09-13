"use client";

import React from "react";
import { MessageSquare } from "lucide-react";
import { useRollingPaper } from "@/hooks/features/useRollingPaper";
import { RollingPaperView } from "@/components/features/rolling-paper/RollingPaperView";

interface RollingPaperContainerProps {
  nickname?: string;
}

export const RollingPaperContainer: React.FC<RollingPaperContainerProps> = ({
  nickname,
}) => {
  const logic = useRollingPaper(nickname);

  // 로딩 상태
  if (logic.isLoading) {
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
  if (!logic.isPublic && (!logic.isAuthenticated || !logic.user)) {
    return null;
  }

  return <RollingPaperView {...logic} />;
};
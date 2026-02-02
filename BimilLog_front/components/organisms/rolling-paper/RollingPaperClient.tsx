"use client";

import React from "react";
import { RollingPaperContainer } from "@/components/organisms/rolling-paper/RollingPaperContainer";
import { useAuth } from "@/hooks";
import type { VisitPaperResult } from "@/types/domains/paper";

interface RollingPaperClientProps {
  nickname?: string;
  initialPaperData?: VisitPaperResult;
}

export const RollingPaperClient: React.FC<RollingPaperClientProps> = ({
  nickname,
  initialPaperData,
}) => {
  const { user } = useAuth();
  // nickname이 없으면(내 롤링페이퍼 페이지) 현재 로그인한 사용자의 memberName 사용
  // nickname이 있으면 URL 디코딩 처리
  const targetNickname = nickname ? decodeURIComponent(nickname) : user?.memberName;

  return <RollingPaperContainer nickname={targetNickname} initialPaperData={initialPaperData} />;
};
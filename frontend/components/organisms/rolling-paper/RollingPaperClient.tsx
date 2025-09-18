"use client";

import React from "react";
import { RollingPaperContainer } from "@/components/organisms/rolling-paper/RollingPaperContainer";
import { useAuth } from "@/hooks";

interface RollingPaperClientProps {
  nickname?: string;
}

export const RollingPaperClient: React.FC<RollingPaperClientProps> = ({
  nickname,
}) => {
  const { user } = useAuth();
  // nickname이 없으면(내 롤링페이퍼 페이지) 현재 로그인한 사용자의 userName 사용
  // nickname이 있으면 URL 디코딩 처리
  const targetNickname = nickname ? decodeURIComponent(nickname) : user?.userName;

  return <RollingPaperContainer nickname={targetNickname} />;
};
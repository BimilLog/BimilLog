"use client";

import React from "react";
import { RollingPaperContainer } from "@/components/organisms/rolling-paper/RollingPaperContainer";

interface RollingPaperClientProps {
  nickname?: string;
}

export const RollingPaperClient: React.FC<RollingPaperClientProps> = ({
  nickname,
}) => {
  return <RollingPaperContainer nickname={nickname} />;
};
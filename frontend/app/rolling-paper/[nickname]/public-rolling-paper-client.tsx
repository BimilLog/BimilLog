"use client";

import { RollingPaperClient } from "../components/RollingPaperClient";

interface PublicRollingPaperClientProps {
  nickname: string;
}

export default function PublicRollingPaperClient({
  nickname,
}: PublicRollingPaperClientProps) {
  return <RollingPaperClient nickname={nickname} />;
}

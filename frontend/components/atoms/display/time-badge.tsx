"use client";

import React from "react";
import { Clock } from "lucide-react";
import { Badge } from "./badge";
import { formatRelativeDate } from "@/lib/utils/date";

interface TimeBadgeProps {
  dateString: string;
  className?: string;
  size?: "xs" | "default" | "sm";
}

/**
 * 시간 표시를 위한 통합 Badge 컴포넌트
 * Flowbite React Badge 스타일을 활용하여 일관된 시간 표시
 * 시간별로 다른 색상을 적용하여 시각적 구분 제공
 */
export const TimeBadge: React.FC<TimeBadgeProps> = React.memo(({
  dateString,
  className,
  size = "default"
}) => {
  const relativeTime = formatRelativeDate(dateString);

  // 시간에 따른 색상 variant 결정
  const getVariant = () => {
    if (relativeTime === "방금 전") return "info";
    if (relativeTime.includes("분 전")) return "gray";
    if (relativeTime.includes("시간 전")) return "purple";
    if (relativeTime.includes("일 전")) return "indigo";
    return "gray"; // 절대 날짜의 경우 기본 gray
  };

  return (
    <Badge
      variant={getVariant()}
      icon={Clock}
      size={size}
      className={className}
    >
      {relativeTime}
    </Badge>
  );
});

TimeBadge.displayName = "TimeBadge";
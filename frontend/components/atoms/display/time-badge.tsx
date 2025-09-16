"use client";

import React from "react";
import { Clock, Calendar, Timer } from "lucide-react";
import { Badge } from "./badge";
import { formatRelativeDate } from "@/lib/utils/date";
import { Popover } from "flowbite-react";

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
  const date = new Date(dateString);

  // 정확한 날짜와 시간 포맷
  const exactDate = date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  });

  const exactTime = date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });

  // 시간에 따른 색상 variant 결정
  const getVariant = () => {
    if (relativeTime === "방금 전") return "info";
    if (relativeTime.includes("분 전")) return "gray";
    if (relativeTime.includes("시간 전")) return "purple";
    if (relativeTime.includes("일 전")) return "indigo";
    return "gray"; // 절대 날짜의 경우 기본 gray
  };

  // 팝오버 콘텐츠
  const popoverContent = (
    <div className="p-3 space-y-2 min-w-[200px]">
      <div className="flex items-center gap-2 text-gray-800">
        <Calendar className="w-4 h-4 text-purple-600" />
        <span className="text-sm font-medium">{exactDate}</span>
      </div>
      <div className="flex items-center gap-2 text-gray-600">
        <Timer className="w-4 h-4 text-blue-600" />
        <span className="text-sm">{exactTime}</span>
      </div>
      <div className="pt-2 border-t border-gray-200">
        <div className="flex items-center gap-2 text-gray-500">
          <Clock className="w-3 h-3" />
          <span className="text-xs">{relativeTime}</span>
        </div>
      </div>
    </div>
  );

  return (
    <Popover
      trigger="hover"
      placement="top"
      content={popoverContent}
    >
      <Badge
        variant={getVariant()}
        icon={Clock}
        size={size}
        className={className}
      >
        {relativeTime}
      </Badge>
    </Popover>
  );
});

TimeBadge.displayName = "TimeBadge";
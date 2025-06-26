"use client";

import { cn } from "@/lib/utils";
import { Heart, MessageSquare, Sparkles, Loader2 } from "lucide-react";

interface LoadingProps {
  className?: string;
  size?: "sm" | "md" | "lg";
  message?: string;
  type?: "default" | "page" | "button" | "card";
}

// 기본 스피너 컴포넌트
export function Spinner({
  className,
  size = "md",
}: {
  className?: string;
  size?: "sm" | "md" | "lg";
}) {
  const sizeClasses = {
    sm: "w-4 h-4",
    md: "w-6 h-6",
    lg: "w-8 h-8",
  };

  return (
    <Loader2
      className={cn(
        "animate-spin text-purple-600",
        sizeClasses[size],
        className
      )}
    />
  );
}

// 브랜드 로딩 애니메이션 (메인페이지 스타일)
export function BrandSpinner({ className }: { className?: string }) {
  return (
    <div className={cn("relative", className)}>
      <img
        src="/log.png"
        alt="비밀로그"
        className="h-12 object-contain animate-pulse"
      />
    </div>
  );
}

// 다양한 로딩 변형들
export function Loading({
  className,
  size = "md",
  message,
  type = "default",
}: LoadingProps) {
  if (type === "page") {
    return (
      <div
        className={cn(
          "min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center",
          className
        )}
      >
        <div className="text-center">
          <BrandSpinner className="mx-auto mb-4" />
          <p className="text-gray-600 text-lg">{message || "로딩 중..."}</p>
        </div>
      </div>
    );
  }

  if (type === "card") {
    return (
      <div
        className={cn(
          "bg-white/80 backdrop-blur-sm rounded-lg border-0 shadow-lg p-8 text-center",
          className
        )}
      >
        <BrandSpinner className="mx-auto mb-4" />
        <p className="text-gray-600">{message || "로딩 중..."}</p>
      </div>
    );
  }

  if (type === "button") {
    return (
      <div className={cn("flex items-center gap-2", className)}>
        <Spinner size={size} />
        <span className="text-sm">{message || "처리 중..."}</span>
      </div>
    );
  }

  // default type
  return (
    <div className={cn("flex items-center justify-center p-8", className)}>
      <div className="text-center">
        <Spinner size={size} className="mx-auto mb-2" />
        {message && <p className="text-sm text-gray-600">{message}</p>}
      </div>
    </div>
  );
}

// 스켈레톤 로딩 컴포넌트 (모바일 최적화)
export function Skeleton({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        "animate-pulse rounded-lg bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 bg-[length:200%_100%]",
        className
      )}
      style={{
        animation: "skeleton-loading 1.5s ease-in-out infinite",
      }}
    />
  );
}

// 카드형 스켈레톤
export function CardSkeleton({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        "bg-white/80 backdrop-blur-sm rounded-lg border-0 shadow-lg p-6",
        className
      )}
    >
      <div className="space-y-4">
        <div className="flex items-center space-x-3">
          <Skeleton className="w-10 h-10 rounded-full" />
          <div className="space-y-2 flex-1">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-3 w-16" />
          </div>
        </div>
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
      </div>
    </div>
  );
}

// 리스트형 스켈레톤
export function ListSkeleton({
  items = 3,
  className,
}: {
  items?: number;
  className?: string;
}) {
  return (
    <div className={cn("space-y-3", className)}>
      {Array.from({ length: items }).map((_, i) => (
        <div
          key={i}
          className="bg-white/80 backdrop-blur-sm rounded-lg border-0 shadow-lg p-4"
        >
          <div className="flex items-center space-x-3">
            <Skeleton className="w-8 h-8 rounded-full" />
            <div className="space-y-2 flex-1">
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-3 w-1/2" />
            </div>
            <Skeleton className="w-6 h-6" />
          </div>
        </div>
      ))}
    </div>
  );
}

// Pull-to-refresh 로딩 (모바일 특화)
export function PullToRefreshLoader({ isVisible }: { isVisible: boolean }) {
  if (!isVisible) return null;

  return (
    <div className="flex justify-center py-4">
      <div className="flex items-center space-x-2">
        <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-full flex items-center justify-center">
          <Sparkles className="w-4 h-4 text-white animate-spin" />
        </div>
        <span className="text-sm text-gray-600">새로고침 중...</span>
      </div>
    </div>
  );
}

// CSS 키프레임 추가 (전역으로 사용)
export const loadingStyles = `
@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
`;

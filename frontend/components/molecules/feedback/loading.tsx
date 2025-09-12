"use client";

import { cn } from "@/lib/utils";
import { Loader2 } from "lucide-react";

interface LoadingProps {
  className?: string;
  size?: "sm" | "md" | "lg";
  message?: string;
  type?: "default" | "page" | "button" | "card";
}

// 기본 스피너 컴포넌트 (내부 사용)
function LoadingIcon({
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
          <LoadingIcon size="lg" className="mx-auto mb-4" />
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
        <LoadingIcon size="lg" className="mx-auto mb-4" />
        <p className="text-gray-600">{message || "로딩 중..."}</p>
      </div>
    );
  }

  if (type === "button") {
    return (
      <div className={cn("flex items-center gap-2", className)}>
        <LoadingIcon size={size} />
        <span className="text-sm">{message || "처리 중..."}</span>
      </div>
    );
  }

  // default type
  return (
    <div className={cn("flex items-center justify-center p-8", className)}>
      <div className="text-center">
        <LoadingIcon size={size} className="mx-auto mb-2" />
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

import { cn } from "@/lib/utils";
import { Spinner as FlowbiteSpinner } from "flowbite-react";
import { Card, CardContent } from "@/components";

interface LoadingProps {
  className?: string;
  size?: "sm" | "md" | "lg" | "xl";
  message?: string;
  type?: "default" | "page" | "button" | "card";
}

// 다양한 로딩 변형들
export function Loading({
  className,
  size = "xl",
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
        <div className="flex flex-col items-center">
          <FlowbiteSpinner
            color="pink"
            size="xl"
            aria-label={message || "로딩 중..."}
          />
          <p className="mt-4 text-brand-muted text-lg">{message || "로딩 중..."}</p>
        </div>
      </div>
    );
  }

  if (type === "card") {
    return (
      <Card variant="elevated" className={className}>
        <CardContent className="p-8 flex flex-col items-center">
          <FlowbiteSpinner
            color="pink"
            size="xl"
            aria-label={message || "로딩 중..."}
          />
          <p className="mt-4 text-brand-muted">{message || "로딩 중..."}</p>
        </CardContent>
      </Card>
    );
  }

  if (type === "button") {
    return (
      <div className={cn("flex items-center justify-center gap-2", className)}>
        <FlowbiteSpinner
          color="pink"
          size={size === "xl" ? "md" : size}
          aria-label={message || "처리 중..."}
        />
        <span className="text-sm">{message || "처리 중..."}</span>
      </div>
    );
  }

  // default type
  return (
    <div className={cn("flex items-center justify-center p-8", className)}>
      <div className="flex flex-col items-center">
        <FlowbiteSpinner
          color="pink"
          size={size === "xl" ? "xl" : size}
          aria-label={message || "로딩 중..."}
        />
        {message && <p className="mt-2 text-sm text-brand-muted">{message}</p>}
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

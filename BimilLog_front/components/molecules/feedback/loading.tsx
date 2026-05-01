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
          "min-h-screen bg-paper flex items-center justify-center",
          className
        )}
        role="status"
        aria-live="polite"
      >
        <div className="flex flex-col items-center">
          <FlowbiteSpinner
            color="failure"
            size="xl"
            aria-label={message || "로딩 중..."}
          />
          <p className="mt-4 font-display text-ink-soft dark:text-muted-foreground text-lg break-keep">
            {message || "로딩 중..."}
          </p>
        </div>
      </div>
    );
  }

  if (type === "card") {
    return (
      <Card variant="elevated" className={className}>
        <CardContent
          className="p-8 flex flex-col items-center"
          role="status"
          aria-live="polite"
        >
          <FlowbiteSpinner
            color="failure"
            size="xl"
            aria-label={message || "로딩 중..."}
          />
          <p className="mt-4 font-body text-ink-soft dark:text-muted-foreground break-keep">
            {message || "로딩 중..."}
          </p>
        </CardContent>
      </Card>
    );
  }

  if (type === "button") {
    return (
      <div
        className={cn("flex items-center justify-center gap-2", className)}
        role="status"
        aria-live="polite"
      >
        <FlowbiteSpinner
          color="failure"
          size={size === "xl" ? "md" : size}
          aria-label={message || "처리 중..."}
        />
        <span className="text-sm">{message || "처리 중..."}</span>
      </div>
    );
  }

  // default type
  return (
    <div
      className={cn("flex items-center justify-center p-8", className)}
      role="status"
      aria-live="polite"
    >
      <div className="flex flex-col items-center">
        <FlowbiteSpinner
          color="failure"
          size={size === "xl" ? "xl" : size}
          aria-label={message || "로딩 중..."}
        />
        {message && (
          <p className="mt-2 text-sm font-body text-ink-soft dark:text-muted-foreground break-keep">
            {message}
          </p>
        )}
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

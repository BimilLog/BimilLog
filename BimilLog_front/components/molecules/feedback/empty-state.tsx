import React from "react";
import { cn } from "@/lib/utils";
import { Button } from "@/components";
import {
  Heart,
  Search,
  FileText,
  Sparkles,
  AlertCircle,
  Wifi,
  RefreshCw,
  MessageCircle,
  ThumbsUp,
} from "lucide-react";
import Link from "next/link";

interface EmptyStateProps {
  className?: string;
  type?: "posts" | "comments" | "liked-posts" | "liked-comments" | "messages" | "search" | "error" | "offline" | "custom";
  title?: string;
  description?: string;
  actionLabel?: string;
  actionHref?: string;
  onAction?: () => void;
  icon?: React.ReactNode;
  showRetry?: boolean;
  onRetry?: () => void;
  variant?: "default" | "cute" | "playful";
}

// 아이콘 맵핑 (더 다양한 귀여운 아이콘 추가)
const iconMap = {
  posts: FileText,
  comments: MessageCircle,
  "liked-posts": Heart,
  "liked-comments": ThumbsUp,
  messages: Heart,
  search: Search,
  error: AlertCircle,
  offline: Wifi,
  custom: Sparkles,
};

// 기본 메시지 맵핑
const defaultMessages = {
  posts: {
    title: "아직 작성된 글이 없어요",
    description: "첫 번째 글을 작성해서 다른 사람들과 소통해보세요!",
    actionLabel: "글쓰기",
    actionHref: "/board/write",
  },
  comments: {
    title: "작성한 댓글이 없습니다",
    description: "다른 사람의 글에 댓글을 달아보세요!",
  },
  "liked-posts": {
    title: "추천한 글이 없습니다",
    description: "마음에 드는 글에 추천을 눌러보세요!",
  },
  "liked-comments": {
    title: "추천한 댓글이 없습니다",
    description: "좋은 댓글에 추천을 눌러보세요!",
  },
  messages: {
    title: "아직 받은 메시지가 없어요",
    description:
      "친구들에게 롤링페이퍼 링크를 공유해서 따뜻한 메시지를 받아보세요!",
    actionLabel: "링크 공유하기",
  },
  search: {
    title: "검색 결과가 없어요",
    description: "다른 검색어로 다시 시도해보세요.",
    actionLabel: "검색 초기화",
  },
  error: {
    title: "문제가 발생했어요",
    description: "잠시 후 다시 시도해주세요.",
    actionLabel: "다시 시도",
    showRetry: true,
  },
  offline: {
    title: "인터넷 연결을 확인해주세요",
    description: "네트워크 상태를 확인하고 다시 시도해주세요.",
    actionLabel: "다시 시도",
    showRetry: true,
  },
  custom: {
    title: "내용이 없어요",
    description: "",
  },
} as const;

export const EmptyState = React.memo<EmptyStateProps>(({
  className,
  type = "custom",
  title: customTitle,
  description: customDescription,
  actionLabel: customActionLabel,
  actionHref,
  onAction,
  icon: customIcon,
  showRetry,
  onRetry,
  variant = "cute",
}) => {
  const defaults =
    defaultMessages[type as keyof typeof defaultMessages] ||
    defaultMessages.custom;

  const title = customTitle || defaults.title || "내용이 없어요";
  const description = customDescription || defaults.description || "";
  const actionLabel =
    customActionLabel || (defaults as { actionLabel?: string }).actionLabel;
  const shouldShowRetry =
    showRetry || (defaults as { showRetry?: boolean }).showRetry || false;

  const IconComponent = iconMap[type];
  const getIconWithColors = () => {
    if (customIcon) return customIcon;
    if (!IconComponent) return null;

    const colorClasses = {
      posts: "stroke-postal-navy fill-postal-navy/10",
      comments: "stroke-green-600 fill-green-100",
      "liked-posts": "stroke-stamp-red fill-stamp-red/10",
      "liked-comments": "stroke-postal-navy fill-postal-navy/10",
      messages: "stroke-stamp-red fill-stamp-red/10",
      search: "stroke-stamp-red fill-stamp-red/10",
      error: "stroke-red-600 fill-red-100",
      offline: "stroke-postal-navy fill-postal-navy/10",
      custom: "stroke-stamp-red fill-stamp-red/10"
    };

    const colorClass = colorClasses[type] || colorClasses.custom;
    return <IconComponent className={`w-6 h-6 ${colorClass}`} />;
  };

  const icon = getIconWithColors();

  if (variant === "cute" || variant === "playful") {
    return (
      <div
        className={cn(
          "flex flex-col items-center justify-center text-center p-8 md:p-12",
          "min-h-[300px] relative",
          className
        )}
      >
        {/* 메인 일러스트레이션 — 종이 위 우표 모티브 */}
        <div className="mb-8 relative">
          <div className="w-24 h-24 mx-auto mb-6 bg-paper-aged border-2 border-dashed border-stamp-red/40 dark:bg-stamp-red/20 rounded-3xl flex items-center justify-center shadow-brand-lg animate-pulse-cute">
            <div className="text-stamp-red dark:text-gray-100 text-2xl">{icon}</div>
          </div>

          {/* 종이 데코: stamp-red / postal-navy / seal-gold 도트 */}
          <div className="flex items-center justify-center space-x-2">
            <div className="w-2 h-2 bg-stamp-red/50 rounded-full animate-bounce-cute"></div>
            <div className="w-3 h-3 bg-postal-navy/40 rounded-full animate-bounce-cute" style={{ animationDelay: "0.2s" }}></div>
            <div className="w-2 h-2 bg-seal-gold rounded-full animate-bounce-cute" style={{ animationDelay: "0.4s" }}></div>
          </div>
        </div>

        {/* 텍스트 영역 */}
        <div className="max-w-md mx-auto mb-8">
          <h3 className="text-xl md:text-2xl font-bold text-brand-primary dark:text-gray-100 mb-4">
            {title}
          </h3>
          {description && (
            <p className="text-brand-secondary dark:text-gray-300 leading-relaxed text-sm md:text-base">
              {description}
            </p>
          )}
        </div>

        {/* 액션 버튼들 */}
        <div className="flex flex-col sm:flex-row gap-3 w-full max-w-sm">
          {shouldShowRetry && onRetry && (
            <Button
              variant="outline"
              size="default"
              onClick={onRetry}
              className="flex items-center justify-center min-h-touch"
            >
              <RefreshCw className="w-4 h-4 mr-2 stroke-slate-600" />
              다시 시도
            </Button>
          )}

          {actionLabel && (actionHref || onAction) && (
            <Button
              asChild={!!actionHref}
              size="default"
              onClick={onAction}
              className="flex items-center justify-center min-h-touch"
            >
              {actionHref ? (
                <Link href={actionHref}>{actionLabel}</Link>
              ) : (
                actionLabel
              )}
            </Button>
          )}
        </div>
      </div>
    );
  }

  // 기본 variant (기존 디자인 유지하되 개선)
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center text-center p-8 md:p-12",
        "min-h-[300px]",
        className
      )}
    >
      {/* 일러스트레이션 영역 — 종이 봉투 톤 */}
      <div className="mb-6">
        <div className="w-20 h-20 mx-auto mb-4 bg-paper-aged border-2 border-dashed border-stamp-red/30 dark:bg-stamp-red/15 rounded-full flex items-center justify-center shadow-brand-lg">
          <div className="text-stamp-red dark:text-gray-100">{icon}</div>
        </div>
        {/* 장식 도트 */}
        <div className="hidden md:flex items-center justify-center space-x-1">
          <div className="w-2 h-2 bg-stamp-red/30 rounded-full"></div>
          <div className="w-2 h-2 bg-postal-navy/30 rounded-full"></div>
          <div className="w-2 h-2 bg-seal-gold rounded-full"></div>
        </div>
      </div>

      {/* 텍스트 영역 */}
      <div className="max-w-md mx-auto mb-8">
        <h3 className="text-xl md:text-2xl font-bold text-brand-primary dark:text-gray-100 mb-3">
          {title}
        </h3>
        {description && (
          <p className="text-brand-secondary dark:text-gray-300 leading-relaxed">{description}</p>
        )}
      </div>

      {/* 액션 버튼들 */}
      <div className="flex flex-col sm:flex-row gap-3 w-full max-w-sm">
        {shouldShowRetry && onRetry && (
          <Button
            variant="outline"
            size="default"
            onClick={onRetry}
            className="flex items-center justify-center min-h-touch"
          >
            <RefreshCw className="w-4 h-4 mr-2 stroke-slate-600" />
            다시 시도
          </Button>
        )}

        {actionLabel && (actionHref || onAction) && (
          <Button
            asChild={!!actionHref}
            size="default"
            onClick={onAction}
            className="flex items-center justify-center min-h-touch"
          >
            {actionHref ? (
              <Link href={actionHref}>{actionLabel}</Link>
            ) : (
              actionLabel
            )}
          </Button>
        )}
      </div>
    </div>
  );
});

EmptyState.displayName = "EmptyState";

// 사전 정의된 EmptyState 타입들
export const CuteEmptyState = React.memo((props: Omit<EmptyStateProps, 'variant'>) => (
  <EmptyState variant="cute" {...props} />
));
CuteEmptyState.displayName = "CuteEmptyState";

export const PlayfulEmptyState = React.memo((props: Omit<EmptyStateProps, 'variant'>) => (
  <EmptyState variant="playful" {...props} />
));
PlayfulEmptyState.displayName = "PlayfulEmptyState";
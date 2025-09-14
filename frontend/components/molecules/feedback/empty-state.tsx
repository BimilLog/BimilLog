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
}

// 아이콘 맵핑
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
  const icon =
    customIcon || (IconComponent && <IconComponent className="w-12 h-12" />);

  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center text-center p-8 md:p-12",
        "min-h-[300px]", // 모바일 최적화된 최소 높이
        className
      )}
    >
      {/* 일러스트레이션 영역 */}
      <div className="mb-6">
        <div className="w-20 h-20 mx-auto mb-4 bg-gradient-to-r from-pink-100 to-purple-100 rounded-full flex items-center justify-center">
          <div className="text-gray-400">{icon}</div>
        </div>
        {/* 장식용 도트들 (모바일에서는 숨김) */}
        <div className="hidden md:flex items-center justify-center space-x-1">
          <div className="w-2 h-2 bg-pink-200 rounded-full"></div>
          <div className="w-2 h-2 bg-purple-200 rounded-full"></div>
          <div className="w-2 h-2 bg-indigo-200 rounded-full"></div>
        </div>
      </div>

      {/* 텍스트 영역 */}
      <div className="max-w-md mx-auto mb-8">
        <h3 className="text-xl md:text-2xl font-bold text-gray-800 mb-3">
          {title}
        </h3>
        {description && (
          <p className="text-gray-600 leading-relaxed">{description}</p>
        )}
      </div>

      {/* 액션 버튼들 */}
      <div className="flex flex-col sm:flex-row gap-3 w-full max-w-sm">
        {shouldShowRetry && onRetry && (
          <Button
            variant="outline"
            size="lg"
            onClick={onRetry}
            className="flex items-center justify-center"
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            다시 시도
          </Button>
        )}

        {actionLabel && (actionHref || onAction) && (
          <Button
            asChild={!!actionHref}
            size="lg"
            onClick={onAction}
            className="flex items-center justify-center"
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

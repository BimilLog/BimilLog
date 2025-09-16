"use client";

import React from "react";
import { Button } from "flowbite-react";
import { MessageSquare, Share2, List } from "lucide-react";
import { KakaoShareButton } from "@/components";
import { useRollingPaperShare } from "@/hooks/features/useRollingPaperShare";

interface RollingPaperHeaderProps {
  nickname: string;
  messageCount: number;
  isOwner?: boolean;
  onShowMessages?: () => void;
  className?: string;
}

export const RollingPaperHeader: React.FC<RollingPaperHeaderProps> = React.memo(({
  nickname,
  messageCount,
  isOwner = false,
  onShowMessages,
  className = "",
}) => {
  const { handleWebShare } = useRollingPaperShare({
    nickname,
    messageCount,
    isOwner,
  });

  return (
    <header className={`bg-white/80 backdrop-blur-md border-b ${className}`}>
      <div className="px-4 py-4">
        <div className="max-w-screen-xl mx-auto">
          {/* 데스크톱 레이아웃 */}
          <div className="hidden md:flex items-center justify-between">
            <div className="flex items-center space-x-2 flex-1 min-w-0">
              <div className="w-8 h-8 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center">
                <MessageSquare className="w-5 h-5 text-white" />
              </div>
              <div className="min-w-0 flex-1">
                <h1 className="font-bold text-brand-primary text-base truncate">
                  {nickname}님의 롤링페이퍼
                </h1>
              </div>
            </div>
            <div className="flex items-center space-x-2 flex-shrink-0">
              {onShowMessages && (
                <Button
                  onClick={onShowMessages}
                  color="blue"
                  size="sm"
                  className="text-xs"
                >
                  <List className="w-4 h-4 mr-1" />
                  메시지 목록 보기
                </Button>
              )}
              <KakaoShareButton
                type="rollingPaper"
                userName={nickname}
                messageCount={messageCount}
                color="yellow"
                size="sm"
                className="text-xs"
              />
              <Button
                onClick={handleWebShare}
                color="gray"
                size="sm"
                className="text-xs"
              >
                <Share2 className="w-4 h-4 mr-1" />
                링크 공유
              </Button>
            </div>
          </div>

          {/* 모바일 레이아웃 */}
          <div className="md:hidden">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2 flex-1 min-w-0">
                <div className="w-7 h-7 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center">
                  <MessageSquare className="w-4 h-4 text-white" />
                </div>
                <h1 className="font-bold text-brand-primary text-sm truncate">
                  {nickname}님의 롤링페이퍼
                </h1>
              </div>
              <div className="flex items-center space-x-1 flex-shrink-0">
                {onShowMessages && (
                  <Button
                    onClick={onShowMessages}
                    color="blue"
                    size="sm"
                    className="px-2 py-1 text-xs h-7"
                  >
                    <List className="w-3 h-3 mr-1" />
                    목록
                  </Button>
                )}
                <KakaoShareButton
                  type="rollingPaper"
                  userName={nickname}
                  messageCount={messageCount}
                  color="yellow"
                  size="sm"
                  className="px-2 py-1 text-xs h-7"
                />
                <Button
                  onClick={handleWebShare}
                  color="gray"
                  size="sm"
                  className="px-2 py-1 text-xs h-7"
                >
                  <Share2 className="w-3 h-3 mr-1" />
                  링크
                </Button>
              </div>
            </div>

            {/* 모바일 메시지 카운트 */}
            {messageCount > 0 && (
              <div className="mt-2 text-center">
                <span className="text-xs text-brand-muted">
                  총 {messageCount}개의 메시지
                </span>
              </div>
            )}
          </div>
        </div>
      </div>

    </header>
  );
});

RollingPaperHeader.displayName = "RollingPaperHeader";
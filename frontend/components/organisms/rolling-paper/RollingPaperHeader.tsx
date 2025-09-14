"use client";

import React from "react";
import { Button } from "@/components";
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

export const RollingPaperHeader: React.FC<RollingPaperHeaderProps> = ({
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
              <KakaoShareButton
                type="rollingPaper"
                userName={nickname}
                messageCount={messageCount}
                variant="outline"
                size="sm"
                className="h-10 px-3"
              />
              <Button
                variant="outline"
                size="sm"
                className="bg-white h-10 px-3"
                onClick={handleWebShare}
              >
                <Share2 className="w-4 h-4 mr-1" />
                공유
              </Button>
              {isOwner && onShowMessages && (
                <Button
                  onClick={onShowMessages}
                  variant="outline"
                  size="sm"
                  className="bg-white/80 h-10 px-3 text-cyan-700 hover:bg-cyan-50 border-cyan-300"
                >
                  <List className="w-4 h-4 mr-1" />
                  받은 메시지
                </Button>
              )}
            </div>
          </div>

          {/* 모바일 레이아웃 */}
          <div className="md:hidden">
            {/* 상단: 제목 영역 */}
            <div className="flex items-center space-x-2 mb-3">
              <div className="w-6 h-6 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center">
                <MessageSquare className="w-3 h-3 text-white" />
              </div>
              <div className="min-w-0 flex-1">
                <h1 className="font-bold text-brand-primary text-sm">
                  {nickname}님의 롤링페이퍼
                </h1>
              </div>
            </div>

            {/* 하단: 버튼 영역 */}
            <div className="flex items-center justify-center space-x-2">
              <KakaoShareButton
                type="rollingPaper"
                userName={nickname}
                messageCount={messageCount}
                variant="outline"
                size="sm"
                className="h-8 px-2 flex-1"
              />
              <Button
                variant="outline"
                size="sm"
                className="bg-white h-8 px-2 flex-1"
                onClick={handleWebShare}
              >
                <Share2 className="w-4 h-4 mr-1" />
                공유
              </Button>
              {isOwner && onShowMessages && (
                <Button
                  onClick={onShowMessages}
                  variant="outline"
                  size="sm"
                  className="bg-white/80 h-8 px-2 text-cyan-700 hover:bg-cyan-50 border-cyan-300 flex-1"
                >
                  <List className="w-4 h-4" />
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};

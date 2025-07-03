"use client";

import React from "react";
import { Button } from "@/components/ui/button";
import { MessageSquare, Share2, ArrowLeft } from "lucide-react";
import { KakaoShareButton } from "@/components/atoms/kakao-share-button";
import Link from "next/link";
import { useRollingPaperShare } from "@/hooks/useRollingPaperShare";

interface RollingPaperHeaderProps {
  nickname: string;
  messageCount: number;
  isOwner?: boolean;
  className?: string;
}

export const RollingPaperHeader: React.FC<RollingPaperHeaderProps> = ({
  nickname,
  messageCount,
  isOwner = false,
  className = "",
}) => {
  const { handleWebShare } = useRollingPaperShare({
    nickname,
    messageCount,
    isOwner,
  });

  return (
    <header
      className={`sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b ${className}`}
    >
      <div className="container mx-auto px-4 py-3 md:py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2 md:space-x-4 flex-1 min-w-0">
            <Link href="/">
              <Button variant="ghost" size="sm" className="h-8 md:h-10">
                <ArrowLeft className="w-4 h-4 mr-1 md:mr-2" />
                <span className="hidden sm:inline">홈으로</span>
                <span className="sm:hidden">홈</span>
              </Button>
            </Link>
            <div className="flex items-center space-x-2 flex-1 min-w-0">
              <div className="w-6 h-6 md:w-8 md:h-8 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-lg flex items-center justify-center">
                <MessageSquare className="w-3 h-3 md:w-5 md:h-5 text-white" />
              </div>
              <div className="min-w-0 flex-1">
                <h1 className="font-bold text-gray-800 text-sm md:text-base truncate">
                  {nickname}님의 롤링페이퍼
                </h1>
                <p className="text-xs text-gray-500">총 {messageCount}개</p>
              </div>
            </div>
          </div>
          <div className="flex items-center space-x-1 md:space-x-2">
            <KakaoShareButton
              type="rollingPaper"
              userName={nickname}
              messageCount={messageCount}
              variant="outline"
              size="sm"
              className="h-8 md:h-10 px-2 md:px-3"
            />
            <Button
              variant="outline"
              size="sm"
              className="bg-white h-8 md:h-10 px-2 md:px-3"
              onClick={handleWebShare}
            >
              <Share2 className="w-4 h-4 md:mr-1" />
              <span className="hidden md:inline">공유</span>
            </Button>
          </div>
        </div>
      </div>
    </header>
  );
};

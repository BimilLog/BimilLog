"use client";

import React from "react";
import { Button } from "@/components";
import { MessageSquare, Share2, List, ChevronLeft, ChevronRight } from "lucide-react";
import { KakaoShareButton } from "@/components";
import { useRollingPaperShare } from "@/hooks";

interface NavigationBarProps {
  nickname: string;
  messageCount: number;
  isOwner?: boolean;
  onShowMessages?: () => void;
  // 페이지네이션 관련
  currentPage?: number;
  totalPages?: number;
  onPageChange?: (page: number) => void;
  showPagination?: boolean;
  className?: string;
}

export const NavigationBar: React.FC<NavigationBarProps> = React.memo(({
  nickname,
  messageCount,
  isOwner = false,
  onShowMessages,
  currentPage = 1,
  totalPages = 1,
  onPageChange,
  showPagination = false,
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
                <h1 className="font-bold text-gray-800 text-base truncate">
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
                className="text-xs"
              />
              {messageCount > 0 && onShowMessages && (
                <Button
                  onClick={onShowMessages}
                  variant="outline"
                  size="sm"
                  className="text-xs"
                >
                  <List className="w-4 h-4 mr-1" />
                  전체 메시지
                </Button>
              )}
              <Button
                onClick={handleWebShare}
                variant="outline"
                size="sm"
                className="text-xs"
              >
                <Share2 className="w-4 h-4 mr-1" />
                공유
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
                <h1 className="font-bold text-gray-800 text-sm truncate">
                  {nickname}님의 롤링페이퍼
                </h1>
              </div>
              <div className="flex items-center space-x-1 flex-shrink-0">
                <KakaoShareButton
                  type="rollingPaper"
                  userName={nickname}
                  messageCount={messageCount}
                  variant="outline"
                  size="sm"
                  className="px-2 py-1 text-xs h-7"
                />
                {messageCount > 0 && onShowMessages && (
                  <Button
                    onClick={onShowMessages}
                    variant="outline"
                    size="sm"
                    className="px-2 py-1 text-xs h-7"
                  >
                    <List className="w-3 h-3" />
                  </Button>
                )}
                <Button
                  onClick={handleWebShare}
                  variant="outline"
                  size="sm"
                  className="px-2 py-1 text-xs h-7"
                >
                  <Share2 className="w-3 h-3" />
                </Button>
              </div>
            </div>

            {/* 모바일 메시지 카운트 */}
            {messageCount > 0 && (
              <div className="mt-2 text-center">
                <span className="text-xs text-gray-600">
                  총 {messageCount}개의 메시지
                </span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 페이지네이션 섹션 */}
      {showPagination && totalPages > 1 && onPageChange && (
        <div className="border-t bg-white/60 px-4 py-3">
          <div className="flex flex-col items-center space-y-4">
            {/* 페이지 인디케이터 */}
            <div className="flex justify-center items-center space-x-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => onPageChange(Math.max(1, currentPage - 1))}
                disabled={currentPage === 1}
                className="h-8 w-8 p-0 bg-white/80"
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>

              <div className="flex space-x-1">
                {Array.from({ length: totalPages }, (_, i) => (
                  <Button
                    key={i + 1}
                    variant={currentPage === i + 1 ? "default" : "outline"}
                    size="sm"
                    onClick={() => onPageChange(i + 1)}
                    className={`h-8 w-8 p-0 ${
                      currentPage === i + 1
                        ? "bg-gradient-to-r from-blue-500 to-cyan-600 text-white"
                        : "bg-white/80"
                    }`}
                  >
                    {i + 1}
                  </Button>
                ))}
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
                disabled={currentPage === totalPages}
                className="h-8 w-8 p-0 bg-white/80"
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>

            {/* 페이지 정보 */}
            <p className="text-xs text-gray-600">
              {currentPage} / {totalPages} 페이지
            </p>
          </div>
        </div>
      )}
    </header>
  );
});

NavigationBar.displayName = "NavigationBar";
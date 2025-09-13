"use client";

import React, { useMemo, memo } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components";
import { Plus, ChevronLeft, ChevronRight, Waves, Snowflake, Sparkles, IceCream2, Diamond, Mail, Star, User, Fish, Zap } from "lucide-react";
import { getDecoInfo } from "@/lib/api";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import { MessageForm } from "@/components/organisms/rolling-paper/MessageForm";
import { MessageView } from "@/components/organisms/rolling-paper/MessageView";
import { Button } from "@/components";
import { DecoIcon } from "@/components";

interface RollingPaperGridProps {
  messages: (RollingPaperMessage | VisitMessage)[];
  nickname: string;
  isOwner: boolean;
  isMobile: boolean;
  totalPages: number;
  currentPage: number;
  setCurrentPage: (page: number) => void;
  onMessageSubmit?: (position: { x: number; y: number }, data: unknown) => void;
  getMessageAt: (
    x: number,
    y: number
  ) => RollingPaperMessage | VisitMessage | null;
  getCoordsFromPageAndGrid: (
    page: number,
    gridX: number,
    gridY: number
  ) => { x: number; y: number };
  highlightedPosition?: { x: number; y: number } | null;
  onHighlightClear?: () => void;
  onSuccess?: (message: string) => void;
  onError?: (message: string) => void;
  onRefresh?: () => void;
  className?: string;
}

export const RollingPaperGrid: React.FC<RollingPaperGridProps> = memo(({
  messages,
  nickname,
  isOwner,
  isMobile,
  totalPages,
  currentPage,
  setCurrentPage,
  onMessageSubmit,
  getMessageAt,
  getCoordsFromPageAndGrid,
  highlightedPosition,
  onHighlightClear,
  onSuccess,
  onError,
  onRefresh,
  className = "",
}) => {
  // 그리드 설정 (메모화)
  const gridConfig = useMemo(() => {
    const pageWidth = isMobile ? 4 : 6;
    const pageHeight = 10;
    const totalSlots = pageWidth * pageHeight;
    return { pageWidth, pageHeight, totalSlots };
  }, [isMobile]);

  const { pageWidth, totalSlots } = gridConfig;

  return (
    <div className={`relative max-w-5xl mx-auto mb-6 md:mb-8 ${className}`}>
      {/* 종이 배경 */}
      <div
        className="relative bg-gradient-to-br from-cyan-50 via-blue-50 to-teal-50 rounded-2xl md:rounded-3xl shadow-xl md:shadow-2xl border-2 md:border-4 border-cyan-200"
        style={{
          backgroundImage: `
            radial-gradient(circle at 15px 15px, rgba(91,192,222,0.3) 1px, transparent 1px),
            radial-gradient(circle at 60px 60px, rgba(135,206,235,0.2) 1px, transparent 1px),
            linear-gradient(45deg, rgba(255,255,255,0.1) 25%, transparent 25%),
            linear-gradient(-45deg, rgba(255,255,255,0.1) 25%, transparent 25%)
          `,
          backgroundSize: "30px 30px, 120px 120px, 15px 15px, 15px 15px",
        }}
      >
        {/* 바인더 구멍들 */}
        <div className="absolute left-3 md:left-6 top-0 bottom-0 flex flex-col justify-evenly">
          {Array.from({ length: 8 }, (_, i) => (
            <div
              key={i}
              className="w-4 h-4 md:w-6 md:h-6 bg-white rounded-full shadow-inner border border-cyan-300 md:border-2"
              style={{
                boxShadow:
                  "inset 0 1px 2px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.1)",
              }}
            />
          ))}
        </div>

        {/* 제목 영역 */}
        <div className="pt-6 md:pt-8 pb-4 md:pb-6 px-12 md:px-20 text-center">
          <div className="relative">
            <h1 className="text-lg md:text-3xl font-bold text-cyan-800 mb-2 transform -rotate-1 flex items-center justify-center space-x-2">
              <Waves className="w-5 h-5 md:w-7 md:h-7" />
              <span>{nickname}님의 롤링페이퍼</span>
              <Waves className="w-5 h-5 md:w-7 md:h-7" />
            </h1>

            {/* 시원한 데코레이션 */}
            <div className="absolute -top-1 md:-top-2 -left-2 md:-left-4 animate-bounce">
              <Snowflake className="w-4 h-4 md:w-6 md:h-6 text-cyan-500" />
            </div>
            <div className="absolute -top-1 -right-3 md:-right-6 animate-pulse">
              <Sparkles className="w-3 h-3 md:w-5 md:h-5 text-blue-500" />
            </div>
            <div className="absolute -bottom-1 md:-bottom-2 left-4 md:left-8 animate-bounce delay-300">
              <IceCream2 className="w-3 h-3 md:w-4 md:h-4 text-cyan-400" />
            </div>
            <div className="absolute -bottom-1 right-6 md:right-12 animate-pulse delay-500">
              <Diamond className="w-3 h-3 md:w-5 md:h-5 text-indigo-500" />
            </div>

            <p className="text-cyan-600 text-xs md:text-sm mt-2 transform rotate-1 font-medium flex items-center justify-center space-x-1">
              <span>총 {messages.length}개의 시원한 메시지</span>
              <Mail className="w-3 h-3 md:w-4 md:h-4" />
            </p>
          </div>
        </div>

        {/* 메시지 그리드 */}
        <div className="px-12 md:px-20 pb-4 md:pb-6">
          {/* 페이지 네비게이션 */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 mb-4">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                disabled={currentPage === 1}
                className="bg-white/80"
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>

              <span className="text-sm font-medium text-cyan-700">
                {currentPage} / {totalPages}
              </span>

              <Button
                variant="outline"
                size="sm"
                onClick={() =>
                  setCurrentPage(Math.min(totalPages, currentPage + 1))
                }
                disabled={currentPage === totalPages}
                className="bg-white/80"
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          )}

          {/* 좌표 기반 그리드 */}
          <div
            className="grid gap-2 md:gap-3 bg-white/30 p-3 md:p-6 rounded-xl md:rounded-2xl border border-dashed md:border-2 border-cyan-300"
            style={{ gridTemplateColumns: `repeat(${pageWidth}, 1fr)` }}
          >
            {Array.from({ length: totalSlots }, (_, i) => {
              const gridX = i % pageWidth;
              const gridY = Math.floor(i / pageWidth);

              // 현재 페이지와 그리드 위치를 백엔드 좌표로 변환
              const { x: actualX, y: actualY } = getCoordsFromPageAndGrid(
                currentPage,
                gridX,
                gridY
              );

              // 해당 좌표에 메시지가 있는지 확인
              const messageAtPosition = getMessageAt(actualX, actualY);
              const decoInfo = messageAtPosition
                ? getDecoInfo(messageAtPosition.decoType)
                : null;

              // 하이라이트 좌표인지 확인
              const isHighlighted = highlightedPosition &&
                highlightedPosition.x === actualX &&
                highlightedPosition.y === actualY;

              return (
                <Dialog key={`${actualX}-${actualY}`}>
                  <DialogTrigger asChild>
                    <div
                      onClick={isHighlighted ? onHighlightClear : undefined}
                      className={`
                        aspect-square rounded-lg md:rounded-xl border-2 md:border-3 flex items-center justify-center transition-all duration-300 relative
                        ${
                          isHighlighted
                            ? "border-4 border-green-400 bg-gradient-to-br from-green-100 to-emerald-100 animate-pulse shadow-xl shadow-green-200 cursor-pointer"
                            : messageAtPosition
                            ? `bg-gradient-to-br ${typeof decoInfo?.color === 'string' ? decoInfo.color : ''} border-white shadow-md md:shadow-lg cursor-pointer hover:scale-105 md:hover:scale-110 hover:rotate-1 md:hover:rotate-3`
                            : isOwner
                            ? "border-dashed border-gray-300 cursor-not-allowed opacity-50"
                            : "border-dashed border-cyan-300 hover:border-cyan-500 hover:bg-cyan-50 cursor-pointer hover:scale-105 hover:rotate-1"
                        }
                      `}
                      style={{
                        boxShadow: messageAtPosition
                          ? "0 2px 8px rgba(91,192,222,0.3), inset 0 1px 0 rgba(255,255,255,0.5)"
                          : "0 1px 4px rgba(91,192,222,0.1)",
                      }}
                    >
                      {messageAtPosition ? (
                        <div className="relative">
                          <DecoIcon 
                            decoType={messageAtPosition.decoType} 
                            size="lg" 
                            showBackground={false} 
                            animate="bounce" 
                          />
                          <div className="absolute -top-0.5 md:-top-1 -right-0.5 md:-right-1 w-1.5 h-1.5 md:w-2 md:h-2 bg-yellow-300 rounded-full animate-ping"></div>
                          {isHighlighted && (
                            <div className="absolute inset-0 bg-green-300 rounded-full opacity-50 animate-ping"></div>
                          )}
                        </div>
                      ) : isOwner ? (
                        <div className="text-gray-400 text-xs md:text-sm text-center leading-tight opacity-0"></div>
                      ) : (
                        <div className="relative group">
                          <Plus
                            className={`w-4 h-4 md:w-5 md:h-5 transition-colors text-cyan-400 group-hover:text-cyan-600`}
                          />
                          <div
                            className={`absolute inset-0 rounded-full opacity-0 group-hover:opacity-30 transition-opacity animate-pulse bg-cyan-200`}
                          ></div>
                        </div>
                      )}
                    </div>
                  </DialogTrigger>
                  {(messageAtPosition || !isOwner) && (
                    <DialogContent className="max-w-sm md:max-w-md mx-auto bg-gradient-to-br from-cyan-50 to-blue-50 border-2 md:border-4 border-cyan-200 rounded-2xl md:rounded-3xl">
                      <DialogHeader>
                        <DialogTitle className="text-center text-cyan-800 font-bold text-sm md:text-base flex items-center justify-center space-x-2">
                          {messageAtPosition ? (
                            <>
                              <Mail className="w-4 h-4" />
                              <span>메시지 보기</span>
                            </>
                          ) : (
                            <>
                              <Sparkles className="w-4 h-4" />
                              <span>새 메시지 작성</span>
                            </>
                          )}
                        </DialogTitle>
                      </DialogHeader>
                      {messageAtPosition ? (
                        <MessageView
                          message={messageAtPosition}
                          isOwner={isOwner}
                          onDelete={onRefresh}
                          onDeleteSuccess={onSuccess}
                          onDeleteError={onError}
                        />
                      ) : (
                        onMessageSubmit && (
                          <MessageForm
                            nickname={nickname}
                            position={{ x: actualX, y: actualY }}
                            onSubmit={(data) => {
                              onMessageSubmit({ x: actualX, y: actualY }, data);
                            }}
                            onSuccess={onSuccess}
                            onError={onError}
                          />
                        )
                      )}
                    </DialogContent>
                  )}
                </Dialog>
              );
            })}
          </div>
        </div>

        {/* 떠다니는 데코레이션 */}
        <div className="absolute top-8 md:top-16 right-4 md:right-8 animate-spin-slow">
          <Star className="w-5 h-5 md:w-7 md:h-7 text-yellow-400" />
        </div>
        <div className="absolute top-16 md:top-32 left-8 md:left-12 animate-bounce">
          <Fish className="w-4 h-4 md:w-6 md:h-6 text-blue-500" />
        </div>
        <div className="absolute bottom-12 md:bottom-20 right-8 md:right-16 animate-pulse">
          <Zap className="w-4 h-4 md:w-6 md:h-6 text-cyan-400" />
        </div>
        <div className="absolute bottom-16 md:bottom-32 left-4 md:left-8 animate-bounce delay-700">
          <User className="w-3 h-3 md:w-5 md:h-5 text-indigo-500" />
        </div>
      </div>
    </div>
  );
});

RollingPaperGrid.displayName = "RollingPaperGrid";

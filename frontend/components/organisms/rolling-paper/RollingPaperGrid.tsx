"use client";

import React, { useMemo, memo, useCallback } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components";
import { Plus, ChevronLeft, ChevronRight, Waves, Sparkles, Mail } from "lucide-react";
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
  // 페이지 네비게이션 핸들러 최적화
  const handlePreviousPage = useCallback(() => {
    setCurrentPage(Math.max(1, currentPage - 1));
  }, [setCurrentPage, currentPage]);

  const handleNextPage = useCallback(() => {
    setCurrentPage(Math.min(totalPages, currentPage + 1));
  }, [setCurrentPage, totalPages, currentPage]);

  // 그리드 설정 (메모화) - 화면 크기에 따른 그리드 레이아웃 결정
  const gridConfig = useMemo(() => {
    const pageWidth = isMobile ? 4 : 6; // 모바일: 4열, PC: 6열
    const pageHeight = 10; // 고정 10행
    const totalSlots = pageWidth * pageHeight; // 한 페이지에 표시할 총 셀 개수
    return { pageWidth, pageHeight, totalSlots };
  }, [isMobile]);




  const { pageWidth, totalSlots } = gridConfig;

  // 메시지 제출 핸들러 최적화
  const handleMessageSubmit = useCallback((actualX: number, actualY: number, data: unknown) => {
    onMessageSubmit?.({ x: actualX, y: actualY }, data);
  }, [onMessageSubmit]);

  return (
    <div className={`relative max-w-5xl mx-auto mb-6 md:mb-8 ${className}`}>
      {/* 종이 배경 */}
      <div
        className="relative min-h-[600px] md:min-h-[700px] bg-gradient-to-br from-cyan-50 via-blue-50 to-teal-50 rounded-2xl md:rounded-3xl shadow-xl md:shadow-2xl border-2 md:border-4 border-cyan-200"
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

        {/* 제목 영역 */}
        <div className="pt-6 md:pt-8 pb-4 md:pb-6 px-12 md:px-20 text-center">
          <div className="relative">
            {/* 예쁜 제목 카드 */}
            <div className="bg-gradient-to-r from-cyan-100/90 via-blue-100/90 to-indigo-100/90 rounded-3xl p-6 md:p-8 shadow-xl border-2 border-white/80 backdrop-blur-md mb-6 relative overflow-hidden">
              {/* 배경 장식 */}
              <div className="absolute inset-0 bg-gradient-to-br from-white/20 to-transparent pointer-events-none"></div>

              <h1 className="text-xl md:text-4xl font-extrabold mb-4 flex items-center justify-center gap-3 relative z-10">
                <span className="text-cyan-700 font-extrabold transform hover:scale-105 transition-transform duration-300 drop-shadow-sm">
                  {nickname}님의 롤링페이퍼
                </span>
              </h1>

              {/* 메시지 수 카드 */}
              <div className="inline-flex items-center gap-3 bg-white/80 px-5 py-3 rounded-full shadow-lg border-2 border-cyan-200 relative z-10 backdrop-blur-sm">
                <Mail className="w-4 h-4 md:w-6 md:h-6 text-cyan-600 animate-bounce drop-shadow-sm" />
                <span className="text-cyan-800 text-sm md:text-lg font-bold tracking-wide">
                  총 {messages.length}개의 메시지
                </span>
                <Sparkles className="w-4 h-4 md:w-6 md:h-6 text-blue-500 animate-pulse drop-shadow-sm" />
              </div>
            </div>

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
                onClick={handlePreviousPage}
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
                onClick={handleNextPage}
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
              // 1차원 인덱스를 2차원 그리드 좌표로 변환
              const gridX = i % pageWidth; // 열 위치: 0, 1, 2, 3... (모바일 0~3, PC 0~5)
              const gridY = Math.floor(i / pageWidth); // 행 위치: 0, 1, 2... (0~9)

              // 현재 페이지와 그리드 위치를 백엔드 좌표로 변환
              // 예: 2페이지, gridX=1, gridY=2 -> PC에서 actualX=7, actualY=2
              const { x: actualX, y: actualY } = getCoordsFromPageAndGrid(
                currentPage,
                gridX,
                gridY
              );

              // 해당 좌표에 메시지가 있는지 확인
              const messageAtPosition = getMessageAt(actualX, actualY);
              // 메시지가 있으면 데코레이션 정보 가져오기
              const decoInfo = messageAtPosition
                ? getDecoInfo(messageAtPosition.decoType)
                : null;

              // 하이라이트 좌표인지 확인 (최근 작성된 메시지 등)
              const isHighlighted = highlightedPosition &&
                highlightedPosition.x === actualX &&
                highlightedPosition.y === actualY;

              return (
                <Dialog key={`grid-cell-${actualX}-${actualY}`}>
                  <DialogTrigger asChild>
                    <div
                      onClick={isHighlighted ? onHighlightClear : undefined}
                      className={`
                        aspect-square rounded-lg md:rounded-xl border-2 md:border-3 flex items-center justify-center transition-all duration-300 relative
                        ${
                          isHighlighted // 하이라이트된 셀 (최근 작성 메시지 등)
                            ? "border-4 border-green-400 bg-gradient-to-br from-green-100 to-emerald-100 animate-pulse shadow-xl shadow-green-200 cursor-pointer"
                            : messageAtPosition // 메시지가 있는 셀
                            ? `bg-gradient-to-br ${typeof decoInfo?.color === 'string' ? decoInfo.color : ''} border-white shadow-md md:shadow-lg cursor-pointer hover:scale-105 md:hover:scale-110 hover:rotate-1 md:hover:rotate-3`
                            : isOwner // 롤링페이퍼 소유자인 경우 메시지 작성 불가
                            ? "border-dashed border-gray-300 cursor-not-allowed opacity-50"
                            : "border-dashed border-cyan-300 hover:border-cyan-500 hover:bg-cyan-50 cursor-pointer hover:scale-105 hover:rotate-1" // 빈 셀, 메시지 작성 가능
                        }
                      `}
                      style={{
                        boxShadow: messageAtPosition
                          ? "0 2px 8px rgba(91,192,222,0.3), inset 0 1px 0 rgba(255,255,255,0.5)"
                          : "0 1px 4px rgba(91,192,222,0.1)",
                      }}
                    >
                      {messageAtPosition ? ( // 메시지가 있는 경우: 데코 아이콘 표시
                        <div className="relative">
                          <DecoIcon
                            decoType={messageAtPosition.decoType}
                            size="lg"
                            showBackground={true}
                            animate="bounce"
                          />
                          {/* 메시지 존재를 나타내는 알림 점 */}
                          <div className="absolute -top-0.5 md:-top-1 -right-0.5 md:-right-1 w-1.5 h-1.5 md:w-2 md:h-2 bg-yellow-300 rounded-full animate-ping"></div>
                          {/* 하이라이트 효과 */}
                          {isHighlighted && (
                            <div className="absolute inset-0 bg-green-300 rounded-full opacity-50 animate-ping"></div>
                          )}
                        </div>
                      ) : isOwner ? ( // 소유자인 경우: 빈 공간 (메시지 작성 불가)
                        <div className="text-gray-400 text-xs md:text-sm text-center leading-tight opacity-0"></div>
                      ) : ( // 방문자인 경우: + 아이콘으로 메시지 작성 유도
                        <div className="relative group">
                          <Plus
                            className={`w-4 h-4 md:w-5 md:h-5 transition-colors text-cyan-400 group-hover:text-cyan-600`}
                          />
                          {/* 호버 시 배경 효과 */}
                          <div
                            className={`absolute inset-0 rounded-full opacity-0 group-hover:opacity-30 transition-opacity animate-pulse bg-cyan-200`}
                          ></div>
                        </div>
                      )}
                    </div>
                  </DialogTrigger>
                  {/* 모달: 메시지가 있거나 소유자가 아닌 경우만 표시 */}
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
                      {messageAtPosition ? ( // 기존 메시지 보기
                        <MessageView
                          message={messageAtPosition}
                          isOwner={isOwner}
                          onDelete={onRefresh}
                          onDeleteSuccess={onSuccess}
                          onDeleteError={onError}
                        />
                      ) : ( // 새 메시지 작성 폼
                        onMessageSubmit && (
                          <MessageForm
                            nickname={nickname}
                            position={{ x: actualX, y: actualY }} // 백엔드 좌표 전달
                            onSubmit={(data) => handleMessageSubmit(actualX, actualY, data)}
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

      </div>
    </div>
  );
});

RollingPaperGrid.displayName = "RollingPaperGrid";
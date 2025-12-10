"use client";

import React, { useMemo, memo, useCallback, useState } from "react";
import { Modal, ModalBody, ModalHeader } from "flowbite-react";
import { Plus, ChevronLeft, ChevronRight, Sparkles, Mail, MessageSquare } from "lucide-react";
import { getDecoInfo } from "@/lib/api";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import { MessageForm } from "@/components/organisms/rolling-paper/MessageForm";
import { MessageView } from "@/components/organisms/rolling-paper/MessageView";
import { Button } from "@/components";
import { DecoIcon } from "@/components";
import { getPageAndGridPosition, findNearestEmptyPositions } from "@/lib/utils/rolling-paper";

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
  // 모달 상태 관리
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedCell, setSelectedCell] = useState<{ x: number; y: number } | null>(null);
  const [recommendedPositions, setRecommendedPositions] = useState<{ x: number; y: number }[]>([]);

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
    return { pageWidth, totalSlots };
  }, [isMobile]);




  const { pageWidth, totalSlots } = gridConfig;

  // 메시지 제출 핸들러 - 비동기 처리 및 에러 핸들링
  const handleMessageSubmit = useCallback(async (actualX: number, actualY: number, data: unknown) => {
    try {
      await onMessageSubmit?.({ x: actualX, y: actualY }, data);
      setModalOpen(false); // 성공 시에만 모달 닫기
      // 성공 메시지는 useRollingPaperMutations에서 처리하므로 제거
    } catch (error) {
      // 에러 메시지 분석
      const requestError = error as {
        response?: { data?: { message?: string } };
        message?: string;
      };
      const errorMessage = requestError.response?.data?.message || requestError.message || '';

      // 위치 정보 계산
      const { page, gridX, gridY } = getPageAndGridPosition(actualX, actualY, isMobile);
      const positionInfo = `페이지 ${page}, ${gridY + 1}번째 줄 ${gridX + 1}번째 칸`;

      if (errorMessage.includes('unique_member_x_y') || errorMessage.includes('중복')) {
        // 가장 가까운 빈 위치 찾기
        const nearestEmpty = findNearestEmptyPositions(
          messages,
          { x: actualX, y: actualY },
          3
        );
        setRecommendedPositions(nearestEmpty);

        if (nearestEmpty.length === 0) {
          onError?.(`이미 메시지가 있는 위치입니다 (${positionInfo}). 모든 위치가 가득 찼습니다.`);
        } else {
          onError?.(`이미 메시지가 있는 위치입니다 (${positionInfo}). 아래에서 가까운 빈 위치를 선택해주세요.`);
        }
      } else if (errorMessage.includes('x는 0~11') || errorMessage.includes('y는 0~9')) {
        onError?.(`잘못된 위치입니다 (${positionInfo}, x: ${actualX}, y: ${actualY})`);
      } else {
        onError?.("메시지 추가에 실패했습니다. 다시 시도해주세요.");
      }
      // 에러를 다시 throw하여 MessageForm이 제대로 catch할 수 있게 함
      throw error;
    }
  }, [onMessageSubmit, onError, isMobile, messages]);

  // 셀 클릭 핸들러
  const handleCellClick = useCallback((actualX: number, actualY: number) => {
    if (highlightedPosition && highlightedPosition.x === actualX && highlightedPosition.y === actualY) {
      onHighlightClear?.();
      return;
    }

    const messageAtPosition = getMessageAt(actualX, actualY);

    // 소유자이고 메시지가 없으면 클릭 불가능
    if (isOwner && !messageAtPosition) return;

    setSelectedCell({ x: actualX, y: actualY });
    setRecommendedPositions([]); // 새로운 셀 클릭 시 추천 위치 초기화
    setModalOpen(true);
  }, [highlightedPosition, onHighlightClear, getMessageAt, isOwner]);

  // 추천 위치로 이동
  const handleMoveToRecommended = useCallback((position: { x: number; y: number }) => {
    setModalOpen(false);
    setRecommendedPositions([]);

    // 해당 페이지로 이동
    const { page } = getPageAndGridPosition(position.x, position.y, isMobile);
    setCurrentPage(page);

    // 약간의 지연 후 모달 열기 (페이지 전환 후)
    setTimeout(() => {
      setSelectedCell({ x: position.x, y: position.y });
      setModalOpen(true);
    }, 100);
  }, [isMobile, setCurrentPage]);

  return (
    <div className={`relative max-w-5xl mx-auto mb-6 md:mb-8 ${className}`}>
      {/* 종이 배경 */}
      <div
        className="relative min-h-[600px] md:min-h-[700px] bg-gradient-to-br from-cyan-50 via-blue-50 to-teal-50 dark:from-gray-800 dark:via-gray-900 dark:to-gray-800 rounded-2xl md:rounded-3xl shadow-xl md:shadow-2xl border-2 md:border-4 border-cyan-200 dark:border-gray-700"
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
            <div className="bg-gradient-to-r from-cyan-100/90 via-blue-100/90 to-indigo-100/90 dark:from-gray-700/90 dark:via-gray-800/90 dark:to-gray-700/90 rounded-3xl p-6 md:p-8 shadow-xl border-2 border-white/80 dark:border-gray-600/80 backdrop-blur-md mb-6 relative overflow-hidden">
              {/* 배경 장식 */}
              <div className="absolute inset-0 bg-gradient-to-br from-white/20 to-transparent pointer-events-none"></div>

              <h1 className="text-xl md:text-4xl font-extrabold mb-4 flex items-center justify-center gap-3 relative z-10">
                <span className="text-cyan-700 dark:text-cyan-300 font-extrabold transform hover:scale-105 transition-transform duration-300 drop-shadow-sm">
                  {nickname}님의 롤링페이퍼
                </span>
              </h1>

              {/* 메시지 수 카드 */}
              <div className="inline-flex items-center gap-3 bg-white/80 dark:bg-gray-800/80 px-5 py-3 rounded-full shadow-lg border-2 border-cyan-200 dark:border-gray-600 relative z-10 backdrop-blur-sm">
                <Mail className="w-4 h-4 md:w-6 md:h-6 stroke-blue-500 dark:stroke-blue-400 fill-blue-200 dark:fill-blue-900 animate-bounce drop-shadow-sm" />
                <span className="text-cyan-800 dark:text-cyan-300 text-sm md:text-lg font-bold tracking-wide">
                  총 {messages.length}개의 메시지
                </span>
                <Sparkles className="w-4 h-4 md:w-6 md:h-6 stroke-yellow-500 fill-yellow-100 animate-pulse drop-shadow-sm" />
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
                className="bg-white/80 dark:bg-gray-800/80"
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>

              <span className="text-sm font-medium text-cyan-700 dark:text-cyan-300">
                {currentPage} / {totalPages}
              </span>

              <Button
                variant="outline"
                size="sm"
                onClick={handleNextPage}
                disabled={currentPage === totalPages}
                className="bg-white/80 dark:bg-gray-800/80"
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          )}

          {/* 좌표 기반 그리드 */}
          <div
            className="grid gap-2 md:gap-3 bg-white/30 dark:bg-gray-800/30 p-3 md:p-6 rounded-xl md:rounded-2xl border border-dashed md:border-2 border-cyan-300 dark:border-gray-600"
            style={{ gridTemplateColumns: `repeat(${pageWidth}, 1fr)` }}
          >
            {Array.from({ length: totalSlots }, (_, i) => {
              // 1차원 인덱스를 2차원 그리드 좌표로 변환 (0-based)
              const gridX = i % pageWidth; // 열 위치: 0, 1, 2, 3... (모바일 0~3, PC 0~5)
              const gridY = Math.floor(i / pageWidth); // 행 위치: 0, 1, 2... (0~9)

              // 현재 페이지와 그리드 위치를 전체 좌표로 변환 (0-based)
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
                <div
                  key={`grid-cell-${actualX}-${actualY}`}
                  onClick={() => handleCellClick(actualX, actualY)}
                  className={`
                        aspect-square rounded-lg md:rounded-xl border-2 md:border-3 flex items-center justify-center transition-all duration-300 relative
                        ${
                          isHighlighted // 하이라이트된 셀 (최근 작성 메시지 등)
                            ? "border-4 border-green-400 bg-gradient-to-br from-green-100 to-emerald-100 animate-pulse shadow-xl shadow-green-200 cursor-pointer"
                            : messageAtPosition // 메시지가 있는 셀
                            ? `bg-gradient-to-br ${typeof decoInfo?.color === 'string' ? decoInfo.color : ''} border-white dark:border-gray-600 shadow-md md:shadow-lg cursor-pointer hover:scale-105 md:hover:scale-110 hover:rotate-1 md:hover:rotate-3`
                            : isOwner // 롤링페이퍼 소유자인 경우 메시지 작성 불가
                            ? "border-dashed border-gray-300 dark:border-gray-600 cursor-not-allowed opacity-50"
                            : "border-dashed border-cyan-300 dark:border-gray-600 hover:border-cyan-500 dark:hover:border-cyan-400 hover:bg-cyan-50 dark:hover:bg-gray-700 cursor-pointer hover:scale-105 hover:rotate-1" // 빈 셀, 메시지 작성 가능
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
                        <div className="text-gray-400 dark:text-gray-600 text-xs md:text-sm text-center leading-tight opacity-0"></div>
                      ) : ( // 방문자인 경우: + 아이콘으로 메시지 작성 유도
                        <div className="relative group">
                          <Plus
                            className={`w-4 h-4 md:w-5 md:h-5 transition-colors text-cyan-400 dark:text-cyan-500 group-hover:text-cyan-600 dark:group-hover:text-cyan-400`}
                          />
                          {/* 호버 시 배경 효과 */}
                          <div
                            className={`absolute inset-0 rounded-full opacity-0 group-hover:opacity-30 transition-opacity animate-pulse bg-cyan-200`}
                          ></div>
                        </div>
                      )}
                </div>
              );
            })}
          </div>
        </div>

      </div>

      {/* Flowbite Modal */}
      {selectedCell && (
        <Modal
          show={modalOpen}
          onClose={() => {
            setModalOpen(false);
            setRecommendedPositions([]);
          }}
          dismissible
          size="md"
          className="modal-container"
        >
          <ModalHeader
            theme={{
              base: "flex items-center justify-between p-5 rounded-t bg-gradient-to-br from-pink-50 to-pink-100 dark:from-gray-700 dark:to-gray-800 border-b border-pink-200 dark:border-gray-600"
            }}
          >
            <div className="flex items-center space-x-2 ">
              {getMessageAt(selectedCell.x, selectedCell.y) ? (
                <>
                  <Mail className="w-4 h-4 stroke-blue-500 fill-blue-200" />
                  <span>메시지 보기</span>
                </>
              ) : (
                <>
                  <MessageSquare className="w-4 h-4 stroke-green-500 fill-green-200" />
                  <span>메시지 작성</span>
                </>
              )}
            </div>
          </ModalHeader>

          <ModalBody
            theme={{
              base: "p-6 bg-gradient-to-br from-pink-50 to-pink-100 dark:from-gray-800 dark:to-gray-900"
            }}
          >
            <div className="p-0">
              {(() => {
                const messageAtPosition = getMessageAt(selectedCell.x, selectedCell.y);
                if (messageAtPosition) {
                  // 기존 메시지 보기
                  return (
                    <MessageView
                      message={messageAtPosition}
                      isOwner={isOwner}
                      onDelete={() => {
                        onRefresh?.();
                        setModalOpen(false);
                      }}
                      onDeleteSuccess={onSuccess}
                      onDeleteError={onError}
                    />
                  );
                } else if (!isOwner && onMessageSubmit) {
                  // 새 메시지 작성 폼
                  return (
                    <>
                      <MessageForm
                        onSubmit={async (data) => {
                          await handleMessageSubmit(selectedCell.x, selectedCell.y, data);
                        }}
                        onSuccess={onSuccess}
                        onError={onError}
                      />

                      {/* 추천 위치 표시 */}
                      {recommendedPositions.length > 0 && (
                        <div className="mt-4 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-xl border-2 border-blue-200 dark:border-blue-700">
                          <p className="text-sm font-semibold text-blue-800 dark:text-blue-400 mb-3 flex items-center gap-2">
                            <Sparkles className="w-4 h-4" />
                            가까운 빈 위치
                          </p>
                          <div className="flex flex-wrap gap-2">
                            {recommendedPositions.map((pos) => {
                              const { page, gridX, gridY } = getPageAndGridPosition(pos.x, pos.y, isMobile);
                              return (
                                <Button
                                  key={`${pos.x}-${pos.y}`}
                                  variant="outline"
                                  size="sm"
                                  onClick={() => handleMoveToRecommended(pos)}
                                  className="bg-white dark:bg-gray-800 hover:bg-blue-100 dark:hover:bg-blue-900/30 border-blue-300 dark:border-blue-600 text-blue-700 dark:text-blue-400 font-medium"
                                >
                                  페이지 {page}, {gridY + 1}줄 {gridX + 1}번째
                                </Button>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </>
                  );
                }
                return null;
              })()}
            </div>
          </ModalBody>
        </Modal>
      )}
    </div>
  );
});

RollingPaperGrid.displayName = "RollingPaperGrid";
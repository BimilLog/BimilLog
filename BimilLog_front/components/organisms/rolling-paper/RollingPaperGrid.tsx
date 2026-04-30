"use client";

import React, { useMemo, memo, useCallback, useState } from "react";
import { Modal, ModalBody, ModalHeader } from "flowbite-react";
import { Plus, ChevronLeft, ChevronRight, Sparkles, Mail, MessageSquare, Lock } from "lucide-react";
import { getDecoInfo } from "@/lib/api";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import { MessageForm } from "@/components/organisms/rolling-paper/MessageForm";
import { MessageView } from "@/components/organisms/rolling-paper/MessageView";
import { Button } from "@/components";
import { DecoIcon } from "@/components";
import { getPageAndGridPosition, findNearestEmptyPositions } from "@/lib/utils/rolling-paper";

// 개별 셀 컴포넌트 - 모달 상태 변경 시 전체 셀 리렌더 방지
interface GridCellProps {
  actualX: number;
  actualY: number;
  message: RollingPaperMessage | VisitMessage | null;
  isHighlighted: boolean;
  isOwner: boolean;
  onClick: (x: number, y: number) => void;
}

const GridCell = memo(({ actualX, actualY, message, isHighlighted, isOwner, onClick }: GridCellProps) => {
  const decoInfo = message ? getDecoInfo(message.decoType) : null;

  const handleClick = useCallback(() => {
    onClick(actualX, actualY);
  }, [onClick, actualX, actualY]);

  // 접근성: 셀의 의미를 스크린리더에 전달하는 aria-label
  const ariaLabel = message
    ? `${actualY + 1}행 ${actualX + 1}열, 메시지 보기`
    : isOwner
    ? `${actualY + 1}행 ${actualX + 1}열, 빈 칸 (작성 불가)`
    : `${actualY + 1}행 ${actualX + 1}열, 빈 칸, 메시지 작성하기`;

  const isDisabled = isOwner && !message;

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={isDisabled}
      aria-label={ariaLabel}
      data-testid="grid-cell"
      data-x={actualX}
      data-y={actualY}
      data-locked={message && !isOwner ? 'true' : undefined}
      className={`
        aspect-square rounded-lg md:rounded-xl border-2 md:border-3 flex items-center justify-center transition-all duration-300 relative focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary focus-visible:ring-offset-2
        ${
          isHighlighted
            ? "border-4 border-green-400 bg-gradient-to-br from-green-100 to-emerald-100 animate-pulse shadow-xl shadow-green-200 cursor-pointer"
            : message
            ? `bg-gradient-to-br ${typeof decoInfo?.color === 'string' ? decoInfo.color : ''} border-white dark:border-gray-600 shadow-md md:shadow-lg cursor-pointer hover:scale-105 md:hover:scale-110 hover:rotate-1 md:hover:rotate-3`
            : isOwner
            ? "border-dashed border-gray-300 dark:border-gray-600 cursor-not-allowed opacity-50"
            : "border-dashed border-sky-300 dark:border-sky-600 hover:border-sky-500 dark:hover:border-sky-400 hover:bg-sky-50 dark:hover:bg-sky-800/50 cursor-pointer hover:scale-105 hover:rotate-1"
        }
      `}
      style={{
        boxShadow: message
          ? "0 2px 8px rgba(91,192,222,0.3), inset 0 1px 0 rgba(255,255,255,0.5)"
          : "0 1px 4px rgba(91,192,222,0.1)",
      }}
    >
      {message ? (
        <div className="relative">
          {/* 셀 애니메이션 축소: DecoIcon 의 bounce 만 사용. animate-ping (yellow dot) 은 제거하여 한 셀에 두 애니메이션이 겹치지 않도록 함. */}
          <DecoIcon
            decoType={message.decoType}
            size="lg"
            showBackground={true}
            animate="bounce"
          />
          {/* 비-소유자(방문자) 시점에서는 잠금 뱃지 노출 - 본인이 아니면 content/anonymity 미노출이므로 잠긴 메시지임을 표시 */}
          {!isOwner && (
            <span
              data-testid="grid-cell-locked"
              data-locked="true"
              className="absolute -top-0.5 md:-top-1 -right-0.5 md:-right-1 w-3 h-3 md:w-3.5 md:h-3.5 rounded-full bg-white/95 border border-gray-300 flex items-center justify-center shadow-sm"
              aria-label="잠긴 메시지"
            >
              <Lock className="w-2 h-2 md:w-2.5 md:h-2.5 stroke-gray-600" aria-hidden="true" />
            </span>
          )}
          {isHighlighted && (
            <div className="absolute inset-0 bg-green-300 rounded-full opacity-50 animate-pulse"></div>
          )}
        </div>
      ) : isOwner ? (
        <div className="text-gray-400 dark:text-gray-600 text-xs md:text-sm text-center leading-tight opacity-0"></div>
      ) : (
        <div className="relative group">
          <Plus
            className="w-4 h-4 md:w-5 md:h-5 transition-colors text-sky-400 dark:text-sky-500 group-hover:text-sky-600 dark:group-hover:text-sky-400"
          />
        </div>
      )}
    </button>
  );
});

GridCell.displayName = "GridCell";

interface RollingPaperGridProps {
  messages: (RollingPaperMessage | VisitMessage)[];
  nickname: string;
  isOwner: boolean;
  isMobile: boolean;
  totalPages: number;
  currentPage: number;
  setCurrentPage: (page: number) => void;
  onMessageSubmit?: (position: { x: number; y: number }, data: unknown) => Promise<void>;
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
      } else if (errorMessage.includes('x는 0~11') || errorMessage.includes('y는 0~9') || /0\s*~\s*\d+/.test(errorMessage)) {
        // raw 좌표 범위 표현(0~11, 0~9 등)은 사용자에게 노출하지 않고
        // 페이지/행/칸 형태의 사용자 친화적 메시지로 대체
        onError?.(`선택한 위치(${positionInfo})에 메시지를 작성할 수 없습니다. 다른 위치를 선택해주세요.`);
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

  // 빈 상태 CTA(방문자) — 첫 빈 셀 좌표를 직접 계산해서 모달을 연다.
  // P-003: querySelector + click() 직접 DOM 조작은 race condition / 모바일 사파리 smooth scroll 마찰 유발.
  // 첫 빈 셀은 currentPage 의 (0, 0) — 빈 상태(messages.length === 0)에서는 항상 유효.
  const handleOpenFirstEmpty = useCallback(() => {
    const { x, y } = getCoordsFromPageAndGrid(currentPage, 0, 0);
    setSelectedCell({ x, y });
    setRecommendedPositions([]);
    setModalOpen(true);
  }, [getCoordsFromPageAndGrid, currentPage]);

  // 빈 상태 CTA(소유자) — 링크 복사 + 토스트 피드백 (F-003)
  const handleCopyShareLink = useCallback(async () => {
    if (typeof window === 'undefined') return;
    const url = window.location.href;
    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(url);
      } else {
        // 폴백: 임시 textarea + execCommand
        const textarea = document.createElement('textarea');
        textarea.value = url;
        textarea.setAttribute('readonly', '');
        textarea.style.position = 'absolute';
        textarea.style.left = '-9999px';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
      }
      onSuccess?.('링크가 복사되었어요. 친구에게 보내고 첫 편지를 받아보세요!');
    } catch {
      onError?.('링크 복사에 실패했어요. 주소창을 직접 복사해주세요.');
    }
  }, [onSuccess, onError]);

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
    <div className={`relative container-paper mb-6 md:mb-8 ${className}`}>
      {/* 종이 배경 — cream/parchment + grain */}
      <div
        className="relative min-h-[600px] md:min-h-[700px] bg-paper rounded-xl md:rounded-2xl shadow-brand-lg border border-ink-soft"
      >

        {/* 제목 영역 */}
        <div className="pt-6 md:pt-10 pb-4 md:pb-6 px-4 md:px-20 text-center">
          <div className="relative">
            {/* 편지지 톤 제목 카드 */}
            <div className="bg-paper-card rounded-2xl p-6 md:p-8 shadow-brand-md border border-ink-soft mb-6 relative overflow-hidden washi-tape">
              <h1 className="text-2xl md:text-4xl mb-4 flex items-center justify-center gap-3 relative z-10">
                <span className="font-handwriting font-bold text-stamp-red">
                  {nickname}
                </span>
                <span className="font-display font-bold text-ink dark:text-sky-300">
                  님의 롤링페이퍼
                </span>
              </h1>

              {/* 메시지 수 카드 */}
              <div className="inline-flex items-center gap-3 bg-paper-soft px-5 py-2.5 rounded-full border border-ink-soft relative z-10">
                <Mail className="w-4 h-4 md:w-5 md:h-5 stroke-stamp-red" />
                <span className="font-display text-ink text-sm md:text-base font-semibold tracking-wide">
                  총 {messages.length}통의 편지
                </span>
                <Sparkles className="w-4 h-4 md:w-5 md:h-5 stroke-[var(--color-seal-gold,#C99B5C)]" />
              </div>
            </div>

          </div>
        </div>

        {/* 빈 상태 CTA: 메시지가 0개일 때 큰 안내 영역 노출 (3종 세트: 일러스트 + 카피 + primary CTA) */}
        {messages.length === 0 && (
          <section
            data-testid="paper-empty-state"
            role="region"
            aria-labelledby="paper-empty-state-title"
            className="mx-4 md:mx-20 mb-6 p-8 md:p-12 rounded-2xl bg-paper-card border-dashed-paper text-center"
          >
            {/* 큰 편지 일러스트 */}
            <div className="w-24 h-24 md:w-32 md:h-32 mx-auto mb-5 relative animate-paper-float" aria-hidden="true">
              <svg viewBox="0 0 120 120" className="w-full h-full">
                <rect x="14" y="34" width="92" height="62" rx="4" fill="#FFFDF7" stroke="#2A1F1A" strokeWidth="2.5" />
                <polyline points="14,34 60,72 106,34" fill="none" stroke="#2A1F1A" strokeWidth="2.5" />
                <circle cx="92" cy="50" r="9" fill="#C73E3E" />
                <text x="92" y="54" textAnchor="middle" fontSize="9" fill="#FFFDF7" fontFamily="serif" fontWeight="700">FIRST</text>
                <line x1="22" y1="20" x2="34" y2="32" stroke="#C73E3E" strokeWidth="2" strokeLinecap="round" />
                <line x1="40" y1="14" x2="46" y2="26" stroke="#C73E3E" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </div>
            <h2
              id="paper-empty-state-title"
              className="font-display text-xl md:text-2xl font-bold text-ink dark:text-sky-200 mb-2"
            >
              아직 도착한 편지가 없어요
            </h2>
            <p className="font-body text-sm md:text-base text-ink-soft dark:text-sky-300 leading-relaxed max-w-md mx-auto">
              {isOwner
                ? '친구들에게 롤링페이퍼 링크를 공유하고 첫 편지를 받아보세요.'
                : `${nickname}님에게 첫 편지를 남겨보세요. 한 줄의 마음이 큰 응원이 됩니다.`}
            </p>
            {!isOwner && (
              <button
                type="button"
                onClick={handleOpenFirstEmpty}
                className="mt-5 inline-flex items-center justify-center min-h-touch px-6 py-3 rounded-md bg-paper-button text-white font-semibold shadow-brand-sm hover:bg-paper-hover transition-colors"
              >
                <MessageSquare className="w-5 h-5 mr-2" />첫 메시지 남기기
              </button>
            )}
            {isOwner && (
              <button
                type="button"
                onClick={handleCopyShareLink}
                className="mt-5 inline-flex items-center justify-center min-h-touch px-6 py-3 rounded-md bg-paper-button text-white font-semibold shadow-brand-sm hover:bg-paper-hover transition-colors"
              >
                <MessageSquare className="w-5 h-5 mr-2" />링크 복사하고 친구에게 공유
              </button>
            )}
          </section>
        )}

        {/* 메시지 그리드 — 빈 상태일 때는 fade-out + 키보드/포인터 차단 (F-004 inert) */}
        <div
          data-testid="paper-grid-container"
          // @ts-expect-error: React 19 부터 inert 가 boolean 으로 정식 지원되지만 일부 타입 정의 누락 대응
          inert={messages.length === 0 ? '' : undefined}
          aria-hidden={messages.length === 0 ? true : undefined}
          className={`px-4 md:px-20 pb-4 md:pb-6 ${messages.length === 0 ? 'opacity-30 pointer-events-none mask-fade-bottom' : ''}`}
        >
          {/* 페이지 네비게이션 */}
          {totalPages > 1 && (
            <nav
              aria-label="롤링페이퍼 페이지 탐색"
              className="flex items-center justify-center gap-4 mb-4"
            >
              <Button
                variant="outline"
                size="sm"
                onClick={handlePreviousPage}
                disabled={currentPage === 1}
                aria-label="이전 페이지"
                className="bg-white/80 dark:bg-gray-800/80"
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>

              <span
                className="text-sm font-medium text-sky-700 dark:text-sky-300"
                aria-current="page"
                aria-live="polite"
              >
                {currentPage} / {totalPages}
              </span>

              <Button
                variant="outline"
                size="sm"
                onClick={handleNextPage}
                disabled={currentPage === totalPages}
                aria-label="다음 페이지"
                className="bg-white/80 dark:bg-gray-800/80"
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </nav>
          )}

          {/* 좌표 기반 그리드 */}
          <div
            className="grid gap-2 md:gap-3 bg-white/50 dark:bg-sky-800/30 p-3 md:p-6 rounded-xl md:rounded-2xl border border-dashed md:border-2 border-sky-300 dark:border-sky-600"
            style={{ gridTemplateColumns: `repeat(${pageWidth}, 1fr)` }}
          >
            {Array.from({ length: totalSlots }, (_, i) => {
              const gridX = i % pageWidth;
              const gridY = Math.floor(i / pageWidth);
              const { x: actualX, y: actualY } = getCoordsFromPageAndGrid(
                currentPage,
                gridX,
                gridY
              );
              const messageAtPosition = getMessageAt(actualX, actualY);
              const isHighlighted = !!(highlightedPosition &&
                highlightedPosition.x === actualX &&
                highlightedPosition.y === actualY);

              return (
                <GridCell
                  key={`grid-cell-${actualX}-${actualY}`}
                  actualX={actualX}
                  actualY={actualY}
                  message={messageAtPosition}
                  isHighlighted={isHighlighted}
                  isOwner={isOwner}
                  onClick={handleCellClick}
                />
              );
            })}
          </div>
        </div>

      </div>

      {/* Flowbite Modal — F-001: 명시적 ARIA 부여 */}
      {selectedCell && (() => {
        const isViewingMessage = !!getMessageAt(selectedCell.x, selectedCell.y);
        const modalTitleId = "rolling-paper-modal-title";
        return (
          <Modal
            show={modalOpen}
            onClose={() => {
              setModalOpen(false);
              setRecommendedPositions([]);
            }}
            dismissible
            size="md"
            className="modal-container"
            role="dialog"
            aria-modal="true"
            aria-labelledby={modalTitleId}
          >
            <ModalHeader
              theme={{
                base: "flex items-center justify-between p-5 rounded-t bg-gradient-to-br from-pink-50 to-pink-100 dark:from-gray-700 dark:to-gray-800 border-b border-pink-200 dark:border-gray-600"
              }}
            >
              <div id={modalTitleId} className="flex items-center space-x-2 ">
                {isViewingMessage ? (
                  <>
                    <Mail className="w-4 h-4 stroke-blue-500 fill-blue-200" aria-hidden="true" />
                    <span>메시지 보기</span>
                  </>
                ) : (
                  <>
                    <MessageSquare className="w-4 h-4 stroke-green-500 fill-green-200" aria-hidden="true" />
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
                              <Sparkles className="w-4 h-4" aria-hidden="true" />
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
        );
      })()}
    </div>
  );
});

RollingPaperGrid.displayName = "RollingPaperGrid";
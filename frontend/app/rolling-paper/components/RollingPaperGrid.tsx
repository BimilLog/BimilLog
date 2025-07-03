"use client";

import React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Plus, Lock } from "lucide-react";
import {
  getDecoInfo,
  type RollingPaperMessage,
  type VisitMessage,
} from "@/lib/api";
import { MessageForm } from "./MessageForm";
import { MessageView } from "./MessageView";
import { PageNavigation } from "./PageNavigation";

interface RollingPaperGridProps {
  messages: { [key: number]: RollingPaperMessage | VisitMessage };
  nickname: string;
  currentPage: number;
  totalPages: number;
  colsPerPage: number;
  rowsPerPage: number;
  slotsPerPage: number;
  isOwner: boolean;
  onPageChange: (page: number) => void;
  onMessageSubmit?: (position: { x: number; y: number }, data: any) => void;
  className?: string;
}

export const RollingPaperGrid: React.FC<RollingPaperGridProps> = ({
  messages,
  nickname,
  currentPage,
  totalPages,
  colsPerPage,
  rowsPerPage,
  slotsPerPage,
  isOwner,
  onPageChange,
  onMessageSubmit,
  className = "",
}) => {
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
            <h1 className="text-lg md:text-3xl font-bold text-cyan-800 mb-2 transform -rotate-1">
              🌊 {nickname}님의 롤링페이퍼 🌊
            </h1>

            {/* 시원한 데코레이션 */}
            <div className="absolute -top-1 md:-top-2 -left-2 md:-left-4 text-lg md:text-2xl animate-bounce">
              ❄️
            </div>
            <div className="absolute -top-1 -right-3 md:-right-6 text-sm md:text-xl animate-pulse">
              ✨
            </div>
            <div className="absolute -bottom-1 md:-bottom-2 left-4 md:left-8 text-sm md:text-lg animate-bounce delay-300">
              🧊
            </div>
            <div className="absolute -bottom-1 right-6 md:right-12 text-sm md:text-xl animate-pulse delay-500">
              💎
            </div>

            <p className="text-cyan-600 text-xs md:text-sm mt-2 transform rotate-1 font-medium">
              총 {Object.keys(messages).length}개의 시원한 메시지 💌
            </p>
          </div>
        </div>

        {/* 메시지 그리드 */}
        <div className="px-12 md:px-20 pb-4 md:pb-6">
          {/* 페이지 네비게이션 */}
          <PageNavigation
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={onPageChange}
            className="mb-4"
          />

          {/* 그리드 */}
          <div className="grid grid-cols-4 md:grid-cols-6 gap-2 md:gap-3 bg-white/30 p-3 md:p-6 rounded-xl md:rounded-2xl border border-dashed md:border-2 border-cyan-300">
            {Array.from({ length: slotsPerPage }, (_, i) => {
              // 현재 페이지의 메시지만 필터링
              const pageMessages = Object.entries(messages).filter(
                ([_, message]) => {
                  const messagePage =
                    Math.floor(message.width / colsPerPage) + 1;
                  return messagePage === currentPage;
                }
              );

              // 현재 슬롯에 해당하는 메시지 찾기
              const slotMessage = pageMessages.find(([_, message]) => {
                const pageWidth = message.width % colsPerPage;
                const position = message.height * colsPerPage + pageWidth;
                return position === i;
              });

              const hasMessage = slotMessage ? slotMessage[1] : null;
              const decoInfo = hasMessage
                ? getDecoInfo(hasMessage.decoType)
                : null;

              // 좌표 계산
              const x = (currentPage - 1) * colsPerPage + (i % colsPerPage);
              const y = Math.floor(i / colsPerPage);

              return (
                <Dialog key={i}>
                  <DialogTrigger asChild>
                    <div
                      className={`
                        aspect-square rounded-lg md:rounded-xl border-2 md:border-3 flex items-center justify-center transition-all duration-300 
                        ${
                          hasMessage
                            ? `bg-gradient-to-br ${decoInfo?.color} border-white shadow-md md:shadow-lg cursor-pointer hover:scale-105 md:hover:scale-110 hover:rotate-1 md:hover:rotate-3`
                            : isOwner
                            ? "border-dashed border-gray-300 cursor-not-allowed opacity-50"
                            : "border-dashed border-cyan-300 hover:border-cyan-500 hover:bg-cyan-50 cursor-pointer hover:scale-105 hover:rotate-1"
                        }
                      `}
                      style={{
                        boxShadow: hasMessage
                          ? "0 2px 8px rgba(91,192,222,0.3), inset 0 1px 0 rgba(255,255,255,0.5)"
                          : "0 1px 4px rgba(91,192,222,0.1)",
                      }}
                    >
                      {hasMessage ? (
                        <div className="relative">
                          <span className="text-lg md:text-2xl animate-bounce">
                            {decoInfo?.emoji}
                          </span>
                          <div className="absolute -top-0.5 md:-top-1 -right-0.5 md:-right-1 w-1.5 h-1.5 md:w-2 md:h-2 bg-yellow-300 rounded-full animate-ping"></div>
                        </div>
                      ) : isOwner ? (
                        <div className="text-gray-400 text-xs md:text-sm text-center leading-tight opacity-0"></div>
                      ) : (
                        <div className="relative group">
                          <Plus className="w-4 h-4 md:w-5 md:h-5 text-cyan-400 group-hover:text-cyan-600 transition-colors" />
                          <div className="absolute inset-0 bg-cyan-200 rounded-full opacity-0 group-hover:opacity-30 transition-opacity animate-pulse"></div>
                        </div>
                      )}
                    </div>
                  </DialogTrigger>
                  {(hasMessage || !isOwner) && (
                    <DialogContent className="max-w-sm md:max-w-md mx-auto bg-gradient-to-br from-cyan-50 to-blue-50 border-2 md:border-4 border-cyan-200 rounded-2xl md:rounded-3xl">
                      <DialogHeader>
                        <DialogTitle className="text-center text-cyan-800 font-bold text-sm md:text-base">
                          {hasMessage ? "💌 메시지 보기" : "✨ 새 메시지 작성"}
                        </DialogTitle>
                      </DialogHeader>
                      {hasMessage ? (
                        <MessageView message={hasMessage} isOwner={isOwner} />
                      ) : (
                        onMessageSubmit && (
                          <MessageForm
                            nickname={nickname}
                            position={{ x, y }}
                            onSubmit={(data) => onMessageSubmit({ x, y }, data)}
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
        <div className="absolute top-8 md:top-16 right-4 md:right-8 text-xl md:text-3xl animate-spin-slow">
          🌟
        </div>
        <div className="absolute top-16 md:top-32 left-8 md:left-12 text-lg md:text-2xl animate-bounce">
          🐋
        </div>
        <div className="absolute bottom-12 md:bottom-20 right-8 md:right-16 text-lg md:text-2xl animate-pulse">
          🌀
        </div>
        <div className="absolute bottom-16 md:bottom-32 left-4 md:left-8 text-base md:text-xl animate-bounce delay-700">
          🏄‍♂️
        </div>
      </div>
    </div>
  );
};

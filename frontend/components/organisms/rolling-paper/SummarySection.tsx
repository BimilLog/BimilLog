import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { Badge } from "@/components";
import { MessageSquare, Waves, FileText, Mail, Lock, Info } from "lucide-react";
import { getDecoInfo } from "@/lib/api";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import { DecoIcon } from "@/components";
import { cn } from "@/lib/utils";
import { SummarySectionClient } from "./SummarySectionClient";

interface SummarySectionProps {
  // InfoCard props
  isOwner: boolean;
  nickname: string;
  
  // RecentMessages props
  messages: (RollingPaperMessage | VisitMessage)[];
  onShare?: () => void;
  onMessageClick?: (message: RollingPaperMessage | VisitMessage) => void;
  className?: string;
}

export const SummarySection: React.FC<SummarySectionProps> = ({
  isOwner,
  nickname,
  messages,
  onShare,
  onMessageClick,
  className = "",
}) => {
  // RollingPaperMessage 타입 가드
  const isRollingPaperMessage = (
    msg: RollingPaperMessage | VisitMessage
  ): msg is RollingPaperMessage => {
    return "content" in msg && "anonymity" in msg;
  };

  // 정적 정보 섹션 (서버 컴포넌트)
  const InfoSection = (
    <Card
      variant="elevated"
      className={cn(
        "text-brand-primary transition-all duration-300 rounded-2xl border-2 border-cyan-200 mb-6",
        className
      )}
    >
      <CardContent className="p-4">
      <div className="flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="text-center md:text-left">
          {isOwner ? (
            <div className="flex items-start space-x-3">
              <Info className="w-5 h-5 text-cyan-600 mt-0.5 flex-shrink-0" />
              <div>
                <p className="text-cyan-800 font-semibold text-sm md:text-base flex items-center space-x-2">
                  <span>내 롤링페이퍼 보기 모드</span>
                  <Waves className="w-4 h-4" />
                </p>
                <p className="text-cyan-700 text-xs md:text-sm mt-1">
                  이곳은 나에게 온 메시지들을 볼 수 있는 공간이에요.
                  <span className="block md:inline flex items-center space-x-1">
                    <span>친구들에게 공유하여 메시지를 받아보세요!</span>
                    <Mail className="w-3 h-3 md:w-4 md:h-4" />
                  </span>
                </p>
              </div>
            </div>
          ) : (
            <div className="flex items-start space-x-3">
              <Info className="w-5 h-5 text-cyan-600 mt-0.5 flex-shrink-0" />
              <div>
                <p className="text-cyan-800 font-semibold text-sm md:text-base flex items-center space-x-2">
                  <span>{nickname}님에게 메시지를 남겨보세요!</span>
                  <Waves className="w-4 h-4" />
                </p>
                <p className="text-cyan-700 text-xs md:text-sm mt-1">
                  빈 칸을 클릭하여 익명으로 따뜻한 메시지를 남길 수 있어요.
                  <span className="block md:inline flex items-center space-x-1">
                    <span>다양한 귀여운 디자인으로 꾸며보세요!</span>
                    <Mail className="w-3 h-3 md:w-4 md:h-4" />
                  </span>
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
      </CardContent>
    </Card>
  );

  // 메시지 목록 표시 부분 (서버 컴포넌트)
  const RecentMessagesDisplay = (
    <Card variant="elevated" className="rounded-2xl md:rounded-3xl border-2 md:border-4 border-cyan-200">
      <CardHeader className="bg-gradient-to-r from-cyan-100 to-blue-100 rounded-t-2xl md:rounded-t-3xl p-4 pb-2">
        <CardTitle className="flex items-center space-x-2 text-cyan-800 text-sm md:text-base">
          <MessageSquare className="w-4 h-4 md:w-5 md:h-5" />
          <span className="font-bold">최근 메시지들</span>
          <Waves className="w-4 h-4 md:w-5 md:h-5" />
        </CardTitle>
      </CardHeader>
      <CardContent className="p-4 pt-3 pb-3">
        {messages.length === 0 ? (
          <div className="text-center py-8 md:py-12">
            <div className="w-16 h-16 md:w-20 md:h-20 mx-auto mb-4 bg-gradient-to-r from-cyan-100 to-blue-100 rounded-full flex items-center justify-center">
              <FileText className="w-8 h-8 md:w-10 md:h-10 text-cyan-600" />
            </div>
            <p className="text-brand-secondary text-base md:text-lg font-semibold">
              아직 메시지가 없어요
            </p>
            <p className="text-brand-secondary text-xs md:text-sm mt-2 font-medium px-4 flex items-center justify-center space-x-1">
              <span>친구들에게 롤링페이퍼를 공유해보세요!</span>
              <Mail className="w-3 h-3 md:w-4 md:h-4" />
            </p>
            {/* 공유 버튼은 클라이언트 컴포넌트로 위임 */}
            {isOwner && onShare && (
              <SummarySectionClient onShare={onShare} />
            )}
          </div>
        ) : (
          <div className="space-y-3">
            {messages.map((message) => {
              const decoInfo = getDecoInfo(message.decoType);
              return (
                <SummarySectionClient
                  key={message.id}
                  message={message}
                  decoInfo={decoInfo}
                  isOwner={isOwner}
                  isRollingPaperMessage={isRollingPaperMessage(message)}
                  onMessageClick={onMessageClick}
                />
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );

  return (
    <div className="space-y-6">
      {InfoSection}
      {RecentMessagesDisplay}
    </div>
  );
};
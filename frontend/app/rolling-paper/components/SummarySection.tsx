"use client";

import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { Badge } from "@/components";
import { Button } from "@/components";
import { MessageSquare, Share2, Waves, FileText, Mail, Lock, Info } from "lucide-react";
import { getDecoInfo } from "@/lib/api";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import { DecoIcon } from "@/components";
import { cn } from "@/lib/utils";

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

export const SummarySection: React.FC<SummarySectionProps> = React.memo(({
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

  const InfoSection = React.useMemo(() => (
    <div
      className={cn(
        "text-gray-800 transition-all duration-300 p-4 bg-white/80 backdrop-blur-sm shadow-lg rounded-2xl border-2 border-cyan-200 mb-6",
        className
      )}
    >
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
    </div>
  ), [isOwner, nickname, className]);

  const RecentMessagesSection = React.useMemo(() => {
    if (messages.length === 0) {
      return (
        <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg md:shadow-xl rounded-2xl md:rounded-3xl border-2 md:border-4 border-cyan-200">
          <CardHeader className="bg-gradient-to-r from-cyan-100 to-blue-100 rounded-t-2xl md:rounded-3xl p-4 md:p-6">
            <CardTitle className="flex items-center space-x-2 text-cyan-800 text-sm md:text-base">
              <MessageSquare className="w-4 h-4 md:w-5 md:h-5" />
              <span className="font-bold">최근 메시지들</span>
              <Waves className="w-4 h-4 md:w-5 md:h-5" />
            </CardTitle>
          </CardHeader>
          <CardContent className="p-4 md:p-6">
            <div className="text-center py-8 md:py-12">
              <div className="w-16 h-16 md:w-20 md:h-20 mx-auto mb-4 bg-gradient-to-r from-cyan-100 to-blue-100 rounded-full flex items-center justify-center">
                <FileText className="w-8 h-8 md:w-10 md:h-10 text-cyan-600" />
              </div>
              <p className="text-gray-500 text-base md:text-lg font-semibold">
                아직 메시지가 없어요
              </p>
              <p className="text-gray-400 text-xs md:text-sm mt-2 font-medium px-4 flex items-center justify-center space-x-1">
                <span>친구들에게 롤링페이퍼를 공유해보세요!</span>
                <Mail className="w-3 h-3 md:w-4 md:h-4" />
              </p>
              {isOwner && onShare && (
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-4 bg-cyan-50 border-cyan-300 text-cyan-700 hover:bg-cyan-100"
                  onClick={onShare}
                >
                  <Share2 className="w-4 h-4 mr-2" />
                  지금 공유하기
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      );
    }

    return (
      <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg md:shadow-xl rounded-2xl md:rounded-3xl border-2 md:border-4 border-cyan-200">
        <CardHeader className="bg-gradient-to-r from-cyan-100 to-blue-100 rounded-t-2xl md:rounded-t-3xl p-4 pb-2">
          <CardTitle className="flex items-center space-x-2 text-cyan-800 text-sm md:text-base">
            <MessageSquare className="w-4 h-4 md:w-5 md:h-5" />
            <span className="font-bold">최근 메시지들</span>
            <Waves className="w-4 h-4 md:w-5 md:h-5" />
          </CardTitle>
        </CardHeader>
        <CardContent className="p-4 pt-3 pb-3">
          <div className="space-y-3">
            {messages.map((message) => {
              const decoInfo = getDecoInfo(message.decoType);
              return (
                <div
                  key={message.id}
                  onClick={() => onMessageClick && onMessageClick(message)}
                  className={`flex items-start space-x-3 p-3 md:p-4 rounded-xl md:rounded-2xl bg-gradient-to-r from-cyan-50 to-blue-50 border border-cyan-200 md:border-2 transform hover:scale-105 transition-transform ${
                    onMessageClick ? "cursor-pointer" : ""
                  }`}
                >
                  <div
                    className={`w-10 h-10 md:w-12 md:h-12 rounded-full bg-gradient-to-r ${decoInfo.color} flex items-center justify-center shadow-lg border-2 border-white flex-shrink-0`}
                  >
                    <DecoIcon decoType={message.decoType} size="md" showBackground={false} />
                  </div>
                  <div className="flex-1 min-w-0">
                    {isRollingPaperMessage(message) && isOwner ? (
                      <p className="text-gray-800 text-sm md:text-base font-medium leading-relaxed">
                        {message.content}
                      </p>
                    ) : (
                      <p className="text-gray-600 text-sm md:text-base font-medium leading-relaxed italic flex items-center space-x-2">
                        <Lock className="w-4 h-4" />
                        <span>메시지 내용은 작성자만 볼 수 있습니다</span>
                      </p>
                    )}
                    <div className="flex items-center space-x-2 mt-2">
                      {isRollingPaperMessage(message) && isOwner && (
                        <Badge
                          variant="outline"
                          className="text-xs bg-white border-cyan-300"
                        >
                          {message.anonymity}
                        </Badge>
                      )}
                      <span className="text-xs text-gray-500 font-medium">
                        {decoInfo.name}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>
    );
  }, [messages, onMessageClick, isOwner, onShare]);

  return (
    <div className="space-y-6">
      {InfoSection}
      {RecentMessagesSection}
    </div>
  );
});

SummarySection.displayName = "SummarySection";
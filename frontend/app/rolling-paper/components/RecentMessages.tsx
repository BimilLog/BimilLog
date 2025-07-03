"use client";

import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { MessageSquare, Share2 } from "lucide-react";
import {
  getDecoInfo,
  type RollingPaperMessage,
  type VisitMessage,
} from "@/lib/api";

interface RecentMessagesProps {
  messages: (RollingPaperMessage | VisitMessage)[];
  isOwner: boolean;
  onShare?: () => void;
  className?: string;
}

export const RecentMessages: React.FC<RecentMessagesProps> = ({
  messages,
  isOwner,
  onShare,
  className = "",
}) => {
  // RollingPaperMessage 타입 가드
  const isRollingPaperMessage = (
    msg: RollingPaperMessage | VisitMessage
  ): msg is RollingPaperMessage => {
    return "content" in msg && "anonymity" in msg;
  };

  if (messages.length === 0) {
    return (
      <Card
        className={`bg-white/80 backdrop-blur-sm border-0 shadow-lg md:shadow-xl rounded-2xl md:rounded-3xl border-2 md:border-4 border-cyan-200 ${className}`}
      >
        <CardHeader className="bg-gradient-to-r from-cyan-100 to-blue-100 rounded-t-2xl md:rounded-t-3xl p-4 md:p-6">
          <CardTitle className="flex items-center space-x-2 text-cyan-800 text-sm md:text-base">
            <MessageSquare className="w-4 h-4 md:w-5 md:h-5" />
            <span className="font-bold">최근 메시지들 🌊</span>
          </CardTitle>
        </CardHeader>
        <CardContent className="p-4 md:p-6">
          <div className="text-center py-8 md:py-12">
            <div className="text-4xl md:text-6xl mb-4">📝</div>
            <p className="text-gray-500 text-base md:text-lg font-semibold">
              아직 메시지가 없어요
            </p>
            <p className="text-gray-400 text-xs md:text-sm mt-2 font-medium px-4">
              친구들에게 롤링페이퍼를 공유해보세요! 💌
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
    <Card
      className={`bg-white/80 backdrop-blur-sm border-0 shadow-lg md:shadow-xl rounded-2xl md:rounded-3xl border-2 md:border-4 border-cyan-200 ${className}`}
    >
      <CardHeader className="bg-gradient-to-r from-cyan-100 to-blue-100 rounded-t-2xl md:rounded-t-3xl p-4 md:p-6">
        <CardTitle className="flex items-center space-x-2 text-cyan-800 text-sm md:text-base">
          <MessageSquare className="w-4 h-4 md:w-5 md:h-5" />
          <span className="font-bold">최근 메시지들 🌊</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="p-4 md:p-6">
        <div className="space-y-3 md:space-y-4">
          {messages.map((message) => {
            const decoInfo = getDecoInfo(message.decoType);
            return (
              <div
                key={message.id}
                className="flex items-start space-x-3 p-3 md:p-4 rounded-xl md:rounded-2xl bg-gradient-to-r from-cyan-50 to-blue-50 border border-cyan-200 md:border-2 transform hover:scale-105 transition-transform"
              >
                <div
                  className={`w-10 h-10 md:w-12 md:h-12 rounded-full bg-gradient-to-r ${decoInfo.color} flex items-center justify-center shadow-lg border-2 border-white flex-shrink-0`}
                >
                  <span className="text-lg md:text-xl">{decoInfo.emoji}</span>
                </div>
                <div className="flex-1 min-w-0">
                  {isRollingPaperMessage(message) && isOwner ? (
                    <p className="text-gray-800 text-sm md:text-base font-medium leading-relaxed">
                      {message.content}
                    </p>
                  ) : (
                    <p className="text-gray-600 text-sm md:text-base font-medium leading-relaxed italic">
                      메시지 내용은 작성자만 볼 수 있습니다 🔒
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
};

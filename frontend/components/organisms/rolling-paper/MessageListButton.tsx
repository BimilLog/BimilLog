"use client";

import React from "react";
import { Button } from "flowbite-react";
import { Card, CardContent } from "@/components";
import { MessageSquare, List, Info } from "lucide-react";
import { Badge } from "@/components";

interface MessageListButtonProps {
  isOwner: boolean;
  nickname: string;
  messageCount: number;
  onOpenMessageList: () => void;
  className?: string;
}

export const MessageListButton: React.FC<MessageListButtonProps> = ({
  isOwner,
  nickname,
  messageCount,
  onOpenMessageList,
  className = "",
}) => {
  // 소유자가 아닐 경우에만 안내 메시지 표시
  if (!isOwner) {
    return (
      <Card
        variant="elevated"
        className={`text-brand-primary transition-all duration-300 rounded-2xl border-2 border-cyan-200 ${className}`}
      >
        <CardContent className="p-4">
          <div className="flex items-start space-x-3">
            <Info className="w-5 h-5 text-cyan-600 mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-cyan-800 font-semibold text-sm md:text-base">
                {nickname}님에게 메시지를 남겨보세요!
              </p>
              <p className="text-cyan-700 text-xs md:text-sm mt-1">
                빈 칸을 클릭하여 익명으로 따뜻한 메시지를 남길 수 있어요.
                <span className="block">
                  다양한 귀여운 디자인으로 꾸며보세요!
                </span>
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  // 소유자인 경우 버튼만 표시
  return (
    <div className={`flex justify-center ${className}`}>
      <Button
        color="info"
        onClick={onOpenMessageList}
        size="lg"
        className="group bg-gradient-to-r from-cyan-500 to-blue-500 text-white"
      >
        <List className="w-5 h-5 mr-2 group-hover:rotate-12 transition-transform" />
        메시지 목록 보기
        {messageCount > 0 && (
          <Badge className="ml-2 bg-white/20 text-white border-white/30">
            {messageCount}개
          </Badge>
        )}
      </Button>
    </div>
  );
};
"use client";

import React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Calendar, MessageSquare, Sparkles, Share2 } from "lucide-react";
import { getDecoInfo, type RollingPaperMessage } from "@/lib/api";
import { DecoIcon } from "@/components";
import { formatRelativeDate } from "@/lib/date-utils";

interface MessageListModalProps {
  isOpen: boolean;
  onClose: () => void;
  messages: RollingPaperMessage[];
  onMessageClick: (message: RollingPaperMessage) => void;
}

export const MessageListModal: React.FC<MessageListModalProps> = ({
  isOpen,
  onClose,
  messages,
  onMessageClick,
}) => {
  // 최신순으로 정렬
  const sortedMessages = messages.sort((a, b) => {
    const dateA = new Date(a.createdAt || 0).getTime();
    const dateB = new Date(b.createdAt || 0).getTime();
    return dateB - dateA; // 최신순
  });


  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md md:max-w-2xl mx-auto max-h-[80vh] bg-gradient-to-br from-cyan-50 to-blue-50 border-2 md:border-4 border-cyan-200 rounded-2xl md:rounded-3xl">
        <DialogHeader className="pb-4">
          <DialogTitle className="flex items-center gap-2 text-cyan-800 font-bold text-lg md:text-xl">
            <MessageSquare className="w-5 h-5 md:w-6 md:h-6" />
            받은 메시지 목록
            <Badge variant="secondary" className="bg-cyan-100 text-cyan-700">
              {messages.length}개
            </Badge>
          </DialogTitle>
        </DialogHeader>

        {messages.length === 0 ? (
          <div className="text-center py-12">
            <div className="w-16 h-16 bg-cyan-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <MessageSquare className="w-8 h-8 text-cyan-400" />
            </div>
            <p className="text-gray-600 mb-2">아직 받은 메시지가 없어요</p>
            <p className="text-sm text-gray-500 flex items-center justify-center gap-2">
              친구들에게 롤링페이퍼를 공유해보세요! <Share2 className="w-4 h-4 text-blue-500" />
            </p>
          </div>
        ) : (
          <div className="max-h-96 md:max-h-[60vh] overflow-y-auto pr-4">
            <div className="space-y-3">
              {sortedMessages.map((message, index) => {
                const decoInfo = getDecoInfo(message.decoType);
                const isAnonymous =
                  message.anonymity && message.anonymity !== "";

                return (
                  <div
                    key={index}
                    onClick={() => {
                      onMessageClick(message);
                      onClose();
                    }}
                    className="group relative bg-white/80 backdrop-blur-sm border border-cyan-200 rounded-xl p-4 hover:shadow-lg hover:scale-[1.02] transition-all duration-300 cursor-pointer"
                  >
                    {/* 메시지 헤더 */}
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-3">
                        {/* 데코 아이콘 */}
                        <DecoIcon 
                          decoType={message.decoType}
                          size="lg"
                          showBackground={true}
                          className="shadow-md"
                        />

                        {/* 작성자 정보 */}
                        <div>
                          <p className="font-medium text-gray-800 text-sm md:text-base">
                            {isAnonymous ? message.anonymity : "익명"}
                          </p>
                          <div className="flex items-center gap-2 text-xs text-gray-500">
                            <Calendar className="w-3 h-3" />
                            {message.createdAt && formatRelativeDate(message.createdAt)}
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* 메시지 내용 */}
                    <div className="relative">
                      <p className="text-gray-700 text-sm md:text-base line-clamp-3 leading-relaxed">
                        {message.content}
                      </p>

                      {/* 호버 효과 */}
                      <div className="absolute inset-0 bg-gradient-to-r from-cyan-100/0 via-cyan-100/30 to-cyan-100/0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 rounded-lg pointer-events-none"></div>
                    </div>

                    {/* 클릭 힌트 */}
                    <div className="mt-3 flex items-center justify-end">
                      <div className="flex items-center gap-1 text-xs text-cyan-500 opacity-0 group-hover:opacity-100 transition-opacity">
                        <Sparkles className="w-3 h-3" />
                        클릭하면 위치로 이동
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 하단 버튼 */}
        <div className="pt-4 border-t border-cyan-200">
          <Button
            onClick={onClose}
            variant="outline"
            className="w-full bg-white hover:bg-cyan-50 border-cyan-300"
          >
            닫기
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

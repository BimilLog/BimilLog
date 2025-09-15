"use client";

import React, { memo, useMemo } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components";
import { Button } from "@/components";
import { Badge, TimeBadge } from "@/components";
import { Card, CardContent } from "@/components";
import { MessageSquare, Sparkles, Share2 } from "lucide-react";
import type { RollingPaperMessage } from "@/types/domains/paper";
import { DecoIcon } from "@/components";

interface MessageListModalProps {
  isOpen: boolean;
  onClose: () => void;
  messages: RollingPaperMessage[];
  onMessageClick: (message: RollingPaperMessage) => void;
}

export const MessageListModal: React.FC<MessageListModalProps> = memo(({
  isOpen,
  onClose,
  messages,
  onMessageClick,
}) => {
  // 최신순으로 정렬 (메모화)
  const sortedMessages = useMemo(() => {
    return [...messages].sort((a, b) => {
      const dateA = new Date(a.createdAt || 0).getTime();
      const dateB = new Date(b.createdAt || 0).getTime();
      return dateB - dateA; // 최신순
    });
  }, [messages]);


  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent size="xl">
        <DialogHeader>
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
            <p className="text-brand-muted mb-2">아직 받은 메시지가 없어요</p>
            <p className="text-sm text-brand-secondary flex items-center justify-center gap-2">
              친구들에게 롤링페이퍼를 공유해보세요! <Share2 className="w-4 h-4 text-blue-500" />
            </p>
          </div>
        ) : (
          <div className="max-h-96 md:max-h-[60vh] overflow-y-auto pr-4">
            <div className="space-y-3">
              {sortedMessages.map((message, index) => {
                const isAnonymous =
                  message.anonymity && message.anonymity !== "";

                return (
                  <Card
                    key={index}
                    variant="elevated"
                    interactive={true}
                    onClick={() => {
                      onMessageClick(message);
                      onClose();
                    }}
                    className="border border-cyan-200 rounded-xl hover:scale-[1.02]"
                  >
                    <CardContent className="p-4">
                    {/* 메시지 헤더 */}
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-3">
                        {/* 데코 아이콘 */}
                        <DecoIcon 
                          decoType={message.decoType}
                          size="lg"
                          showBackground={true}
                          className="shadow-brand-md"
                        />

                        {/* 작성자 정보 */}
                        <div>
                          <p className="font-medium text-brand-primary text-sm md:text-base">
                            {isAnonymous ? message.anonymity : "익명"}
                          </p>
                          {message.createdAt && (
                            <TimeBadge dateString={message.createdAt} size="xs" />
                          )}
                        </div>
                      </div>
                    </div>

                    {/* 메시지 내용 */}
                    <div className="relative">
                      <p className="text-brand-primary text-sm md:text-base line-clamp-3 leading-relaxed">
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
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          </div>
        )}

        <DialogFooter>
          <Button
            onClick={onClose}
            variant="outline"
            className="w-full"
          >
            닫기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
});

MessageListModal.displayName = "MessageListModal";

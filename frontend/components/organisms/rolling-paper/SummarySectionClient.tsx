"use client";

import React from "react";
import { Button } from "@/components";
import { Badge } from "@/components";
import { Share2, Lock } from "lucide-react";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import { DecoIcon } from "@/components";

interface SummarySectionClientProps {
  onShare?: () => void;
  message?: RollingPaperMessage | VisitMessage;
  decoInfo?: { name: string; color: string } | null;
  isOwner?: boolean;
  isRollingPaperMessage?: boolean;
  onMessageClick?: (message: RollingPaperMessage | VisitMessage) => void;
}

export const SummarySectionClient: React.FC<SummarySectionClientProps> = ({
  onShare,
  message,
  decoInfo,
  isOwner,
  isRollingPaperMessage,
  onMessageClick,
}) => {
  // 공유 버튼만 있는 경우 (빈 메시지 상태)
  if (onShare && !message) {
    return (
      <Button
        variant="outline"
        size="sm"
        className="mt-4 bg-cyan-50 border-cyan-300 text-cyan-700 hover:bg-cyan-100"
        onClick={onShare}
      >
        <Share2 className="w-4 h-4 mr-2" />
        지금 공유하기
      </Button>
    );
  }

  // 메시지 아이템 (클릭 가능)
  if (message && decoInfo !== undefined) {
    return (
      <div
        onClick={() => onMessageClick && onMessageClick(message)}
        className={`flex items-start space-x-3 p-3 md:p-4 rounded-xl md:rounded-2xl bg-gradient-to-r from-cyan-50 to-blue-50 border border-cyan-200 md:border-2 transform hover:scale-105 transition-transform ${
          onMessageClick ? "cursor-pointer" : ""
        }`}
      >
        <div
          className={`w-10 h-10 md:w-12 md:h-12 rounded-full bg-gradient-to-r ${typeof decoInfo?.color === 'string' ? decoInfo.color : ''} flex items-center justify-center shadow-brand-lg border-2 border-white flex-shrink-0`}
        >
          <DecoIcon decoType={message.decoType} size="md" showBackground={false} />
        </div>
        <div className="flex-1 min-w-0">
          {isRollingPaperMessage && isOwner ? (
            <p className="text-brand-primary text-sm md:text-base font-medium leading-relaxed">
              {(message as RollingPaperMessage).content}
            </p>
          ) : (
            <p className="text-brand-muted text-sm md:text-base font-medium leading-relaxed italic flex items-center space-x-2">
              <Lock className="w-4 h-4" />
              <span>메시지 내용은 작성자만 볼 수 있습니다</span>
            </p>
          )}
          <div className="flex items-center space-x-2 mt-2">
            {isRollingPaperMessage && isOwner && (
              <Badge
                variant="outline"
                className="text-xs bg-white border-cyan-300"
              >
                {(message as RollingPaperMessage).anonymity}
              </Badge>
            )}
            <span className="text-xs text-brand-secondary font-medium">
              {typeof decoInfo?.name === 'string' ? decoInfo.name : '기본'}
            </span>
          </div>
        </div>
      </div>
    );
  }

  return null;
};
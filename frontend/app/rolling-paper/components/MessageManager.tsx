"use client";

import React, { useState, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Send, Snowflake, Waves, IceCream2, Lock, Trash2 } from "lucide-react";
import { getDecoInfo, decoTypeMap, paperCommand } from "@/lib/api";
import type { DecoType, RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import { DecoIcon } from "@/components";
import { ErrorHandler } from "@/lib/error-handler";

interface MessageFormProps {
  nickname?: string;
  position?: { x: number; y: number };
  onSubmit: (data: {
    content: string;
    anonymousNickname: string;
    decoType: string;
  }) => void;
  onSuccess?: (message: string) => void;
  onError?: (message: string) => void;
  onCancel?: () => void;
}

interface MessageViewProps {
  message: RollingPaperMessage | VisitMessage;
  isOwner: boolean;
  onDelete?: () => void;
  onDeleteSuccess?: (message: string) => void;
  onDeleteError?: (message: string) => void;
}

interface MessageManagerProps {
  // Form mode props
  isFormMode?: boolean;
  formProps?: MessageFormProps;
  
  // View mode props  
  isViewMode?: boolean;
  viewProps?: MessageViewProps;

  // Dialog props
  isOpen: boolean;
  onClose: () => void;
  title?: string;
}

const MessageForm: React.FC<MessageFormProps> = React.memo(({
  onSubmit,
  onSuccess,
  onError,
  onCancel,
}) => {
  const [content, setContent] = useState("");
  const [anonymousNickname, setAnonymousNickname] = useState("");
  const [decoType, setDecoType] = useState("POTATO");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const decoOptions = Object.entries(decoTypeMap).map(([key, info]) => ({
    value: key,
    label: info.name,
    info,
  }));

  const handleSubmit = useCallback(async () => {
    if (!content.trim() || !anonymousNickname.trim()) {
      onError?.("모든 필드를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    try {
      onSubmit({
        content: content.trim(),
        anonymousNickname: anonymousNickname.trim(),
        decoType,
      });
      setContent("");
      setAnonymousNickname("");
      onSuccess?.("메시지가 성공적으로 추가되었습니다!");
    } catch (error) {
      console.error("Failed to add message:", error);
      onError?.("메시지 추가에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  }, [content, anonymousNickname, decoType, onSubmit, onSuccess, onError]);

  const selectedDecoInfo = getDecoInfo(decoType);

  return (
    <div
      className="space-y-6 p-6 bg-gradient-to-br from-cyan-50 to-blue-50 rounded-2xl border-2 border-cyan-200"
      style={{
        backgroundImage: `
          radial-gradient(circle at 15px 15px, rgba(91,192,222,0.2) 1px, transparent 1px),
          radial-gradient(circle at 45px 45px, rgba(135,206,235,0.1) 1px, transparent 1px)
        `,
        backgroundSize: "30px 30px, 90px 90px",
      }}
    >
      {/* 미리보기 카드 */}
      <div
        className={`p-4 rounded-xl bg-gradient-to-br ${selectedDecoInfo.color} border-2 border-white shadow-lg relative overflow-hidden`}
        style={{
          backgroundImage: `
            radial-gradient(circle at 8px 8px, rgba(255,255,255,0.3) 1px, transparent 1px),
            radial-gradient(circle at 24px 24px, rgba(255,255,255,0.2) 1px, transparent 1px)
          `,
          backgroundSize: "16px 16px, 48px 48px",
        }}
      >
        <div className="flex items-center space-x-2 mb-2">
          <DecoIcon decoType={decoType as DecoType} size="lg" showBackground={false} />
          <span className="text-sm font-semibold text-gray-800">
            {selectedDecoInfo.name}
          </span>
        </div>
        <p className="text-gray-800 text-sm font-medium">
          {content || "여기에 메시지가 표시됩니다..."}
        </p>
        <div className="absolute top-1 right-1 w-2 h-2 bg-yellow-300 rounded-full animate-ping"></div>
      </div>

      {/* 폼 입력 */}
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            닉네임
          </label>
          <Input
            placeholder="익명으로 표시될 닉네임을 입력하세요"
            value={anonymousNickname}
            onChange={(e) => setAnonymousNickname(e.target.value)}
            className="bg-white/80"
            maxLength={10}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            디자인 선택
          </label>
          <Select value={decoType} onValueChange={setDecoType}>
            <SelectTrigger className="bg-white/80">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {decoOptions.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  <div className="flex items-center space-x-2">
                    <DecoIcon 
                      decoType={option.value as DecoType} 
                      size="sm" 
                      showBackground={false} 
                    />
                    <span>{option.label}</span>
                  </div>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            메시지
          </label>
          <Textarea
            placeholder="따뜻한 메시지를 남겨주세요..."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className="bg-white/80 min-h-[100px]"
            maxLength={500}
          />
          <div className="text-xs text-gray-500 mt-1 text-right">
            {content.length}/500
          </div>
        </div>
      </div>

      {/* 액션 버튼 */}
      <div className="flex space-x-3 pt-2">
        <Button
          onClick={handleSubmit}
          disabled={isSubmitting || !content.trim() || !anonymousNickname.trim()}
          className="flex-1 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-600 hover:to-blue-700"
        >
          {isSubmitting ? (
            <div className="flex items-center space-x-2">
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              <span>전송 중...</span>
            </div>
          ) : (
            <>
              <Send className="w-4 h-4 mr-2" />
              메시지 전송
            </>
          )}
        </Button>
        {onCancel && (
          <Button variant="outline" onClick={onCancel} className="bg-white/80">
            취소
          </Button>
        )}
      </div>
    </div>
  );
});

const MessageView: React.FC<MessageViewProps> = React.memo(({
  message,
  isOwner,
  onDelete,
  onDeleteSuccess,
  onDeleteError,
}) => {
  const decoInfo = getDecoInfo(message.decoType);

  // RollingPaperMessage 타입 가드
  const isRollingPaperMessage = (
    msg: RollingPaperMessage | VisitMessage
  ): msg is RollingPaperMessage => {
    return "content" in msg && "anonymity" in msg;
  };

  const handleDelete = useCallback(async () => {
    if (!isRollingPaperMessage(message)) return;

    if (!window.confirm("정말로 이 메시지를 삭제하시겠습니까?")) {
      return;
    }

    try {
      const response = await paperCommand.deleteMessage(message.id);
      if (response.success) {
        onDelete?.();
        onDeleteSuccess?.("메시지가 성공적으로 삭제되었습니다.");
      } else {
        onDeleteError?.("메시지 삭제에 실패했습니다.");
      }
    } catch (error) {
      console.error("Failed to delete message:", error);
      const appError = ErrorHandler.mapApiError(error);
      onDeleteError?.(appError.userMessage);
    }
  }, [message, onDelete, onDeleteSuccess, onDeleteError]);

  return (
    <div className="space-y-4">
      <div
        className={`p-4 rounded-xl bg-gradient-to-br ${decoInfo.color} border-2 border-white shadow-lg relative overflow-hidden`}
        style={{
          backgroundImage: `
            radial-gradient(circle at 8px 8px, rgba(255,255,255,0.3) 1px, transparent 1px),
            radial-gradient(circle at 24px 24px, rgba(255,255,255,0.2) 1px, transparent 1px)
          `,
          backgroundSize: "16px 16px, 48px 48px",
        }}
      >
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center space-x-2">
            <DecoIcon decoType={message.decoType} size="lg" showBackground={false} />
            <span className="text-sm font-semibold text-gray-800">
              {decoInfo.name}
            </span>
          </div>
          <div className="text-xs text-gray-600">
            위치: {message.x}, {message.y}
          </div>
        </div>

        {isRollingPaperMessage(message) && isOwner ? (
          <>
            <p className="text-gray-800 text-sm font-medium leading-relaxed mb-3">
              {message.content}
            </p>
            <div className="flex items-center justify-between">
              <Badge variant="outline" className="bg-white/80 border-gray-300">
                {message.anonymity}
              </Badge>
              <div className="text-xs text-gray-600">
                {new Date(message.createdAt).toLocaleDateString()}
              </div>
            </div>
          </>
        ) : (
          <div className="text-center py-4">
            <Lock className="w-8 h-8 mx-auto text-gray-400 mb-2" />
            <p className="text-gray-600 text-sm">
              메시지 내용은 작성자만 볼 수 있습니다
            </p>
          </div>
        )}

        <div className="absolute top-1 right-1 w-2 h-2 bg-yellow-300 rounded-full animate-ping"></div>
      </div>

      {isRollingPaperMessage(message) && isOwner && (
        <div className="flex justify-end">
          <Button
            variant="destructive"
            size="sm"
            onClick={handleDelete}
            className="text-xs"
          >
            <Trash2 className="w-3 h-3 mr-1" />
            삭제
          </Button>
        </div>
      )}
    </div>
  );
});

export const MessageManager: React.FC<MessageManagerProps> = ({
  isFormMode = false,
  formProps,
  isViewMode = false,
  viewProps,
  isOpen,
  onClose,
  title,
}) => {
  const getDialogTitle = () => {
    if (title) return title;
    if (isFormMode) return "메시지 작성";
    if (isViewMode) return "메시지 보기";
    return "";
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md mx-4 bg-gradient-to-br from-blue-50 to-indigo-50 border-2 border-blue-200 rounded-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-center text-blue-800 font-bold">
            {getDialogTitle()}
          </DialogTitle>
        </DialogHeader>

        {isFormMode && formProps && (
          <MessageForm {...formProps} onCancel={onClose} />
        )}

        {isViewMode && viewProps && (
          <MessageView {...viewProps} />
        )}
      </DialogContent>
    </Dialog>
  );
};
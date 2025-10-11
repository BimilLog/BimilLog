"use client";

import { useState } from "react";
import { Button } from "@/components";
import { Badge } from "@/components";
import { ConfirmModal } from "@/components";
import { Spinner } from "@/components";
import { Lock, Trash2 } from "lucide-react";
import { getDecoInfo } from "@/lib/api";
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";
import { DecoIcon } from "@/components";
import { ErrorHandler } from "@/lib/api/helpers";
import { useDeleteRollingPaperMessage } from "@/hooks/api/useRollingPaperMutations";

interface MessageViewProps {
  message: RollingPaperMessage | VisitMessage;
  isOwner: boolean;
  onDelete?: () => void;
  onDeleteSuccess?: (message: string) => void;
  onDeleteError?: (message: string) => void;
}

export const MessageView: React.FC<MessageViewProps> = ({
  message,
  isOwner,
  onDelete,
  onDeleteSuccess,
  onDeleteError,
}) => {
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const deleteMutation = useDeleteRollingPaperMessage();
  const decoInfo = getDecoInfo(message.decoType);

  // RollingPaperMessage 타입 가드: content와 anonymity 필드 존재로 구분
  // VisitMessage는 작성자만 볼 수 있는 메시지이므로 구조가 다름
  const isRollingPaperMessage = (
    msg: RollingPaperMessage | VisitMessage
  ): msg is RollingPaperMessage => {
    return "content" in msg && "anonymity" in msg;
  };

  // 메시지 삭제 처리: RollingPaperMessage만 삭제 가능, 확인창 후 API 호출
  const handleDeleteClick = () => {
    if (!isRollingPaperMessage(message)) return;
    setShowDeleteConfirm(true);
  };

  const handleDeleteConfirm = () => {
    if (!isRollingPaperMessage(message)) return;

    deleteMutation.mutate(message.id, {
      onSuccess: () => {
        setShowDeleteConfirm(false);
        onDelete?.(); // 상위 컴포넌트에 삭제 완료 알림 (목록 새로고침 등)
        onDeleteSuccess?.("메시지가 성공적으로 삭제되었습니다.");
      },
      onError: (error) => {
        setShowDeleteConfirm(false);
        const appError = ErrorHandler.mapApiError(error);
        onDeleteError?.(appError.message);
      }
    });
  };

  return (
    <>
      <div
        className={`p-4 sm:p-6 bg-gradient-to-br ${decoInfo.color} rounded-2xl border-2 border-white shadow-brand-lg relative overflow-hidden`}
        style={{
          backgroundImage: `
            radial-gradient(circle at 8px 8px, rgba(255,255,255,0.3) 1px, transparent 1px),
            radial-gradient(circle at 24px 24px, rgba(255,255,255,0.2) 1px, transparent 1px)
          `,
          backgroundSize: "16px 16px, 48px 48px",
        }}
      >
        <div className="flex items-center gap-3 mb-4">
          <DecoIcon decoType={message.decoType} size="xl" showBackground={false} animate="bounce" />
          <div className="flex flex-col gap-1 flex-1 min-w-0">
            <Badge
              variant="secondary"
              className="bg-white/80 text-pink-800 border-pink-300 font-semibold text-sm w-fit"
            >
              {decoInfo.name}
            </Badge>
            {/* 익명 닉네임 배지: RollingPaperMessage만 표시, 비어있으면 '익명' 기본값 */}
            {isRollingPaperMessage(message) && (
              <Badge
                variant="outline"
                className="bg-white/60 text-brand-primary border-gray-300 text-xs w-fit"
              >
                {message.anonymity && message.anonymity !== ""
                  ? message.anonymity
                  : "익명"}
              </Badge>
            )}
          </div>
        </div>
        {/* 메시지 내용 표시: RollingPaperMessage는 내용 표시, VisitMessage는 잠금 메시지 */}
        {isRollingPaperMessage(message) ? (
          <p className="text-brand-primary leading-relaxed font-medium text-sm sm:text-base break-words">
            {message.content}
          </p>
        ) : (
          <p className="text-brand-muted leading-relaxed font-medium italic flex items-center gap-2 text-sm sm:text-base">
            <Lock className="w-4 h-4 flex-shrink-0" />
            <span className="break-words">메시지 내용은 작성자만 볼 수 있습니다</span>
          </p>
        )}

        {/* 반짝이는 효과 */}
        <div className="absolute top-2 right-2 w-3 h-3 bg-yellow-300 rounded-full animate-ping"></div>
        <div className="absolute bottom-3 left-3 w-2 h-2 bg-pink-300 rounded-full animate-pulse delay-500"></div>

        {/* 삭제 버튼: 롤링페이퍼 소유자에게만 표시 */}
        {isOwner && (
          <div className="flex justify-end pt-4 border-t border-white/30">
            <Button
              variant="destructive"
              size="sm"
              onClick={handleDeleteClick}
              disabled={deleteMutation.isPending}
              className="bg-red-500/80 hover:bg-red-600/80 text-white border-0 min-h-[44px] touch-manipulation text-sm disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {deleteMutation.isPending ? (
                <span className="flex items-center gap-2">
                  <Spinner size="sm" />
                  삭제 중...
                </span>
              ) : (
                "메시지 삭제"
              )}
            </Button>
          </div>
        )}
      </div>

      {/* 삭제 확인 모달 */}
      <ConfirmModal
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDeleteConfirm}
        title="메시지 삭제"
        message="정말로 이 메시지를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다."
        confirmText="삭제"
        cancelText="취소"
        confirmButtonVariant="destructive"
        icon={<Trash2 className="h-8 w-8 text-red-500" />}
        isLoading={deleteMutation.isPending}
      />
    </>
  );
};

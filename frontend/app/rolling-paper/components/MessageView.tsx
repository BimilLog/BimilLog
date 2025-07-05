import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Trash2, Clock } from "lucide-react";
import {
  getDecoInfo,
  type RollingPaperMessage,
  type VisitMessage,
  rollingPaperApi,
} from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { useToast } from "@/hooks/useToast";
import { ToastContainer } from "@/components/molecules/toast";

interface MessageViewProps {
  message: RollingPaperMessage | VisitMessage;
  isOwner: boolean;
}

export const MessageView: React.FC<MessageViewProps> = ({
  message,
  isOwner,
}) => {
  const { showSuccess, showError, toasts, removeToast } = useToast();
  const decoInfo = getDecoInfo(message.decoType);

  // RollingPaperMessage 타입 가드
  const isRollingPaperMessage = (
    msg: RollingPaperMessage | VisitMessage
  ): msg is RollingPaperMessage => {
    return "content" in msg && "anonymity" in msg;
  };

  const handleDelete = async () => {
    if (!isRollingPaperMessage(message)) return;

    if (!window.confirm("정말로 이 메시지를 삭제하시겠습니까?")) {
      return;
    }

    try {
      const response = await rollingPaperApi.deleteMessage({
        id: message.id,
        userId: message.userId,
        decoType: message.decoType,
        anonymity: message.anonymity,
        content: message.content,
        width: message.width,
        height: message.height,
      });
      if (response.success) {
        window.location.reload();
      } else {
        showError("메시지 삭제 실패", "메시지 삭제에 실패했습니다.");
      }
    } catch (error) {
      console.error("Failed to delete message:", error);
      showError("메시지 삭제 실패", "메시지 삭제 중 오류가 발생했습니다.");
    }
  };

  return (
    <>
      <div
        className={`p-6 bg-gradient-to-br ${decoInfo.color} rounded-2xl border-2 border-white shadow-lg relative overflow-hidden`}
        style={{
          backgroundImage: `
            radial-gradient(circle at 8px 8px, rgba(255,255,255,0.3) 1px, transparent 1px),
            radial-gradient(circle at 24px 24px, rgba(255,255,255,0.2) 1px, transparent 1px)
          `,
          backgroundSize: "16px 16px, 48px 48px",
        }}
      >
        <div className="flex items-center space-x-3 mb-4">
          <span className="text-4xl animate-bounce">{decoInfo.emoji}</span>
          <div className="flex flex-col space-y-1">
            <Badge
              variant="secondary"
              className="bg-white/80 text-pink-800 border-pink-300 font-semibold"
            >
              {decoInfo.name}
            </Badge>
            {isRollingPaperMessage(message) && (
              <Badge
                variant="outline"
                className="bg-white/60 text-gray-700 border-gray-300 text-xs"
              >
                {message.anonymity && message.anonymity !== ""
                  ? message.anonymity
                  : "익명"}
              </Badge>
            )}
          </div>
        </div>
        {isRollingPaperMessage(message) ? (
          <p className="text-gray-800 leading-relaxed font-medium">
            {message.content}
          </p>
        ) : (
          <p className="text-gray-600 leading-relaxed font-medium italic">
            메시지 내용은 작성자만 볼 수 있습니다 🔒
          </p>
        )}

        {/* 반짝이는 효과 */}
        <div className="absolute top-2 right-2 w-3 h-3 bg-yellow-300 rounded-full animate-ping"></div>
        <div className="absolute bottom-3 left-3 w-2 h-2 bg-pink-300 rounded-full animate-pulse delay-500"></div>

        {isOwner && (
          <div className="flex justify-end pt-4 border-t border-white/30">
            <Button
              variant="destructive"
              size="sm"
              onClick={handleDelete}
              className="bg-red-500/80 hover:bg-red-600/80 text-white border-0"
            >
              메시지 삭제
            </Button>
          </div>
        )}
      </div>

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
};

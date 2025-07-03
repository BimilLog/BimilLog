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

interface MessageViewProps {
  message: RollingPaperMessage | VisitMessage;
  isOwner: boolean;
}

export const MessageView: React.FC<MessageViewProps> = ({
  message,
  isOwner,
}) => {
  const decoInfo = getDecoInfo(message.decoType);

  // RollingPaperMessage 타입 가드
  const isRollingPaperMessage = (
    msg: RollingPaperMessage | VisitMessage
  ): msg is RollingPaperMessage => {
    return "content" in msg && "anonymity" in msg;
  };

  const handleDelete = async () => {
    if (!isOwner || !isRollingPaperMessage(message)) return;

    if (confirm("정말로 이 메시지를 삭제하시겠습니까?")) {
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
        }
      } catch (error) {
        console.error("Failed to delete message:", error);
        alert("메시지 삭제에 실패했습니다.");
      }
    }
  };

  return (
    <div className="space-y-4">
      <div
        className={`p-6 rounded-2xl bg-gradient-to-br ${decoInfo.color} border-4 border-white shadow-xl relative overflow-hidden`}
        style={{
          backgroundImage: `
            radial-gradient(circle at 10px 10px, rgba(255,255,255,0.3) 1px, transparent 1px),
            radial-gradient(circle at 30px 30px, rgba(255,255,255,0.2) 1px, transparent 1px)
          `,
          backgroundSize: "20px 20px, 60px 60px",
        }}
      >
        <div className="flex items-center space-x-3 mb-4">
          <span className="text-4xl animate-bounce">{decoInfo.emoji}</span>
          <Badge
            variant="secondary"
            className="bg-white/80 text-pink-800 border-pink-300 font-semibold"
          >
            {decoInfo.name}
          </Badge>
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
      </div>

      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          {isRollingPaperMessage(message) && (
            <Badge
              variant="outline"
              className="bg-pink-50 border-pink-300 text-pink-800 font-semibold"
            >
              {message.anonymity}
            </Badge>
          )}
          {isRollingPaperMessage(message) && message.createdAt && (
            <div className="flex items-center space-x-1 text-xs text-gray-500">
              <Clock className="w-3 h-3" />
              <span>{formatDate(message.createdAt)}</span>
            </div>
          )}
        </div>
        {isOwner && (
          <Button
            variant="outline"
            size="sm"
            className="text-red-600 border-red-200 hover:bg-red-50 rounded-full font-semibold"
            onClick={handleDelete}
          >
            <Trash2 className="w-4 h-4 mr-1" />
            삭제
          </Button>
        )}
      </div>
    </div>
  );
};

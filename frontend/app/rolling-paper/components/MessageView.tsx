import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Trash2 } from "lucide-react";
import {
  getDecoInfo,
  type RollingPaperMessage,
  rollingPaperApi,
} from "@/lib/api";

interface MessageViewProps {
  message: RollingPaperMessage;
  isOwner: boolean;
}

export const MessageView: React.FC<MessageViewProps> = ({
  message,
  isOwner,
}) => {
  const decoInfo = getDecoInfo(message.decoType);

  const handleDelete = async () => {
    if (!isOwner) return;

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
            className="bg-white/80 text-cyan-800 border-cyan-300 font-semibold"
          >
            {decoInfo.name}
          </Badge>
        </div>
        <p className="text-gray-800 leading-relaxed font-medium">
          {message.content}
        </p>

        {/* 시원한 반짝이는 효과 */}
        <div className="absolute top-2 right-2 w-3 h-3 bg-cyan-300 rounded-full animate-ping"></div>
        <div className="absolute bottom-3 left-3 w-2 h-2 bg-blue-300 rounded-full animate-pulse delay-500"></div>
      </div>

      <div className="flex items-center justify-between">
        <div>
          <Badge
            variant="outline"
            className="bg-cyan-50 border-cyan-300 text-cyan-800 font-semibold"
          >
            {message.anonymity}
          </Badge>
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

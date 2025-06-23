import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Trash2 } from "lucide-react";
import { getDecoInfo, type RollingPaperMessage } from "@/lib/api";

interface MessageViewProps {
  message: RollingPaperMessage;
  isOwner: boolean;
}

export const MessageView: React.FC<MessageViewProps> = ({
  message,
  isOwner,
}) => {
  const decoInfo = getDecoInfo(message.decoType);

  return (
    <div className="space-y-4">
      <div className={`p-4 rounded-lg bg-gradient-to-br ${decoInfo.color}`}>
        <p className="text-gray-800 leading-relaxed">{message.content}</p>
      </div>
      <div className="flex items-center justify-between">
        <div>
          <Badge variant="secondary">{message.anonymity}</Badge>
        </div>
        {isOwner && (
          <Button
            variant="outline"
            size="sm"
            className="text-red-600 border-red-200 hover:bg-red-50"
          >
            <Trash2 className="w-4 h-4" />
          </Button>
        )}
      </div>
    </div>
  );
};

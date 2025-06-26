import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Plus } from "lucide-react";
import { getDecoInfo, type RollingPaperMessage } from "@/lib/api";
import { MessageForm } from "./MessageForm";
import { MessageView } from "./MessageView";

interface RollingPaperGridProps {
  messages: { [key: number]: RollingPaperMessage };
  onMessageSubmit: (index: number, data: any) => void;
}

export const RollingPaperGrid: React.FC<RollingPaperGridProps> = ({
  messages,
  onMessageSubmit,
}) => {
  return (
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl mb-8">
      <CardHeader className="text-center">
        <CardTitle className="text-2xl bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
          롤링페이퍼
        </CardTitle>
        <p className="text-gray-600">메시지를 클릭하면 내용을 볼 수 있어요</p>
      </CardHeader>
      <CardContent className="p-6">
        <div className="grid grid-cols-7 gap-2 max-w-4xl mx-auto">
          {Array.from({ length: 98 }, (_, i) => {
            const hasMessage = messages[i];
            const decoInfo = hasMessage
              ? getDecoInfo(hasMessage.decoType)
              : null;
            return (
              <Dialog key={i}>
                <DialogTrigger asChild>
                  <div
                    className={`
                      aspect-square rounded-lg border-2 flex items-center justify-center cursor-pointer transition-all duration-200
                      ${
                        hasMessage
                          ? `bg-gradient-to-br ${decoInfo?.color} hover:scale-105 shadow-md`
                          : "border-dashed border-gray-200 hover:border-gray-300 hover:bg-gray-50"
                      }
                    `}
                  >
                    {hasMessage ? (
                      <span className="text-lg">{decoInfo?.emoji}</span>
                    ) : (
                      <Plus className="w-4 h-4 text-gray-400" />
                    )}
                  </div>
                </DialogTrigger>
                <DialogContent className="max-w-md">
                  <DialogHeader>
                    <DialogTitle>
                      {hasMessage ? "메시지 보기" : "새 메시지 작성"}
                    </DialogTitle>
                  </DialogHeader>
                  {hasMessage ? (
                    <MessageView message={hasMessage} isOwner={true} />
                  ) : (
                    <MessageForm
                      onSubmit={(data) => onMessageSubmit(i, data)}
                    />
                  )}
                </DialogContent>
              </Dialog>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
};

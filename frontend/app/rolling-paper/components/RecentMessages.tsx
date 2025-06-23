import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { MessageSquare } from "lucide-react";
import { getDecoInfo, type RollingPaperMessage } from "@/lib/api";

interface RecentMessagesProps {
  messages: { [key: number]: RollingPaperMessage };
}

export const RecentMessages: React.FC<RecentMessagesProps> = ({ messages }) => {
  return (
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <MessageSquare className="w-5 h-5 text-purple-600" />
          <span>최근 메시지</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {Object.entries(messages)
            .slice(-3)
            .map(([index, message]) => {
              const decoInfo = getDecoInfo(message.decoType);
              return (
                <div
                  key={index}
                  className="flex items-start space-x-3 p-3 rounded-lg bg-gray-50"
                >
                  <div
                    className={`w-8 h-8 rounded-full bg-gradient-to-r ${decoInfo.color} flex items-center justify-center`}
                  >
                    <span className="text-sm">{decoInfo.emoji}</span>
                  </div>
                  <div className="flex-1">
                    <p className="text-gray-800 text-sm">{message.content}</p>
                    <div className="flex items-center space-x-2 mt-1">
                      <Badge variant="outline" className="text-xs">
                        {message.anonymity}
                      </Badge>
                      <span className="text-xs text-gray-500">
                        {decoInfo.name}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
        </div>
      </CardContent>
    </Card>
  );
};

import { Button } from "@/components/ui/button";
import { MessageSquare, Heart, Share2, ArrowLeft } from "lucide-react";
import Link from "next/link";
import { User } from "@/lib/api";

interface RollingPaperHeaderProps {
  user: User;
  messageCount: number;
}

export const RollingPaperHeader: React.FC<RollingPaperHeaderProps> = ({
  user,
  messageCount,
}) => {
  return (
    <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b">
      <div className="container mx-auto px-4 py-4 flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link href="/">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="w-4 h-4 mr-2" />
              홈으로
            </Button>
          </Link>
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-lg flex items-center justify-center">
              <MessageSquare className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="font-bold text-gray-800">
                {user.userName}님의 롤링페이퍼
              </h1>
              <p className="text-xs text-gray-500">
                총 {messageCount}개의 메시지
              </p>
            </div>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline" size="sm" className="bg-white">
            <Share2 className="w-4 h-4 mr-2" />
            공유하기
          </Button>
          <Button variant="outline" size="sm" className="bg-white">
            <Heart className="w-4 h-4" />
          </Button>
        </div>
      </div>
    </header>
  );
};

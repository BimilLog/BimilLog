import React from "react";
import { Info, Waves, Mail } from "lucide-react";
import { cn } from "@/lib/utils";
import { Card, CardContent } from "@/components";

interface InfoCardProps {
  isOwner: boolean;
  nickname: string;
  className?: string;
}

export const InfoCard: React.FC<InfoCardProps> = ({
  isOwner,
  nickname,
  className = "",
}) => {
  return (
    <Card
      variant="elevated"
      className={cn(
        "text-brand-primary transition-all duration-300 rounded-2xl border-2 border-cyan-200",
        className
      )}
    >
      <CardContent className="p-4">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="text-center md:text-left">
          {isOwner ? (
            <div className="flex items-start space-x-3">
              <Info className="w-5 h-5 text-cyan-600 mt-0.5 flex-shrink-0" />
              <div>
                <p className="text-cyan-800 font-semibold text-sm md:text-base flex items-center space-x-2">
                  <span>내 롤링페이퍼 보기 모드</span>
                  <Waves className="w-4 h-4" />
                </p>
                <p className="text-cyan-700 text-xs md:text-sm mt-1">
                  이곳은 나에게 온 메시지들을 볼 수 있는 공간이에요.
                  <span className="block md:inline flex items-center space-x-1">
                    <span>친구들에게 공유하여 메시지를 받아보세요!</span>
                    <Mail className="w-3 h-3 md:w-4 md:h-4" />
                  </span>
                </p>
              </div>
            </div>
          ) : (
            <div className="flex items-start space-x-3">
              <Info className="w-5 h-5 text-cyan-600 mt-0.5 flex-shrink-0" />
              <div>
                <p className="text-cyan-800 font-semibold text-sm md:text-base flex items-center space-x-2">
                  <span>{nickname}님에게 메시지를 남겨보세요!</span>
                  <Waves className="w-4 h-4" />
                </p>
                <p className="text-cyan-700 text-xs md:text-sm mt-1">
                  빈 칸을 클릭하여 시원한 메시지를 남겨주세요.
                  <span className="block md:inline flex items-center space-x-1">
                    <span>익명으로 내용은 암호화되어 안전하게 전달됩니다!</span>
                    <Mail className="w-3 h-3 md:w-4 md:h-4" />
                  </span>
                </p>
              </div>
            </div>
          )}
        </div>
        </div>
      </CardContent>
    </Card>
  );
};

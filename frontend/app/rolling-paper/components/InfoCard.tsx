"use client";

import React from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Info } from "lucide-react";
import { cn } from "@/lib/utils";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Sparkles } from "lucide-react";

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
    <div
      className={cn(
        "text-gray-800 transition-all duration-300 p-4 bg-white/80 backdrop-blur-sm shadow-lg rounded-2xl border-2 border-cyan-200",
        className
      )}
    >
      <div className="flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="text-center md:text-left">
          {isOwner ? (
            <div className="flex items-start space-x-3">
              <Info className="w-5 h-5 text-cyan-600 mt-0.5 flex-shrink-0" />
              <div>
                <p className="text-cyan-800 font-semibold text-sm md:text-base">
                  내 롤링페이퍼 보기 모드 🌊
                </p>
                <p className="text-cyan-700 text-xs md:text-sm mt-1">
                  이곳은 나에게 온 메시지들을 볼 수 있는 공간이에요.
                  <span className="block md:inline">
                    {" "}
                    친구들에게 공유하여 메시지를 받아보세요! 💌
                  </span>
                </p>
              </div>
            </div>
          ) : (
            <div className="flex items-start space-x-3">
              <Info className="w-5 h-5 text-cyan-600 mt-0.5 flex-shrink-0" />
              <div>
                <p className="text-cyan-800 font-semibold text-sm md:text-base">
                  {nickname}님에게 메시지를 남겨보세요! 🌊
                </p>
                <p className="text-cyan-700 text-xs md:text-sm mt-1">
                  빈 칸을 클릭하여 시원한 메시지를 남겨주세요.
                  <span className="block md:inline">
                    {" "}
                    익명으로 내용은 암호화되어 안전하게 전달됩니다! 💌
                  </span>
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

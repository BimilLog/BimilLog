"use client";

import { Card } from "@/components";
import { Loader2 } from "lucide-react";

export function LoadingState() {
  return (
    <Card className="p-12">
      <div className="flex flex-col items-center justify-center gap-4">
        <Loader2 className="w-8 h-8 animate-spin text-purple-600" />
        <p className="text-gray-600">신고 목록을 불러오는 중...</p>
      </div>
    </Card>
  );
}
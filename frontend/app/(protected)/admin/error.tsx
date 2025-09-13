"use client";

import { useEffect } from "react";
import { Button } from "@/components";
import { AlertTriangle } from "lucide-react";

export default function AdminError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("Admin page error:", error);
  }, [error]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center p-4">
      <div className="bg-white/90 backdrop-blur-sm rounded-2xl shadow-xl p-8 max-w-md w-full text-center">
        <div className="p-4 rounded-full bg-red-100 inline-block mb-6">
          <AlertTriangle className="w-12 h-12 text-red-600" />
        </div>
        
        <h2 className="text-2xl font-bold text-gray-900 mb-3">
          오류가 발생했습니다
        </h2>
        
        <p className="text-gray-600 mb-6">
          관리자 페이지를 불러오는 중 문제가 발생했습니다.
          잠시 후 다시 시도해주세요.
        </p>
        
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Button
            onClick={reset}
            className="min-h-[48px] bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 text-white font-medium touch-manipulation"
          >
            다시 시도
          </Button>
          <Button
            onClick={() => window.location.href = "/"}
            variant="outline"
            className="min-h-[48px] border-gray-300 hover:bg-gray-50 touch-manipulation"
          >
            홈으로 이동
          </Button>
        </div>
      </div>
    </div>
  );
}
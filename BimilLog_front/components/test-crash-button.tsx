"use client";

import { useState } from "react";
import { Button } from "@/components";
import { Bug } from "lucide-react";
import { errorLogger } from "@/lib/error-logger";

/**
 * 개발 모드 전용 테스트 크래시 버튼
 * 에러 로깅 시스템을 테스트하기 위한 컴포넌트입니다.
 */
export function TestCrashButton() {
  const [isVisible, setIsVisible] = useState(false);

  // 개발 모드가 아니면 렌더링하지 않음
  if (process.env.NODE_ENV !== "development") {
    return null;
  }

  // Ctrl + Shift + D 키 조합으로 버튼 토글
  if (typeof window !== "undefined") {
    window.addEventListener("keydown", (e) => {
      if (e.ctrlKey && e.shiftKey && e.key === "D") {
        setIsVisible((prev) => !prev);
      }
    });
  }

  if (!isVisible) {
    return (
      <div className="fixed bottom-4 right-4 text-xs text-gray-500">
        Ctrl + Shift + D로 테스트 버튼 표시
      </div>
    );
  }

  const testScenarios = [
    {
      name: "동기 에러",
      action: () => {
        throw new Error("Test synchronous error");
      },
    },
    {
      name: "비동기 에러",
      action: async () => {
        throw new Error("Test async error");
      },
    },
    {
      name: "Promise Rejection",
      action: () => {
        Promise.reject(new Error("Test promise rejection"));
      },
    },
    {
      name: "수동 로깅",
      action: () => {
        errorLogger.logError("Test manual error logging", {
          testData: "test value",
        });
        alert("에러가 백엔드로 전송되었습니다. 로그를 확인하세요.");
      },
    },
  ];

  return (
    <div className="fixed bottom-4 right-4 bg-white border-2 border-red-500 rounded-lg p-4 shadow-lg z-50">
      <div className="flex items-center gap-2 mb-3">
        <Bug className="w-5 h-5 text-red-600" />
        <h3 className="font-bold text-sm">크래시 테스트</h3>
        <button
          onClick={() => setIsVisible(false)}
          className="ml-auto text-gray-500 hover:text-gray-700"
        >
          ✕
        </button>
      </div>
      <div className="space-y-2">
        {testScenarios.map((scenario) => (
          <Button
            key={scenario.name}
            onClick={scenario.action}
            variant="outline"
            size="sm"
            className="w-full text-xs"
          >
            {scenario.name}
          </Button>
        ))}
      </div>
      <p className="text-xs text-gray-500 mt-3">
        Ctrl + Shift + D로 닫기
      </p>
    </div>
  );
}

"use client";

import { useEffect } from "react";
import { errorLogger } from "@/lib/error-logger";

/**
 * 전역 에러 핸들러 초기화 컴포넌트
 * 앱 시작 시 window.onerror와 unhandledrejection 리스너를 설정합니다.
 */
export function ErrorInitializer() {
  useEffect(() => {
    errorLogger.setupGlobalErrorHandlers();
  }, []);

  return null;
}

"use client";

import { useState, useEffect, useCallback } from "react";
import { sseManager, type Notification } from "@/lib/api";
import { useAuth } from "@/hooks";
import { logger } from "@/lib/utils";
import { useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/tanstack-query/keys";

/**
 * SSE 연결 + 실시간 알림 동기화를 담당하는 Hook.
 */
export function useNotifications() {
  const { isAuthenticated, user } = useAuth();
  const queryClient = useQueryClient();
  const [isSSEConnected, setIsSSEConnected] = useState(false);
  const [connectionState, setConnectionState] = useState<string>("DISCONNECTED");

  const canConnectSSE = useCallback(() => {
    if (!isAuthenticated || !user) return false;
    if (!user.memberName || user.memberName.trim() === "") return false;
    return true;
  }, [isAuthenticated, user]);

  const updateConnectionState = useCallback(() => {
    const state = sseManager.getConnectionState();
    const connected = sseManager.isConnected();
    logger.log(`SSE 연결 상태: ${state}, 연결됨: ${connected}`);
    setConnectionState(state);
    setIsSSEConnected(connected);
  }, []);

  useEffect(() => {
    if (!canConnectSSE()) return;

    const statusListener = (status: "connecting" | "connected" | "disconnected" | "error" | "reconnecting") => {
      logger.log(`SSE 상태 변화: ${status}`);

      const normalizedState = status.toUpperCase();
      setConnectionState(normalizedState);
      setIsSSEConnected(status === "connected");

      switch (status) {
        case "connecting":
          logger.log("SSE 연결 시도 중...");
          break;
        case "connected":
          logger.log("SSE 연결 완료");
          if (!sseManager.hasShownConnectedToast()) {
            sseManager.markConnectedToastShown();
          }
          break;
        case "reconnecting":
          logger.log("SSE 재연결 시도 중...");
          break;
        case "error":
          logger.log("SSE 연결 실패 - 재시도 예정");
          break;
        case "disconnected":
          logger.log("SSE 연결 종료");
          break;
      }
    };

    sseManager.addStatusListener(statusListener);
    return () => {
      sseManager.removeStatusListener(statusListener);
    };
  }, [canConnectSSE]);

  useEffect(() => {
    if (canConnectSSE()) {
      logger.log(`사용자 인증 완료 (${user?.memberName}) - 알림 리스너 등록`);

      sseManager.removeEventListener("notification");
      sseManager.addEventListener("notification", (data: Notification) => {
        logger.log("새 알림 수신:", data);

        queryClient.invalidateQueries({ queryKey: queryKeys.notification.list(), exact: true });
        logger.log("알림 목록 자동 갱신 완료");

        if (typeof window !== "undefined" && "Notification" in window && Notification.permission === "granted") {
          new Notification(data.content, {
            body: data.url,
            icon: "/favicon.ico",
          });
        }
      });

      updateConnectionState();
      const timeoutId = setTimeout(updateConnectionState, 1000);
      const intervalId = setInterval(updateConnectionState, 30000);

      return () => {
        clearTimeout(timeoutId);
        clearInterval(intervalId);
        sseManager.removeEventListener("notification");
      };
    } else {
      sseManager.removeEventListener("notification");
      setIsSSEConnected(false);
      setConnectionState("DISCONNECTED");
    }
  }, [canConnectSSE, user, updateConnectionState]);

  return {
    isSSEConnected,
    connectionState,
    canConnectSSE,
  };
}

"use client";

import { useState, useEffect, useCallback } from "react";
import { sseManager } from "@/lib/api";
import type { Notification as NotificationData } from "@/types/domains/notification";
import { useAuth } from "@/hooks";
import { logger } from "@/lib/utils";
import { useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/tanstack-query/keys";
import { useToastStore } from "@/stores/toast.store";
import { useRouter } from "next/navigation";

/**
 * SSE 연결 + 실시간 알림 동기화를 담당하는 Hook.
 */
export function useNotifications() {
  const { isAuthenticated, user } = useAuth();
  const queryClient = useQueryClient();
  const router = useRouter();
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
      sseManager.addEventListener("notification", (data: NotificationData) => {
        logger.log("새 알림 수신:", data);

        queryClient.invalidateQueries({ queryKey: queryKeys.notification.list(), exact: true });
        logger.log("알림 목록 자동 갱신 완료");

        // INITIATE 는 사용자 노출 불필요 (연결 환영 — sseManager 내부에서 처리)
        if (data.notificationType === "INITIATE") return;

        const toastCopy = getNotificationToastCopy(data.notificationType);

        // F-1107: in-page 토스트 — OS 권한이 없거나 다른 탭에서도 인지 가능
        // 라운드 5 패턴 (showAdvancedToast + action) 재사용.
        const { showAdvancedToast } = useToastStore.getState();
        showAdvancedToast({
          type: "info",
          title: toastCopy.title,
          description: data.content,
          duration: 5000,
          action: data.url
            ? {
                label: "보러 가기",
                onClick: () => {
                  if (typeof window === "undefined") return;
                  const url = data.url;
                  const isExternal =
                    /^https?:\/\//i.test(url) && !url.startsWith(window.location.origin);
                  if (isExternal) {
                    window.location.assign(url);
                  } else {
                    router.push(url);
                  }
                },
              }
            : undefined,
        });

        // F-1104: window.Notification 으로 명시. body 는 url 대신 type별 카피.
        // 도메인 타입(`Notification`)과 OS API(`window.Notification`) 충돌 회피.
        if (
          typeof window !== "undefined" &&
          "Notification" in window &&
          window.Notification.permission === "granted"
        ) {
          try {
            const osNotification = new window.Notification(toastCopy.title, {
              body: data.content,
              icon: "/favicon.ico",
            });
            // F-1104: OS 토스트 클릭 → 창 포커스 + SPA 라우팅
            osNotification.onclick = () => {
              try {
                window.focus();
              } catch {
                /* noop */
              }
              if (data.url) {
                const url = data.url;
                const isExternal =
                  /^https?:\/\//i.test(url) && !url.startsWith(window.location.origin);
                if (isExternal) {
                  window.location.assign(url);
                } else {
                  router.push(url);
                }
              }
              osNotification.close();
            };
          } catch (err) {
            logger.log("OS 알림 생성 실패", err);
          }
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
  }, [canConnectSSE, user, updateConnectionState, queryClient, router]);

  return {
    isSSEConnected,
    connectionState,
    canConnectSSE,
  };
}

/** F-1107: 알림 type 별 in-page 토스트 카피 (종이/편지 메타포 일관) */
function getNotificationToastCopy(notificationType: NotificationData["notificationType"]): {
  title: string;
} {
  switch (notificationType) {
    case "PAPER":
      return { title: "새 편지가 도착했어요" };
    case "COMMENT":
      return { title: "내 글에 댓글이 달렸어요" };
    case "POST_FEATURED":
      return { title: "내 글이 인기글에 올랐어요" };
    case "FRIEND":
      return { title: "새 친구 요청이 도착했어요" };
    case "ADMIN":
      return { title: "관리자 안내가 도착했어요" };
    case "INITIATE":
    default:
      return { title: "새 알림이 도착했어요" };
  }
}

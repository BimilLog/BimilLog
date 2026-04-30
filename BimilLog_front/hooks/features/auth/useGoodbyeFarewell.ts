"use client";

import { useEffect, useRef } from "react";
import { useToastStore } from "@/stores/toast.store";

const GOODBYE_MARKER_KEY = "bimillog_pending_goodbye";
const GOODBYE_DEDUPE_KEY = "bimillog_goodbye_shown_at";
const GOODBYE_DEDUPE_WINDOW_MS = 60_000;

interface GoodbyePayload {
  reason?: "normal" | "timeout";
  ts: number;
}

/**
 * B-304: 로그아웃 완료 후 다음 페이지(주로 홈) 마운트 시 안내 토스트 발사.
 *
 * - `useWelcomeOnboarding` 과 동일한 마커-소비 패턴 (sessionStorage).
 * - 60초 dedupe 윈도우로 라우터 remount 방어.
 * - duration 2500ms (짧게), type `info` (success 가 아니라 정보 톤 — "로그아웃" 은 긍정 이벤트가 아님).
 */
export function useGoodbyeFarewell() {
  const showInfo = useToastStore((state) => state.showInfo);
  const triggeredRef = useRef(false);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (triggeredRef.current) return;

    let raw: string | null = null;
    try {
      raw = window.sessionStorage.getItem(GOODBYE_MARKER_KEY);
    } catch {
      return;
    }

    if (!raw) return;

    try {
      const parsed = JSON.parse(raw) as GoodbyePayload;

      // 60초 내 dedupe (라우터 mount/remount 방어)
      const lastShownRaw = window.sessionStorage.getItem(GOODBYE_DEDUPE_KEY);
      const lastShown = lastShownRaw ? Number.parseInt(lastShownRaw, 10) : 0;
      const now = Date.now();
      if (
        Number.isFinite(lastShown) &&
        lastShown > 0 &&
        now - lastShown < GOODBYE_DEDUPE_WINDOW_MS
      ) {
        // 직전에 이미 표시됨 → 마커만 정리
        window.sessionStorage.removeItem(GOODBYE_MARKER_KEY);
        return;
      }

      const timer = window.setTimeout(() => {
        if (parsed.reason === "timeout") {
          // /logout 페이지에서 이미 직접 토스트를 띄웠으므로 여기서는 발사하지 않음
          // (이중 발사 방지) — 단지 dedupe 마크만 찍어두기
        } else {
          showInfo(
            "안전하게 로그아웃했어요",
            "다음에 다시 만나요.",
            2500
          );
        }
        triggeredRef.current = true;

        try {
          window.sessionStorage.setItem(GOODBYE_DEDUPE_KEY, String(now));
          window.sessionStorage.removeItem(GOODBYE_MARKER_KEY);
        } catch {
          /* noop */
        }
      }, 200);

      return () => window.clearTimeout(timer);
    } catch {
      try {
        window.sessionStorage.removeItem(GOODBYE_MARKER_KEY);
      } catch {
        /* noop */
      }
    }
  }, [showInfo]);
}

"use client";

import { useEffect, useRef } from "react";
import { useToastStore } from "@/stores/toast.store";
import { useAuthStore } from "@/stores/auth.store";
import type { SocialProvider } from "@/lib/api";

const ONBOARDED_KEY = "bimillog_onboarded";
const PENDING_WELCOME_KEY = "bimillog_pending_welcome";
const WELCOME_DEDUPE_KEY = "bimillog_welcome_shown_at";
const WELCOME_DEDUPE_WINDOW_MS = 60_000;

/**
 * 콜백 처리 직후 ("막 로그인/가입 완료" 시점) 클라이언트에서
 * 신규 여부를 판별하기 위한 마커를 sessionStorage 에 심는다.
 *
 * - `localStorage[ONBOARDED_KEY]` 가 없으면 신규로 간주 (첫 인증).
 * - 콜백 → 홈 라우팅 사이에 토스트 표시 컴포넌트가 마운트되기 전 단계가 있어서
 *   sessionStorage 의 PENDING flag 로 인계한다.
 */
export function markPendingWelcome(provider: SocialProvider | null) {
  if (typeof window === "undefined") return;

  try {
    const onboarded = window.localStorage.getItem(ONBOARDED_KEY);
    const isNewcomer = !onboarded;

    window.sessionStorage.setItem(
      PENDING_WELCOME_KEY,
      JSON.stringify({
        provider,
        isNewcomer,
        ts: Date.now(),
      })
    );

    // 신규 마크는 즉시 찍어둠 (다음 진입부터는 기존 사용자)
    if (isNewcomer) {
      window.localStorage.setItem(ONBOARDED_KEY, String(Date.now()));
    }
  } catch {
    // storage 차단 환경 — 토스트 무시
  }
}

/**
 * 홈에 도착했을 때 pending welcome flag 를 소비해
 * 환영 토스트를 띄운다.
 *
 * - 신규 사용자: "환영" 톤 + 첫 액션 유도.
 * - 기존 사용자: "다시 만나서 반가워요" 짧은 톤.
 * - provider 별 카피 차별화 (KAKAO 는 친구 동선 한 마디 추가).
 *
 * 토스트는 이 훅이 마운트된 이후 한 tick 만에 처리되며,
 * 새로고침이나 다른 페이지 이동에서 중복 발사되지 않도록
 * 1분 안에 같은 마크가 또 들어와도 무시한다.
 */
export function useWelcomeOnboarding() {
  const showAdvancedToast = useToastStore((state) => state.showAdvancedToast);
  const triggeredRef = useRef(false);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (triggeredRef.current) return;

    let raw: string | null = null;
    try {
      raw = window.sessionStorage.getItem(PENDING_WELCOME_KEY);
    } catch {
      return;
    }

    if (!raw) return;

    try {
      const parsed = JSON.parse(raw) as {
        provider: SocialProvider | null;
        isNewcomer: boolean;
        ts: number;
      };

      // 1분 내 dedupe (라우터 mount/remount 방어)
      const lastShownRaw = window.sessionStorage.getItem(WELCOME_DEDUPE_KEY);
      const lastShown = lastShownRaw ? Number.parseInt(lastShownRaw, 10) : 0;
      const now = Date.now();
      if (
        Number.isFinite(lastShown) &&
        lastShown > 0 &&
        now - lastShown < WELCOME_DEDUPE_WINDOW_MS
      ) {
        // 직전에 이미 표시됨 → 마커만 정리
        window.sessionStorage.removeItem(PENDING_WELCOME_KEY);
        return;
      }

      // 마운트 후 짧은 tick 뒤에 인증 store 의 user 확인 (provider 카피용).
      // B-405/B-406: 250ms 는 콜백 → 홈 hydration 사이의 빈 시간을 키운다.
      // auth store 의 user 는 콜백 시점에 이미 sync 되어 있으므로 80ms 면 충분하며,
      // 너무 짧게(0/RAF) 두면 첫 페인트 직전에 발사되어 토스트가 빈 화면 위에 잠깐 떠 있는
      // 어색한 프레임이 생기므로 약간의 여유를 둔다.
      const timer = window.setTimeout(() => {
        const user = useAuthStore.getState().user;
        const memberName = user?.memberName?.trim() || user?.socialNickname?.trim() || "";

        const greeting = parsed.isNewcomer
          ? memberName
            ? `환영해요, ${memberName}님`
            : "환영해요"
          : memberName
            ? `다시 만나서 반가워요, ${memberName}님`
            : "다시 만나서 반가워요";

        const description = parsed.isNewcomer
          ? parsed.provider === "KAKAO"
            ? "첫 비밀편지함을 만들고 카카오 친구에게 공유해 보세요."
            : "첫 비밀편지함을 만들고 친구에게 링크로 공유해 보세요."
          : "오늘도 따뜻한 한 줄, 누군가에게 닿길 바라요.";

        showAdvancedToast({
          type: parsed.isNewcomer ? "feedback" : "success",
          title: greeting,
          description,
          duration: parsed.isNewcomer ? 6000 : 3500,
          action: parsed.isNewcomer
            ? {
                label: "내 페이퍼 열기",
                onClick: () => {
                  if (typeof window !== "undefined") {
                    window.location.assign("/rolling-paper");
                  }
                },
              }
            : undefined,
        });

        triggeredRef.current = true;

        try {
          window.sessionStorage.setItem(WELCOME_DEDUPE_KEY, String(now));
          window.sessionStorage.removeItem(PENDING_WELCOME_KEY);
        } catch {
          /* noop */
        }
      }, 80);

      return () => window.clearTimeout(timer);
    } catch {
      try {
        window.sessionStorage.removeItem(PENDING_WELCOME_KEY);
      } catch {
        /* noop */
      }
    }
  }, [showAdvancedToast]);
}

/**
 * 현재 사용자가 "신규 (첫 가입 직후)" 인지 클라이언트 휴리스틱으로 판별.
 *
 * 사용 시점 — 홈 진입 직후 신규 온보딩 카드 노출 결정.
 * 한 번 본 사용자는 `dismissNewcomerOnboarding()` 으로 마킹.
 */
const NEWCOMER_ONBOARDING_KEY = "bimillog_newcomer_onboarding_dismissed";
const NEWCOMER_GRACE_WINDOW_MS = 1000 * 60 * 60 * 24 * 7; // 7일

export function isFreshlyOnboarded(): boolean {
  if (typeof window === "undefined") return false;
  try {
    const onboardedAt = window.localStorage.getItem(ONBOARDED_KEY);
    if (!onboardedAt) return false;
    const onboardedTs = Number.parseInt(onboardedAt, 10);
    if (!Number.isFinite(onboardedTs)) return false;

    const dismissed = window.localStorage.getItem(NEWCOMER_ONBOARDING_KEY);
    if (dismissed) return false;

    return Date.now() - onboardedTs < NEWCOMER_GRACE_WINDOW_MS;
  } catch {
    return false;
  }
}

export function dismissNewcomerOnboarding() {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(NEWCOMER_ONBOARDING_KEY, String(Date.now()));
  } catch {
    /* noop */
  }
}

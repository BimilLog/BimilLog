"use client";

import type { SocialProvider } from "@/types/domains/auth";

/**
 * 4-5: last-used provider 힌트.
 *
 * `auth-storage` 의 persisted provider 는 로그아웃 시 cleanup 으로 null 이 된다.
 * → 별도 localStorage key (`bimillog_last_provider`) 로 단말 기억을 보존.
 *
 * - 단말 로컬에만 저장 (서버 전송 X). 닉네임/이메일은 저장하지 않음.
 * - SSR 안전: typeof window 체크.
 */
const LAST_PROVIDER_KEY = "bimillog_last_provider";

export function rememberLastUsedProvider(provider: SocialProvider | null) {
  if (typeof window === "undefined") return;
  if (!provider) return;
  try {
    window.localStorage.setItem(LAST_PROVIDER_KEY, provider);
  } catch {
    /* storage 차단 환경 — 무시 */
  }
}

export function readLastUsedProvider(): SocialProvider | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(LAST_PROVIDER_KEY);
    if (raw === "KAKAO" || raw === "NAVER" || raw === "GOOGLE") {
      return raw;
    }
    return null;
  } catch {
    return null;
  }
}

export function clearLastUsedProvider() {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.removeItem(LAST_PROVIDER_KEY);
  } catch {
    /* noop */
  }
}

"use client";

import { useEffect, useRef } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useToastStore } from "@/stores/toast.store";

/**
 * F-203 회복 흐름이 `/?recovered=1` 으로 라우팅한 직후 한 번만 안내 토스트를 띄우고
 * 쿼리 파라미터를 정리한다. (B-404)
 *
 * - 카피 톤은 차분한 info — 일반 환영 토스트와 구분.
 * - 마운트당 1회만 동작 (StrictMode/리마운트 대비 ref 가드).
 * - URL 정리: `router.replace(pathname)` — 다른 쿼리는 보존하지 않는다 (회복 직후 홈에서만 호출되는 것을 가정).
 */
export function useRecoveredHint() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const showInfo = useToastStore((state) => state.showInfo);
  const triggeredRef = useRef(false);

  useEffect(() => {
    if (triggeredRef.current) return;
    if (searchParams.get("recovered") !== "1") return;

    triggeredRef.current = true;

    showInfo(
      "이미 로그인되어 있어 그대로 이어갑니다.",
      "잠깐 멈춤 없이 계속 사용하실 수 있어요.",
      4000
    );

    // 쿼리 파라미터 정리 — URL 공유 시 노이즈 방지
    router.replace(pathname);
  }, [searchParams, router, pathname, showInfo]);
}

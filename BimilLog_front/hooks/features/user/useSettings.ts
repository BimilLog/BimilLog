"use client";

import { useUserSettings } from "./useUserSettings";

/**
 * Settings 페이지에서 사용하는 간소화된 설정 관리 훅
 * useUserSettings를 래핑하여 Settings 페이지와 호환되는 인터페이스 제공
 */
export function useSettings() {
  return useUserSettings();
}
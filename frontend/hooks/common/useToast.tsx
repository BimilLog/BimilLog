"use client";

import { useToastStore } from "@/stores/toast.store";

// Main hook - Zustand store를 직접 사용
export function useToast() {
  const store = useToastStore();

  return {
    toasts: store.toasts,
    addToast: store.addToast,
    removeToast: store.removeToast,
    clearAllToasts: store.clearAllToasts,
    showSuccess: store.showSuccess,
    showError: store.showError,
    showWarning: store.showWarning,
    showInfo: store.showInfo,
    showFeedback: store.showFeedback,
    showNeutral: store.showNeutral,
    showWithUndo: store.showWithUndo,
    showAdvancedToast: store.showAdvancedToast,
    // showToast 메서드 추가 (하위 호환성)
    showToast: store.showToast,
  };
}
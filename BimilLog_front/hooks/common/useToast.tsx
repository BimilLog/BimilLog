"use client";

import { useToastStore } from "@/stores/toast.store";
import { useShallow } from "zustand/shallow";

// Main hook - Zustand store를 selector로 사용하여 불필요한 리렌더링 방지
export function useToast() {
  return useToastStore(
    useShallow((state) => ({
      toasts: state.toasts,
      addToast: state.addToast,
      removeToast: state.removeToast,
      clearAllToasts: state.clearAllToasts,
      showSuccess: state.showSuccess,
      showError: state.showError,
      showWarning: state.showWarning,
      showInfo: state.showInfo,
      showFeedback: state.showFeedback,
      showNeutral: state.showNeutral,
      showWithUndo: state.showWithUndo,
      showAdvancedToast: state.showAdvancedToast,
      // showToast 메서드 추가 (하위 호환성)
      showToast: state.showToast,
    }))
  );
}

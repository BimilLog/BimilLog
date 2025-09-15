"use client";

import { type ReactNode } from "react";
import { useToastStore } from "@/stores/toast.store";
import { ToastContainer } from "@/components/molecules/feedback/toast";

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

// Legacy Context API for backward compatibility
export function ToastProvider({ children }: { children: ReactNode }) {
  const { toasts, removeToast } = useToastStore();

  return (
    <>
      {children}
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
}

// Legacy hook for backward compatibility
export function useToastContext() {
  const store = useToastStore();
  
  return {
    showSuccess: store.showSuccess,
    showError: store.showError,
    showWarning: store.showWarning,
    showInfo: store.showInfo,
  };
}
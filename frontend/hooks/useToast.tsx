"use client";

import { useState, useCallback } from "react";
import type { Toast, ToastType } from "@/components/molecules/toast";
import { ToastContainer } from "@/components/molecules/toast";

export function useToast() {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = useCallback(
    (
      type: ToastType,
      title: string,
      description?: string,
      duration: number = 5000
    ) => {
      const id = Math.random().toString(36).substring(2, 9);
      const newToast: Toast = {
        id,
        type,
        title,
        description,
        duration,
      };

      setToasts((prev) => [...prev, newToast]);
      return id;
    },
    []
  );

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  const clearAllToasts = useCallback(() => {
    setToasts([]);
  }, []);

  // 편의 메서드들
  const showSuccess = useCallback(
    (title: string, description?: string, duration?: number) => {
      return addToast("success", title, description, duration);
    },
    [addToast]
  );

  const showError = useCallback(
    (title: string, description?: string, duration?: number) => {
      return addToast("error", title, description, duration);
    },
    [addToast]
  );

  const showWarning = useCallback(
    (title: string, description?: string, duration?: number) => {
      return addToast("warning", title, description, duration);
    },
    [addToast]
  );

  const showInfo = useCallback(
    (title: string, description?: string, duration?: number) => {
      return addToast("info", title, description, duration);
    },
    [addToast]
  );

  return {
    toasts,
    addToast,
    removeToast,
    clearAllToasts,
    showSuccess,
    showError,
    showWarning,
    showInfo,
  };
}

// 전역 토스트 관리를 위한 Context (선택사항)
import React, { createContext, useContext, type ReactNode } from "react";

interface ToastContextType {
  showSuccess: (
    title: string,
    description?: string,
    duration?: number
  ) => string;
  showError: (title: string, description?: string, duration?: number) => string;
  showWarning: (
    title: string,
    description?: string,
    duration?: number
  ) => string;
  showInfo: (title: string, description?: string, duration?: number) => string;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export function ToastProvider({ children }: { children: ReactNode }) {
  const { showSuccess, showError, showWarning, showInfo, toasts, removeToast } =
    useToast();

  const value: ToastContextType = {
    showSuccess,
    showError,
    showWarning,
    showInfo,
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </ToastContext.Provider>
  );
}

export function useToastContext() {
  const context = useContext(ToastContext);
  if (context === undefined) {
    throw new Error("useToastContext must be used within a ToastProvider");
  }
  return context;
}

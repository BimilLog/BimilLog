"use client";

import { useCallback, useEffect, useState } from "react";
import { Toast as FlowbiteToast } from "flowbite-react";
import {
  X,
  AlertCircle,
  CheckCircle,
  Info,
  AlertTriangle,
  MessageSquare,
  Undo,
  Send
} from "lucide-react";
import { cn } from "@/lib/utils";

export type ToastType = "success" | "error" | "warning" | "info" | "feedback" | "neutral";

export interface Toast {
  id: string;
  type: ToastType;
  title: string;
  description?: string;
  duration?: number;
  action?: {
    label: string;
    onClick: () => void;
  };
  undoAction?: () => void;
}

interface ToastProps {
  toast: Toast;
  onRemove: (id: string) => void;
}

const iconMap = {
  success: CheckCircle,
  error: AlertCircle,
  warning: AlertTriangle,
  info: Info,
  feedback: MessageSquare,
  neutral: Send,
};

// Flowbite Toast 색상 테마
const colorTheme = {
  success: "green",
  error: "red",
  warning: "orange",
  info: "blue",
  feedback: "purple",
  neutral: "gray",
} as const;

export function ToastComponent({ toast, onRemove }: ToastProps) {
  const [isVisible, setIsVisible] = useState(false);
  const [isLeaving, setIsLeaving] = useState(false);

  const IconComponent = iconMap[toast.type];
  const theme = colorTheme[toast.type];

  const handleRemove = useCallback(() => {
    setIsLeaving(true);
    setTimeout(() => {
      onRemove(toast.id);
    }, 300);
  }, [onRemove, toast.id]);

  useEffect(() => {
    // 마운트 시 애니메이션
    const timer = setTimeout(() => setIsVisible(true), 10);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    // 자동 제거 (타입별 차등 시간 적용)
    if (toast.duration && toast.duration > 0) {
      const timer = setTimeout(() => {
        handleRemove();
      }, toast.duration);
      return () => clearTimeout(timer);
    }
  }, [toast.duration, toast.id, handleRemove]);

  // Feedback 타입 토스트 (Interactive)
  if (toast.type === 'feedback' && (toast.action || toast.undoAction)) {
    return (
      <div
        className={cn(
          "transition-all duration-300 ease-in-out",
          isVisible && !isLeaving
            ? "translate-x-0 opacity-100"
            : "translate-x-full opacity-0"
        )}
      >
        <FlowbiteToast className="shadow-lg">
          <div className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-purple-100 text-purple-500">
            <MessageSquare className="h-5 w-5 stroke-purple-500 fill-purple-100" />
          </div>
          <div className="ml-3 text-sm font-normal">
            <div className="mb-2 text-sm font-semibold text-gray-900">{toast.title}</div>
            {toast.description && (
              <div className="mb-3 text-sm font-normal text-gray-500">{toast.description}</div>
            )}
            <div className="flex gap-2">
              {toast.action && (
                <button
                  onClick={() => {
                    toast.action?.onClick();
                    handleRemove();
                  }}
                  className="inline-flex h-8 items-center justify-center rounded-lg bg-purple-600 px-3 text-center text-xs font-medium text-white hover:bg-purple-700 focus:outline-none focus:ring-4 focus:ring-purple-300"
                >
                  {toast.action.label}
                </button>
              )}
              {toast.undoAction && (
                <button
                  onClick={() => {
                    toast.undoAction?.();
                    handleRemove();
                  }}
                  className="inline-flex h-8 items-center gap-1 justify-center rounded-lg border border-gray-300 bg-white px-3 text-center text-xs font-medium text-gray-900 hover:bg-gray-100 focus:outline-none focus:ring-4 focus:ring-gray-200"
                >
                  <Undo className="h-3 w-3 stroke-slate-600" />
                  실행 취소
                </button>
              )}
            </div>
          </div>
          <button
            type="button"
            className="ml-auto -mx-1.5 -my-1.5 bg-white text-gray-400 hover:text-gray-900 rounded-lg focus:ring-2 focus:ring-gray-300 p-1.5 hover:bg-gray-100 inline-flex h-8 w-8"
            onClick={handleRemove}
            aria-label="Close"
          >
            <X className="w-5 h-5 stroke-slate-600" />
          </button>
        </FlowbiteToast>
      </div>
    );
  }

  // 기본 토스트 (Flowbite Toast 사용)
  return (
    <div
      className={cn(
        "transition-all duration-300 ease-in-out",
        isVisible && !isLeaving
          ? "translate-x-0 opacity-100"
          : "translate-x-full opacity-0"
      )}
    >
      <FlowbiteToast className="shadow-lg">
        <div className={cn(
          "inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg",
          theme === "green" && "bg-green-100 text-green-500",
          theme === "red" && "bg-red-100 text-red-500",
          theme === "orange" && "bg-orange-100 text-orange-500",
          theme === "blue" && "bg-blue-100 text-blue-500",
          theme === "purple" && "bg-purple-100 text-purple-500",
          theme === "gray" && "bg-gray-100 text-gray-500"
        )}>
          <IconComponent className={cn(
            "h-5 w-5",
            theme === "green" && "stroke-green-600 fill-green-100",
            theme === "red" && "stroke-red-600 fill-red-100",
            theme === "orange" && "stroke-amber-600 fill-amber-100",
            theme === "blue" && "stroke-blue-600 fill-blue-100",
            theme === "purple" && "stroke-purple-500 fill-purple-100",
            theme === "gray" && "stroke-slate-600 fill-slate-100"
          )} />
        </div>
        <div className="ml-3 text-sm font-normal">
          <span className="mb-1 text-sm font-semibold text-gray-900">{toast.title}</span>
          {toast.description && (
            <div className="mt-1 text-sm font-normal text-gray-500">{toast.description}</div>
          )}
        </div>
        <button
          type="button"
          className="ml-auto -mx-1.5 -my-1.5 bg-white text-gray-400 hover:text-gray-900 rounded-lg focus:ring-2 focus:ring-gray-300 p-1.5 hover:bg-gray-100 inline-flex h-8 w-8"
          onClick={handleRemove}
          aria-label="Close"
        >
          <X className="w-5 h-5 stroke-slate-600" />
        </button>
      </FlowbiteToast>
    </div>
  );
}

interface ToastContainerProps {
  toasts: Toast[];
  onRemove: (id: string) => void;
}

export function ToastContainer({ toasts, onRemove }: ToastContainerProps) {
  if (toasts.length === 0) return null;

  return (
    <div className="fixed top-20 right-4 z-50 flex flex-col gap-3 pointer-events-none max-w-md">
      {toasts.map((toast) => (
        <div key={toast.id} className="pointer-events-auto">
          <ToastComponent toast={toast} onRemove={onRemove} />
        </div>
      ))}
    </div>
  );
}

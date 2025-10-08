"use client";

import { useEffect } from 'react';
import { Toast, ToastToggle } from 'flowbite-react';
import { HiCheck, HiX, HiExclamation, HiInformationCircle } from 'react-icons/hi';
import { useToastStore } from '@/stores/toast.store';
import type { ToastType } from '@/components/molecules/feedback/toast';

export function GlobalToast() {
  const { toasts, removeToast } = useToastStore();

  // 자동 제거 타이머 설정
  useEffect(() => {
    const timers = toasts.map((toast) => {
      if (toast.duration && toast.duration !== 0) {
        return setTimeout(() => {
          removeToast(toast.id);
        }, toast.duration);
      }
      return null;
    });

    return () => {
      timers.forEach((timer) => {
        if (timer) clearTimeout(timer);
      });
    };
  }, [toasts, removeToast]);

  const getIcon = (type: ToastType) => {
    switch (type) {
      case 'success':
        return <HiCheck className="h-5 w-5" />;
      case 'error':
        return <HiX className="h-5 w-5" />;
      case 'warning':
        return <HiExclamation className="h-5 w-5" />;
      case 'info':
      default:
        return <HiInformationCircle className="h-5 w-5" />;
    }
  };

  const getColorClass = (type: ToastType) => {
    switch (type) {
      case 'success':
        return 'bg-green-100 text-green-500 dark:bg-green-800 dark:text-green-200';
      case 'error':
        return 'bg-red-100 text-red-500 dark:bg-red-800 dark:text-red-200';
      case 'warning':
        return 'bg-orange-100 text-orange-500 dark:bg-orange-700 dark:text-orange-200';
      case 'info':
      case 'feedback':
      case 'neutral':
      default:
        return 'bg-blue-100 text-blue-500 dark:bg-blue-800 dark:text-blue-200';
    }
  };

  if (toasts.length === 0) return null;

  return (
    <div className="fixed top-20 right-4 z-50 space-y-2 max-w-md">
      {toasts.map((toast) => (
        <Toast key={toast.id} className="shadow-lg">
          <div className={`inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${getColorClass(toast.type)}`}>
            {getIcon(toast.type)}
          </div>
          <div className="ml-3 text-sm font-normal">
            <div className="font-semibold mb-1">{toast.title}</div>
            {toast.description && <div>{toast.description}</div>}
          </div>
          <ToastToggle onDismiss={() => removeToast(toast.id)} />
        </Toast>
      ))}
    </div>
  );
}
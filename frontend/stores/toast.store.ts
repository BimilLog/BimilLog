import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import type { Toast, ToastType } from '@/components/molecules/feedback/toast';

interface ToastState {
  toasts: Toast[];

  // Actions
  addToast: (type: ToastType, title: string, description?: string, duration?: number) => string;
  removeToast: (id: string) => void;
  clearAllToasts: () => void;

  // 편의 메서드들 (각 타입별로 쉽게 호출할 수 있도록)
  showSuccess: (title: string, description?: string, duration?: number) => string;
  showError: (title: string, description?: string, duration?: number) => string;
  showWarning: (title: string, description?: string, duration?: number) => string;
  showInfo: (title: string, description?: string, duration?: number) => string;
  showToast: (options: { type: ToastType; message: string; description?: string; duration?: number }) => string;
}

export const useToastStore = create<ToastState>()(
  devtools(
    (set, get) => ({
      toasts: [],
      
      addToast: (type, title, description, duration = 5000) => {
        // Math.random으로 고유한 ID 생성 (7자리 랜덤 문자열)
        const id = Math.random().toString(36).substring(2, 9);
        const newToast: Toast = {
          id,
          type,
          title,
          description,
          duration,
        };

        // 기존 토스트 배열에 새 토스트 추가 (기존 것들은 유지)
        set((state) => ({
          toasts: [...state.toasts, newToast],
        }));

        return id;
      },
      
      removeToast: (id) => {
        set((state) => ({
          toasts: state.toasts.filter((toast) => toast.id !== id),
        }));
      },
      
      clearAllToasts: () => {
        set({ toasts: [] });
      },
      
      // 각 타입별 편의 메서드들 - addToast를 내부적으로 호출
      showSuccess: (title, description, duration) => {
        return get().addToast('success', title, description, duration);
      },

      showError: (title, description, duration) => {
        return get().addToast('error', title, description, duration);
      },

      showWarning: (title, description, duration) => {
        return get().addToast('warning', title, description, duration);
      },

      showInfo: (title, description, duration) => {
        return get().addToast('info', title, description, duration);
      },

      // 객체 형태로 옵션을 받는 통합 메서드 (TanStack Query 에러 핸들링 등에서 사용)
      showToast: (options) => {
        return get().addToast(options.type, options.message, options.description, options.duration);
      },
    }),
    {
      name: 'toast-store',
    }
  )
);
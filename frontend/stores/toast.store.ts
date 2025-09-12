import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import type { Toast, ToastType } from '@/components/molecules/feedback/toast';

interface ToastState {
  toasts: Toast[];
  
  // Actions
  addToast: (type: ToastType, title: string, description?: string, duration?: number) => string;
  removeToast: (id: string) => void;
  clearAllToasts: () => void;
  
  // Convenience methods
  showSuccess: (title: string, description?: string, duration?: number) => string;
  showError: (title: string, description?: string, duration?: number) => string;
  showWarning: (title: string, description?: string, duration?: number) => string;
  showInfo: (title: string, description?: string, duration?: number) => string;
}

export const useToastStore = create<ToastState>()(
  devtools(
    (set, get) => ({
      toasts: [],
      
      addToast: (type, title, description, duration = 5000) => {
        const id = Math.random().toString(36).substring(2, 9);
        const newToast: Toast = {
          id,
          type,
          title,
          description,
          duration,
        };
        
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
    }),
    {
      name: 'toast-store',
    }
  )
);
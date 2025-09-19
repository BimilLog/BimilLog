import { create } from 'zustand';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title?: string;
  message: string;
  duration?: number; // milliseconds, 0 means no auto-dismiss
}

interface ToastState {
  toasts: ToastMessage[];
  addToast: (toast: Omit<ToastMessage, 'id'>) => void;
  removeToast: (id: string) => void;
  clearToasts: () => void;
}

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],
  addToast: (toast) => {
    const id = `toast-${Date.now()}-${Math.random()}`;
    set((state) => ({
      toasts: [...state.toasts, { ...toast, id }],
    }));
  },
  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((toast) => toast.id !== id),
    }));
  },
  clearToasts: () => {
    set({ toasts: [] });
  },
}));

// Helper hook for easier toast usage
export const useGlobalToast = () => {
  const { addToast } = useToastStore();

  return {
    success: (message: string, title?: string) => {
      addToast({ type: 'success', message, title });
    },
    error: (message: string, title?: string) => {
      addToast({ type: 'error', message, title });
    },
    warning: (message: string, title?: string) => {
      addToast({ type: 'warning', message, title });
    },
    info: (message: string, title?: string) => {
      addToast({ type: 'info', message, title });
    },
  };
};
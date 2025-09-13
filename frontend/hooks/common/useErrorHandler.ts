"use client";

import { useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useToast } from '@/hooks';
import { useAuthStore } from '@/stores/auth.store';
import { useToastStore } from '@/stores/toast.store';
import { ErrorHandler, type AppError, type ErrorType } from '@/lib/api/helpers';
import type { DomainError, ErrorDomain } from '@/lib/errors/domainErrors';

interface UseErrorHandlerOptions {
  showToast?: boolean;
  onError?: (error: AppError) => void;
  customMessages?: Partial<Record<ErrorType, string>>;
  domain?: string;
  enableAutoRecovery?: boolean;
}

export function useErrorHandler(options: UseErrorHandlerOptions = {}) {
  const {
    showToast = true,
    onError,
    customMessages = {},
    domain,
    enableAutoRecovery = true
  } = options;

  const { showError, showWarning } = useToast();
  const router = useRouter();
  const authStore = useAuthStore();
  const toastStore = useToastStore();

  const handleError = useCallback(async (
    error: unknown,
    context?: string,
    operation?: string,
    data?: unknown
  ) => {
    const appError = ErrorHandler.mapApiError(error);

    // 커스텀 메시지 적용
    if (customMessages[appError.type]) {
      appError.userMessage = customMessages[appError.type];
    }

    // 컨텍스트 추가
    if (context) {
      appError.message = `[${context}] ${appError.message}`;
    }

    // 자동 복구 시도 (향후 구현 예정)
    if (enableAutoRecovery && appError.type === 'AUTH_ERROR') {
      // 인증 에러시 자동 로그아웃 처리
      if (appError.message.includes('만료') || appError.message.includes('다른 기기')) {
        authStore.logout();
        router.push('/login');
      }
    }

    // 토스트 표시 (자동 복구 실패시 또는 비활성화시)
    if (showToast) {
      const { title, message } = ErrorHandler.formatErrorForToast(appError);

      // 에러 타입에 따라 다른 처리
      switch (appError.type) {
        case 'AUTH_ERROR':
        case 'PERMISSION_DENIED':
          showWarning(title, message);
          break;
        default:
          showError(title, message);
      }
    }

    // 커스텀 에러 핸들러 호출
    onError?.(appError);

    return appError;
  }, [showToast, showError, showWarning, onError, customMessages, domain, enableAutoRecovery, router, authStore, toastStore]);

  const handleRollingPaperError = useCallback(async (error: unknown, data?: unknown) => {
    // 롤링페이퍼 도메인으로 처리
    return await handleError(error, 'rolling-paper', 'write-message', data);
  }, [handleError]);

  const isNetworkError = useCallback((error: unknown): boolean => {
    const appError = ErrorHandler.mapApiError(error);
    return appError.type === 'NETWORK_ERROR';
  }, []);

  const isAuthError = useCallback((error: unknown): boolean => {
    const appError = ErrorHandler.mapApiError(error);
    return appError.type === 'AUTH_ERROR';
  }, []);

  const isValidationError = useCallback((error: unknown): boolean => {
    const appError = ErrorHandler.mapApiError(error);
    return appError.type === 'VALIDATION_ERROR';
  }, []);

  const isNotFoundError = useCallback((error: unknown): boolean => {
    const appError = ErrorHandler.mapApiError(error);
    return appError.type === 'NOT_FOUND';
  }, []);

  const isServerError = useCallback((error: unknown): boolean => {
    const appError = ErrorHandler.mapApiError(error);
    return appError.type === 'SERVER_ERROR';
  }, []);

  const isPermissionError = useCallback((error: unknown): boolean => {
    const appError = ErrorHandler.mapApiError(error);
    return appError.type === 'PERMISSION_DENIED';
  }, []);

  return {
    handleError,
    handleRollingPaperError,
    isNetworkError,
    isAuthError,
    isValidationError,
    isNotFoundError,
    isServerError,
    isPermissionError,
    ErrorHandler // 원본 클래스도 노출
  };
}
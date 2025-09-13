"use client";

import { useCallback } from 'react';
import { useToast } from '@/hooks';
import { ErrorHandler, type AppError, type ErrorType } from '@/lib/api/helpers';

interface UseErrorHandlerOptions {
  showToast?: boolean;
  onError?: (error: AppError) => void;
  customMessages?: Partial<Record<ErrorType, string>>;
}

export function useErrorHandler(options: UseErrorHandlerOptions = {}) {
  const { showToast = true, onError, customMessages = {} } = options;
  const { showError, showWarning } = useToast();

  const handleError = useCallback((error: unknown, context?: string) => {
    const appError = ErrorHandler.mapApiError(error);
    
    // 커스텀 메시지 적용
    if (customMessages[appError.type]) {
      appError.userMessage = customMessages[appError.type];
    }
    
    // 컨텍스트 추가
    if (context) {
      appError.message = `[${context}] ${appError.message}`;
    }
    
    // 토스트 표시
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
  }, [showToast, showError, showWarning, onError, customMessages]);

  const handleRollingPaperError = useCallback((error: unknown) => {
    const appError = ErrorHandler.handleRollingPaperError(error);
    
    if (showToast) {
      const { title, message } = ErrorHandler.formatErrorForToast(appError);
      showError(title, message);
    }
    
    onError?.(appError);
    
    return appError;
  }, [showToast, showError, onError]);

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
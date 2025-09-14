/**
 * 도메인별 특화 에러 핸들러 hooks
 */

import { useCallback } from 'react';
import { useErrorHandler } from './useErrorHandler';

/**
 * Auth 도메인 특화 에러 핸들러
 */
export function useAuthErrorHandler() {
  return useErrorHandler({
    domain: 'auth',
    enableAutoRecovery: true,
    customMessages: {
      'AUTH_ERROR': '로그인이 필요하거나 세션이 만료되었습니다.',
      'PERMISSION_DENIED': '접근 권한이 없습니다.',
    }
  });
}

/**
 * User 도메인 특화 에러 핸들러
 */
export function useUserErrorHandler() {
  const errorHandler = useErrorHandler({
    domain: 'user',
    enableAutoRecovery: true
  });

  const handleProfileError = useCallback(async (error: unknown, profileData?: unknown) => {
    return await errorHandler.handleError(error, 'user-profile', 'update-profile', profileData);
  }, [errorHandler]);

  const handleNicknameError = useCallback(async (error: unknown, nickname?: string) => {
    return await errorHandler.handleError(error, 'user-nickname', 'check-nickname', { nickname });
  }, [errorHandler]);

  const handleWithdrawalError = useCallback(async (error: unknown) => {
    return await errorHandler.handleError(error, 'user-withdrawal', 'withdraw', undefined);
  }, [errorHandler]);

  return {
    ...errorHandler,
    handleProfileError,
    handleNicknameError,
    handleWithdrawalError
  };
}

/**
 * Post 도메인 특화 에러 핸들러
 */
export function usePostErrorHandler() {
  const errorHandler = useErrorHandler({
    domain: 'post',
    enableAutoRecovery: true
  });

  const handleCreateError = useCallback(async (error: unknown, postData?: { title?: string; content?: string }) => {
    return await errorHandler.handleError(error, 'post-create', 'create-post', postData);
  }, [errorHandler]);

  const handleUpdateError = useCallback(async (error: unknown, postData?: { title?: string; content?: string }) => {
    return await errorHandler.handleError(error, 'post-update', 'update-post', postData);
  }, [errorHandler]);

  const handleDeleteError = useCallback(async (error: unknown, postId?: string) => {
    return await errorHandler.handleError(error, 'post-delete', 'delete-post', { postId });
  }, [errorHandler]);

  const handleSearchError = useCallback(async (error: unknown, query?: string) => {
    return await errorHandler.handleError(error, 'post-search', 'search-posts', { query });
  }, [errorHandler]);

  const handleLikeError = useCallback(async (error: unknown, postId?: string) => {
    return await errorHandler.handleError(error, 'post-like', 'toggle-like', { postId });
  }, [errorHandler]);

  return {
    ...errorHandler,
    handleCreateError,
    handleUpdateError,
    handleDeleteError,
    handleSearchError,
    handleLikeError
  };
}

/**
 * Comment 도메인 특화 에러 핸들러
 */
export function useCommentErrorHandler() {
  const errorHandler = useErrorHandler({
    domain: 'comment',
    enableAutoRecovery: true
  });

  const handleCreateError = useCallback(async (error: unknown, commentData?: { content?: string; postId?: string; parentId?: string }) => {
    return await errorHandler.handleError(error, 'comment-create', 'create-comment', commentData);
  }, [errorHandler]);

  const handleUpdateError = useCallback(async (error: unknown, commentData?: { content?: string; commentId?: string }) => {
    return await errorHandler.handleError(error, 'comment-update', 'update-comment', commentData);
  }, [errorHandler]);

  const handleDeleteError = useCallback(async (error: unknown, commentId?: string) => {
    return await errorHandler.handleError(error, 'comment-delete', 'delete-comment', { commentId });
  }, [errorHandler]);

  const handleLikeError = useCallback(async (error: unknown, commentId?: string) => {
    return await errorHandler.handleError(error, 'comment-like', 'toggle-like', { commentId });
  }, [errorHandler]);

  return {
    ...errorHandler,
    handleCreateError,
    handleUpdateError,
    handleDeleteError,
    handleLikeError
  };
}

/**
 * Paper (Rolling Paper) 도메인 특화 에러 핸들러
 */
export function usePaperErrorHandler() {
  const errorHandler = useErrorHandler({
    domain: 'paper',
    enableAutoRecovery: true
  });

  const handleMessageError = useCallback(async (error: unknown, messageData?: {
    content?: string;
    userName?: string;
    decoType?: string;
    position?: { x: number; y: number };
  }) => {
    return await errorHandler.handleError(error, 'paper-message', 'create-message', messageData);
  }, [errorHandler]);

  const handleDeleteError = useCallback(async (error: unknown, messageId?: string) => {
    return await errorHandler.handleError(error, 'paper-message', 'delete-message', { messageId });
  }, [errorHandler]);

  const handleShareError = useCallback(async (error: unknown, userName?: string) => {
    return await errorHandler.handleError(error, 'paper-share', 'share-paper', { userName });
  }, [errorHandler]);

  const handleLoadError = useCallback(async (error: unknown, userName?: string) => {
    return await errorHandler.handleError(error, 'paper-load', 'load-paper', { userName });
  }, [errorHandler]);

  return {
    ...errorHandler,
    handleMessageError,
    handleDeleteError,
    handleShareError,
    handleLoadError
  };
}

/**
 * Notification 도메인 특화 에러 핸들러
 */
export function useNotificationErrorHandler() {
  const errorHandler = useErrorHandler({
    domain: 'notification',
    enableAutoRecovery: true,
    showToast: false // 알림 에러는 조용히 처리
  });

  const handleLoadError = useCallback(async (error: unknown) => {
    return await errorHandler.handleError(error, 'notification-load', 'load-notifications', undefined);
  }, [errorHandler]);

  const handleMarkReadError = useCallback(async (error: unknown, notificationId?: string) => {
    return await errorHandler.handleError(error, 'notification-read', 'mark-read', { notificationId });
  }, [errorHandler]);

  const handleSSEError = useCallback(async (error: unknown) => {
    return await errorHandler.handleError(error, 'notification-sse', 'connect-sse', undefined);
  }, [errorHandler]);

  const handlePushError = useCallback(async (error: unknown, token?: string) => {
    return await errorHandler.handleError(error, 'notification-push', 'subscribe-push', { token });
  }, [errorHandler]);

  return {
    ...errorHandler,
    handleLoadError,
    handleMarkReadError,
    handleSSEError,
    handlePushError
  };
}

/**
 * Admin 도메인 특화 에러 핸들러
 */
export function useAdminErrorHandler() {
  const errorHandler = useErrorHandler({
    domain: 'admin',
    enableAutoRecovery: true
  });

  const handleReportError = useCallback(async (error: unknown, reportData?: { reportId?: string; action?: string }) => {
    return await errorHandler.handleError(error, 'admin-report', 'process-report', reportData);
  }, [errorHandler]);

  const handleBanError = useCallback(async (error: unknown, userData?: { userId?: string; reason?: string }) => {
    return await errorHandler.handleError(error, 'admin-ban', 'ban-user', userData);
  }, [errorHandler]);

  const handleStatsError = useCallback(async (error: unknown) => {
    return await errorHandler.handleError(error, 'admin-stats', 'load-stats', undefined);
  }, [errorHandler]);

  return {
    ...errorHandler,
    handleReportError,
    handleBanError,
    handleStatsError
  };
}

/**
 * 도메인 에러 핸들러 팩토리
 */
export function useDomainErrorHandler(domain: string) {
  switch (domain) {
    case 'auth':
      return useAuthErrorHandler();
    case 'user':
      return useUserErrorHandler();
    case 'post':
      return usePostErrorHandler();
    case 'comment':
      return useCommentErrorHandler();
    case 'paper':
      return usePaperErrorHandler();
    case 'notification':
      return useNotificationErrorHandler();
    case 'admin':
      return useAdminErrorHandler();
    default:
      return useErrorHandler({ domain });
  }
}
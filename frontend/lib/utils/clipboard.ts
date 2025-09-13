/**
 * 클립보드 유틸리티 함수들
 * 프로젝트 전반에 걸쳐 일관된 클립보드 접근을 위한 통합 유틸리티
 */
import { logger } from './index';

/**
 * 텍스트를 클립보드에 복사
 * @param text 복사할 텍스트
 * @param options 복사 옵션
 * @returns 복사 성공 여부
 */
export async function copyToClipboard(
  text: string,
  options: {
    showToast?: boolean;
    toastTitle?: string;
    toastDescription?: string;
    fallbackAlert?: boolean;
    showSuccess?: (title: string, message: string) => void;
    showError?: (title: string, message: string) => void;
  } = {}
): Promise<boolean> {
  const {
    showToast = true,
    toastTitle = "링크 복사 완료",
    toastDescription = "링크가 클립보드에 복사되었습니다!",
    fallbackAlert = false,
    showSuccess,
    showError
  } = options;

  // SSR 환경에서는 실행하지 않음
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    logger.warn('copyToClipboard: SSR 환경에서는 실행할 수 없습니다.');
    return false;
  }

  try {
    // 최신 Clipboard API 사용 (HTTPS 환경에서만 동작)
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      
      if (showToast && showSuccess) {
        showSuccess(toastTitle, toastDescription);
      }
      
      return true;
    }

    // Fallback: execCommand 사용 (deprecated but still works)
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'absolute';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    
    const successful = document.execCommand('copy');
    document.body.removeChild(textArea);

    if (successful) {
      if (showToast && showSuccess) {
        showSuccess(toastTitle, toastDescription);
      } else if (fallbackAlert) {
        alert(toastDescription);
      }
      return true;
    }

    throw new Error('execCommand failed');
  } catch (error) {
    logger.error('클립보드 복사 실패:', error);

    // 최후 수단: alert로 사용자에게 수동 복사 요청
    if (fallbackAlert) {
      alert(`다음 링크를 복사해주세요:\n${text}`);
    } else if (showToast && showError) {
      showError(
        "복사 실패", 
        "클립보드 복사에 실패했습니다. 브라우저 설정을 확인해주세요."
      );
    }

    return false;
  }
}

/**
 * 롤링페이퍼 공유 링크 복사
 */
export async function copyRollingPaperLink(
  nickname: string,
  messageCount: number = 0
): Promise<boolean> {
  const baseUrl = typeof window !== 'undefined' 
    ? `${window.location.protocol}//${window.location.host}`
    : 'https://grow-farm.com';
  
  const shareUrl = `${baseUrl}/rolling-paper/${encodeURIComponent(nickname)}`;
  
  return copyToClipboard(shareUrl, {
    toastTitle: "링크 복사 완료",
    toastDescription: messageCount > 0 
      ? `${nickname}님의 롤링페이퍼 링크가 복사되었습니다! (${messageCount}개의 메시지)`
      : `${nickname}님의 롤링페이퍼 링크가 복사되었습니다!`
  });
}

/**
 * 게시글 공유 링크 복사
 */
export async function copyPostLink(
  postId: number,
  title?: string
): Promise<boolean> {
  const baseUrl = typeof window !== 'undefined' 
    ? `${window.location.protocol}//${window.location.host}`
    : 'https://grow-farm.com';
  
  const shareUrl = `${baseUrl}/board/post/${postId}`;
  
  return copyToClipboard(shareUrl, {
    toastTitle: "링크 복사 완료",
    toastDescription: title 
      ? `"${title}" 게시글 링크가 복사되었습니다!`
      : "게시글 링크가 복사되었습니다!"
  });
}

/**
 * 현재 페이지 링크 복사
 */
export async function copyCurrentPageLink(
  customTitle?: string
): Promise<boolean> {
  if (typeof window === 'undefined') {
    return false;
  }

  return copyToClipboard(window.location.href, {
    toastTitle: customTitle || "링크 복사 완료",
    toastDescription: "현재 페이지 링크가 복사되었습니다!"
  });
}

/**
 * 클립보드 읽기 (권한 필요)
 */
export async function readFromClipboard(): Promise<string | null> {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return null;
  }

  try {
    if (navigator.clipboard && window.isSecureContext) {
      const text = await navigator.clipboard.readText();
      return text;
    }
    return null;
  } catch (error) {
    logger.error('클립보드 읽기 실패:', error);
    return null;
  }
}

/**
 * 클립보드 사용 가능 여부 확인
 */
export function isClipboardSupported(): boolean {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return false;
  }

  return !!(
    (navigator.clipboard && window.isSecureContext) ||
    document.queryCommandSupported?.('copy')
  );
}
import DOMPurify from 'dompurify';

/**
 * HTML 콘텐츠를 안전하게 정화합니다.
 * XSS 공격을 방지하기 위해 위험한 HTML, JavaScript 코드를 제거합니다.
 */
export const sanitizeHtml = (content: string): string => {
  if (typeof window === 'undefined') {
    return content; // 서버 사이드에서는 실행하지 않음
  }
  return DOMPurify.sanitize(content, {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['script', 'style', 'iframe', 'frame', 'object', 'embed'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover']
  });
};

/**
 * URL을 안전하게 정화합니다.
 */
export const sanitizeUrl = (url: string): string => {
  if (!url) return '';
  
  // javascript: URL 스키마 차단
  if (url.toLowerCase().startsWith('javascript:')) {
    return '';
  }
  
  // data: URL 스키마 차단 (이미지 제외)
  if (url.toLowerCase().startsWith('data:') && 
      !url.toLowerCase().startsWith('data:image/')) {
    return '';
  }
  
  return url;
}; 
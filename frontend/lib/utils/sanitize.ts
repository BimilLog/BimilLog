import DOMPurify from 'dompurify';

interface SanitizeOptions {
  allowedTags?: string[];
  forbiddenTags?: string[];
}

interface DOMPurifyConfig {
  USE_PROFILES?: { html: boolean };
  FORBID_TAGS?: string[];
  FORBID_ATTR?: string[];
  ALLOWED_TAGS?: string[];
  ALLOWED_ATTR?: string[];
}

/**
 * HTML 콘텐츠를 안전하게 정화합니다.
 * XSS 공격을 방지하기 위해 위험한 HTML, JavaScript 코드를 제거합니다.
 */
export const sanitizeHtml = (content: string, options?: SanitizeOptions): string => {
  if (typeof window === 'undefined') {
    return content; // 서버 사이드에서는 실행하지 않음
  }

  // 기본 설정
  const config: DOMPurifyConfig = {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ['script', 'style', 'iframe', 'frame', 'object', 'embed', 'form', 'input', 'meta', 'link'],
    FORBID_ATTR: [
      'onerror', 'onload', 'onclick', 'onmouseover', 'onmouseout', 
      'onmousedown', 'onmouseup', 'onkeydown', 'onkeyup', 'onkeypress',
      'onfocus', 'onblur', 'onchange', 'onsubmit', 'onreset', 'onselect',
      'ondblclick', 'oncontextmenu', 'ondrag', 'ondrop', 'onscroll'
    ],
    // 안전한 태그만 허용
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'em', 'u', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li', 'blockquote', 'a', 'span', 'div'
    ],
    ALLOWED_ATTR: ['href', 'target', 'rel', 'class']
  };

  // 사용자 정의 태그 설정 적용
  if (options?.allowedTags) {
    config.ALLOWED_TAGS = options.allowedTags;
  }
  if (options?.forbiddenTags) {
    config.FORBID_TAGS = [...(config.FORBID_TAGS || []), ...options.forbiddenTags];
  }

  return String(DOMPurify.sanitize(content, config));
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

/**
 * 입력 텍스트를 HTML 엔티티로 이스케이프합니다.
 * 단순 텍스트 입력의 XSS 방지용
 */
export const escapeHtml = (text: string): string => {
  const htmlEntities: { [key: string]: string } = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
    '/': '&#x2F;',
  };
  
  return text.replace(/[&<>"'/]/g, (match) => htmlEntities[match]);
}; 
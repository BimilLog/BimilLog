/**
 * 입력 필드에 대한 XSS 검증 유틸리티
 */

// 스크립트 태그와 이벤트 핸들러를 감지하는 정규식
const DANGEROUS_PATTERNS = [
  /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, // 스크립트 태그
  /on\w+\s*=\s*["']?[^"']*["']?/gi, // 이벤트 핸들러 (onclick, onload 등)
  /javascript\s*:/gi, // javascript: 프로토콜
];

// 이스케이프가 필요한 특수 문자 매핑
const ESCAPE_MAP: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#039;'
};

// 허용되지 않는 특수문자 정규식
const FORBIDDEN_CHARS = /[<>'"&\\]/g;

/**
 * 입력값에 잠재적인 XSS 공격 패턴이 있는지 확인합니다.
 * @param value 확인할 입력값
 * @returns 안전하면 true, 위험하면 false
 */
export const isInputSafe = (value: string): boolean => {
  if (!value) return true;
  
  // 위험한 패턴 체크
  const hasDangerousPattern = DANGEROUS_PATTERNS.some(pattern => pattern.test(value));
  if (hasDangerousPattern) return false;
  
  // 특수문자 체크
  return !FORBIDDEN_CHARS.test(value);
};

/**
 * 입력값에서 위험한 문자를 이스케이프합니다.
 * @param value 이스케이프할 문자열
 * @returns 이스케이프된 문자열
 */
export const escapeHTML = (value: string): string => {
  if (!value) return '';
  
  return value.replace(/[&<>"']/g, (char) => ESCAPE_MAP[char]);
};

/**
 * React Hook Form의 유효성 검사 규칙으로 사용할 수 있는 XSS 검증 함수
 */
export const validateNoXSS = (value: string): boolean => {
  if (!value) return true;
  
  // 특수문자 체크
  if (FORBIDDEN_CHARS.test(value)) {
    return false;
  }
  
  return isInputSafe(value);
}; 
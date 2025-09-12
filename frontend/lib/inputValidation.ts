/**
 * 입력 필드에 대한 XSS 검증 유틸리티
 */

// 스크립트 태그와 이벤트 핸들러를 감지하는 정규식
const DANGEROUS_PATTERNS = [
  /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, // 스크립트 태그
  /on\w+\s*=\s*["']?[^"']*["']?/gi, // 이벤트 핸들러 (onclick, onload 등)
  /javascript\s*:/gi, // javascript: 프로토콜
  /<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, // iframe 태그
  /<object\b[^<]*(?:(?!<\/object>)<[^<]*)*<\/object>/gi, // object 태그
  /<embed\b[^<]*(?:(?!<\/embed>)<[^<]*)*<\/embed>/gi, // embed 태그
  /<form\b[^<]*(?:(?!<\/form>)<[^<]*)*<\/form>/gi, // form 태그
  /<input\b[^>]*>/gi, // input 태그
  /<meta\b[^>]*>/gi, // meta 태그
  /<link\b[^>]*>/gi, // link 태그
  /data\s*:\s*text\/html/gi, // data:text/html
  /vbscript\s*:/gi, // vbscript: 프로토콜
];

// 이스케이프가 필요한 특수 문자 매핑
const ESCAPE_MAP: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#039;'
};

// 단순 텍스트 입력용 (닉네임, 제목 등)
const SIMPLE_FORBIDDEN_CHARS = /[<>'"&\\]/g;

/**
 * 입력값에 잠재적인 XSS 공격 패턴이 있는지 확인합니다.
 * @param value 확인할 입력값
 * @returns 안전하면 true, 위험하면 false
 */
export const isInputSafe = (value: string): boolean => {
  if (!value) return true;
  
  // 위험한 패턴 체크
  const hasDangerousPattern = DANGEROUS_PATTERNS.some(pattern => pattern.test(value));
  return !hasDangerousPattern;
  

};

/**
 * 단순 텍스트 입력 (제목, 닉네임 등)의 안전성을 확인합니다.
 * @param value 확인할 입력값
 * @returns 안전하면 true, 위험하면 false
 */
export const isSimpleTextSafe = (value: string): boolean => {
  if (!value) return true;
  
  // 단순 텍스트에서는 HTML 태그 자체를 허용하지 않음
  return !SIMPLE_FORBIDDEN_CHARS.test(value);
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
 * HTML 내용에서 위험한 패턴을 제거합니다.
 * @param content HTML 내용
 * @returns 정화된 내용
 */
export const removeScriptTags = (content: string): string => {
  if (!content) return '';
  
  let cleaned = content;
  
  // 위험한 패턴들을 제거
  DANGEROUS_PATTERNS.forEach(pattern => {
    cleaned = cleaned.replace(pattern, '');
  });
  
  return cleaned;
};

/**
 * React Hook Form의 유효성 검사 규칙으로 사용할 수 있는 XSS 검증 함수
 */
export const validateNoXSS = (value: string): boolean => {
  return isInputSafe(value);
};

/**
 * 제목 유효성 검사 (XSS 방지 포함)
 * @param title 검사할 제목
 * @returns { valid: boolean, message: string }
 */
export const validateTitle = (title: string): { valid: boolean; message: string } => {
  if (!title || title.trim().length === 0) {
    return { valid: false, message: "제목을 입력해주세요." };
  }
  
  if (title.length > 100) {
    return { valid: false, message: "제목은 100자 이하로 입력해주세요." };
  }
  
  if (!isSimpleTextSafe(title)) {
    return { valid: false, message: "제목에 특수문자나 HTML 태그는 사용할 수 없습니다." };
  }
  
  return { valid: true, message: "사용 가능한 제목입니다." };
};

/**
 * 댓글 내용 유효성 검사 (XSS 방지 포함)
 * @param content 검사할 댓글 내용
 * @returns { valid: boolean, message: string }
 */
export const validateComment = (content: string): { valid: boolean; message: string } => {
  if (!content || content.trim().length === 0) {
    return { valid: false, message: "댓글 내용을 입력해주세요." };
  }
  
  if (content.length > 1000) {
    return { valid: false, message: "댓글은 1000자 이하로 입력해주세요." };
  }
  
  if (!isInputSafe(content)) {
    return { valid: false, message: "댓글에 위험한 내용이 포함되어 있습니다." };
  }
  
  return { valid: true, message: "사용 가능한 댓글입니다." };
};

/**
 * 닉네임 유효성을 검사합니다.
 * - 2~8자 길이
 * - 한글, 영어, 숫자만 허용
 * @param nickname 검사할 닉네임
 * @returns { valid: boolean, message: string }
 */
export const validateNickname = (nickname: string): { valid: boolean; message: string } => {
  if (nickname.length < 2 || nickname.length > 8) {
    return { valid: false, message: "닉네임은 2~8자 사이여야 합니다." };
  }
  
  const nicknameRegex = /^[가-힣a-zA-Z0-9]+$/;
  if (!nicknameRegex.test(nickname)) {
    return { valid: false, message: "특수문자는 사용할 수 없습니다." };
  }
  
  if (!isSimpleTextSafe(nickname)) {
    return { valid: false, message: "닉네임에 위험한 문자가 포함되어 있습니다." };
  }
  
  return { valid: true, message: "사용 가능한 닉네임 형식입니다." };
}; 
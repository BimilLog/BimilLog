/**
 * 공통 검증 패턴 헬퍼
 */

export interface ValidationResult {
  isValid: boolean;
  error?: string;
}

// XSS 보안 검증을 위한 패턴들
const DANGEROUS_PATTERNS = [
  // 스크립트 태그: <script>로 시작해서 </script>로 끝나는 모든 패턴 감지
  // \b는 단어 경계, [^<]*는 <가 아닌 문자들, (?:(?!<\/script>)<[^<]*)*는 </script>가 나오기 전까지의 모든 내용
  /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi,

  // 이벤트 핸들러: onclick, onload 등 on으로 시작하는 모든 이벤트 속성 감지
  // on\w+는 on + 하나 이상의 단어 문자, \s*=\s*는 등호 앞뒤 공백, ["']?는 따옴표 선택적
  /on\w+\s*=\s*["']?[^"']*["']?/gi,

  // JavaScript 프로토콜: href="javascript:..." 같은 위험한 URL 감지
  /javascript\s*:/gi,

  // 위험한 HTML 태그들: iframe, object, embed는 외부 콘텐츠 삽입 가능
  /<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi,
  /<object\b[^<]*(?:(?!<\/object>)<[^<]*)*<\/object>/gi,
  /<embed\b[^<]*(?:(?!<\/embed>)<[^<]*)*<\/embed>/gi,

  // Data URI의 HTML: data:text/html,<script>... 같은 인라인 HTML 실행 방지
  /data\s*:\s*text\/html/gi,

  // VBScript 프로토콜: Internet Explorer에서 사용되던 위험한 스크립팅
  /vbscript\s*:/gi,
];

// 단순 텍스트 입력용 금지 문자
const SIMPLE_FORBIDDEN_CHARS = /[<>'"&\\]/g;

/**
 * XSS 공격 패턴 검사
 */
export const isInputSafe = (value: string): boolean => {
  if (!value) return true;
  return !DANGEROUS_PATTERNS.some(pattern => pattern.test(value));
};

/**
 * 단순 텍스트 안전성 검사 (HTML 태그 불허)
 */
export const isSimpleTextSafe = (value: string): boolean => {
  if (!value) return true;
  return !SIMPLE_FORBIDDEN_CHARS.test(value);
};

/**
 * 기본 검증 규칙들
 */
export const validationRules = {
  // 닉네임 검증 (통합된 버전)
  nickname: (value: string): ValidationResult => {
    if (!value || value.trim().length === 0) {
      return { isValid: false, error: '닉네임을 입력해주세요.' };
    }

    if (value.length < 2) {
      return { isValid: false, error: '닉네임은 2자 이상이어야 합니다.' };
    }

    if (value.length > 8) {
      return { isValid: false, error: '닉네임은 8자 이하여야 합니다.' };
    }

    // 특수문자 검증 (한글, 영문, 숫자만 허용 - 기존 validation.ts 규칙 적용)
    const nicknameRegex = /^[가-힣a-zA-Z0-9]+$/;
    if (!nicknameRegex.test(value)) {
      return {
        isValid: false,
        error: '특수문자는 사용할 수 없습니다.'
      };
    }

    // XSS 보안 검증 추가
    if (!isSimpleTextSafe(value)) {
      return { isValid: false, error: '닉네임에 위험한 문자가 포함되어 있습니다.' };
    }

    // 금지어 검증
    const forbiddenWords = ['관리자', 'admin', 'Administrator', '익명', 'anonymous'];
    if (forbiddenWords.some(word => value.toLowerCase().includes(word.toLowerCase()))) {
      return { isValid: false, error: '사용할 수 없는 닉네임입니다.' };
    }

    return { isValid: true };
  },

  // 이메일 검증
  email: (value: string): ValidationResult => {
    if (!value || value.trim().length === 0) {
      return { isValid: false, error: '이메일을 입력해주세요.' };
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value)) {
      return { isValid: false, error: '올바른 이메일 형식이 아닙니다.' };
    }

    return { isValid: true };
  },

  // 비밀번호 검증 (익명 게시글/댓글용 - 4자리 숫자)
  anonymousPassword: (value: string): ValidationResult => {
    if (!value || value.trim().length === 0) {
      return { isValid: false, error: '비밀번호를 입력해주세요.' };
    }

    const numPassword = Number(value.trim());
    if (isNaN(numPassword) || numPassword < 1000 || numPassword > 9999) {
      return { isValid: false, error: '비밀번호는 4자리 숫자여야 합니다 (1000-9999).' };
    }

    return { isValid: true };
  },

  // 게시글 제목 검증 (XSS 보안 포함)
  postTitle: (value: string): ValidationResult => {
    if (!value || value.trim().length === 0) {
      return { isValid: false, error: '제목을 입력해주세요.' };
    }

    if (value.length < 2) {
      return { isValid: false, error: '제목은 2자 이상이어야 합니다.' };
    }

    if (value.length > 100) {
      return { isValid: false, error: '제목은 100자 이하여야 합니다.' };
    }

    if (!isSimpleTextSafe(value)) {
      return { isValid: false, error: '제목에 특수문자나 HTML 태그는 사용할 수 없습니다.' };
    }

    return { isValid: true };
  },

  // 게시글/댓글 내용 검증 (XSS 보안 포함)
  content: (value: string, minLength: number = 1, maxLength: number = 1000): ValidationResult => {
    if (!value || value.trim().length === 0) {
      return { isValid: false, error: '내용을 입력해주세요.' };
    }

    if (value.length < minLength) {
      return { isValid: false, error: `내용은 ${minLength}자 이상이어야 합니다.` };
    }

    if (value.length > maxLength) {
      return { isValid: false, error: `내용은 ${maxLength}자 이하여야 합니다.` };
    }

    if (!isInputSafe(value)) {
      return { isValid: false, error: '내용에 위험한 스크립트가 포함되어 있습니다.' };
    }

    return { isValid: true };
  },

  // 롤링페이퍼 메시지 검증
  paperMessage: (value: string): ValidationResult => {
    return validationRules.content(value, 1, 500);
  },

  // 검색어 검증
  searchQuery: (value: string): ValidationResult => {
    if (!value || value.trim().length === 0) {
      return { isValid: false, error: '검색어를 입력해주세요.' };
    }

    if (value.length < 2) {
      return { isValid: false, error: '검색어는 2자 이상 입력해주세요.' };
    }

    if (value.length > 50) {
      return { isValid: false, error: '검색어는 50자 이하여야 합니다.' };
    }

    return { isValid: true };
  },

  // URL 검증
  url: (value: string): ValidationResult => {
    if (!value || value.trim().length === 0) {
      return { isValid: true }; // URL은 선택사항으로 처리
    }

    try {
      new URL(value);
      return { isValid: true };
    } catch {
      return { isValid: false, error: '올바른 URL 형식이 아닙니다.' };
    }
  }
} as const;

/**
 * 여러 필드를 한번에 검증하는 헬퍼
 */
type ValidationRule = (value: string, ...args: unknown[]) => ValidationResult;

interface FieldValidation {
  value: string;
  rules: ValidationRule[];
  args?: unknown[];
}

interface ValidationFields {
  [key: string]: FieldValidation;
}

export const validateFields = (fields: ValidationFields): {
  isValid: boolean;
  errors: Record<string, string>;
} => {
  const errors: Record<string, string> = {};
  let isValid = true;

  Object.entries(fields).forEach(([fieldName, { value, rules, args = [] }]) => {
    // 각 필드에 대해 여러 검증 규칙을 순차적으로 실행
    for (const rule of rules) {
      const result = rule(value, ...args);
      if (!result.isValid && result.error) {
        // 첫 번째 에러 발생 시 해당 필드의 나머지 규칙은 검사하지 않음 (fail-fast)
        // 사용자에게는 가장 우선순위가 높은 에러 메시지만 표시하여 혼란 방지
        errors[fieldName] = result.error;
        isValid = false;
        break; // 첫 번째 에러에서 중단
      }
    }
  });

  return { isValid, errors };
};

/**
 * React Hook Form과 함께 사용할 수 있는 검증 규칙 변환기
 */
export const toReactHookFormRule = (
  validationFn: ValidationRule,
  ...args: unknown[]
) => ({
  validate: (value: string) => {
    const result = validationFn(value, ...args);
    return result.isValid || result.error || true;
  }
});

/**
 * 실시간 검증을 위한 디바운스 검증 헬퍼
 */
export const createDebouncedValidator = (
  validationFn: (value: string) => ValidationResult,
  delay: number = 300
) => {
  // 클로저를 이용한 타이머 ID 보관 - 각 validator 인스턴스마다 독립적인 타이머 관리
  let timeoutId: NodeJS.Timeout;

  return (
    value: string,
    callback: (result: ValidationResult) => void
  ) => {
    // 이전 타이머가 있다면 취소 (사용자가 연속으로 입력하는 경우)
    clearTimeout(timeoutId);

    // 새로운 타이머 설정 - delay 후에 실제 검증 실행
    // 사용자가 타이핑을 멈춘 후에만 검증이 실행되어 불필요한 API 호출이나 연산 방지
    timeoutId = setTimeout(() => {
      const result = validationFn(value);
      callback(result);
    }, delay);
  };
};

/**
 * 조건부 검증 헬퍼
 */
export const conditionalValidation = (
  condition: boolean,
  validationFn: (value: string) => ValidationResult
) => {
  return (value: string): ValidationResult => {
    if (!condition) {
      return { isValid: true };
    }
    return validationFn(value);
  };
};

/**
 * 커스텀 검증 규칙 빌더 - 빌더 패턴으로 복잡한 검증 규칙을 체이닝 방식으로 구성
 */
export class ValidationBuilder {
  // 검증 함수들을 배열로 저장 - 빌더 패턴의 핵심
  private rules: ((value: string) => ValidationResult)[] = [];

  // 필수 입력 검증 추가 - 메서드 체이닝을 위해 this 반환
  required(message: string = '필수 입력 항목입니다.') {
    this.rules.push((value: string) => {
      if (!value || value.trim().length === 0) {
        return { isValid: false, error: message };
      }
      return { isValid: true };
    });
    return this; // 메서드 체이닝 지원
  }

  // 최소 길이 검증 추가 - 동적으로 메시지 생성하거나 커스텀 메시지 사용
  minLength(min: number, message?: string) {
    this.rules.push((value: string) => {
      if (value && value.length < min) {
        return {
          isValid: false,
          error: message || `최소 ${min}자 이상 입력해주세요.`
        };
      }
      return { isValid: true };
    });
    return this; // 메서드 체이닝 지원
  }

  // 최대 길이 검증 추가
  maxLength(max: number, message?: string) {
    this.rules.push((value: string) => {
      if (value && value.length > max) {
        return {
          isValid: false,
          error: message || `최대 ${max}자까지 입력 가능합니다.`
        };
      }
      return { isValid: true };
    });
    return this; // 메서드 체이닝 지원
  }

  // 정규표현식 패턴 검증 추가
  pattern(regex: RegExp, message: string) {
    this.rules.push((value: string) => {
      if (value && !regex.test(value)) {
        return { isValid: false, error: message };
      }
      return { isValid: true };
    });
    return this; // 메서드 체이닝 지원
  }

  // 커스텀 검증 함수 추가 - 복잡한 비즈니스 로직도 통합 가능
  custom(validationFn: (value: string) => ValidationResult) {
    this.rules.push(validationFn);
    return this; // 메서드 체이닝 지원
  }

  // 빌더 패턴의 최종 단계 - 모든 규칙을 하나의 검증 함수로 결합
  build() {
    return (value: string): ValidationResult => {
      // 등록된 모든 규칙을 순차 실행, 첫 번째 실패에서 중단 (fail-fast)
      for (const rule of this.rules) {
        const result = rule(value);
        if (!result.isValid) {
          return result;
        }
      }
      return { isValid: true };
    };
  }
}

/**
 * 비밀번호 validation (4자리 숫자) - 기존 validation.ts와 호환
 * @param password 검증할 비밀번호
 * @param isAuthenticated 인증 여부 (인증된 경우 비밀번호 불필요)
 * @returns 유효한 비밀번호 숫자 또는 undefined
 * @throws 비밀번호가 유효하지 않은 경우 Error
 */
export function validatePassword(password: string, isAuthenticated: boolean): number | undefined {
  if (isAuthenticated) return undefined;

  if (!password.trim()) {
    throw new Error("비밀번호를 입력해주세요.");
  }

  const numPassword = Number(password.trim());
  if (isNaN(numPassword) || numPassword < 1000 || numPassword > 9999) {
    throw new Error("비밀번호는 4자리 숫자여야 합니다 (1000-9999).");
  }

  return numPassword;
}

/**
 * 자주 사용되는 검증 규칙 조합들
 */
export const commonValidations = {
  // 게시글 작성 폼
  postForm: {
    title: [validationRules.postTitle],
    content: [validationRules.content],
    anonymousPassword: [validationRules.anonymousPassword]
  },

  // 댓글 작성 폼
  commentForm: {
    content: [(value: string) => validationRules.content(value, 1, 500)],
    anonymousPassword: [validationRules.anonymousPassword]
  },

  // 롤링페이퍼 메시지 폼
  paperMessageForm: {
    content: [validationRules.paperMessage],
    userName: [validationRules.nickname]
  },

  // 사용자 설정 폼
  userSettingsForm: {
    nickname: [validationRules.nickname]
  },

  // 검색 폼
  searchForm: {
    query: [validationRules.searchQuery]
  }
} as const;
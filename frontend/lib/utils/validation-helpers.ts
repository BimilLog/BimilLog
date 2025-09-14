/**
 * 공통 검증 패턴 헬퍼
 */

export interface ValidationResult {
  isValid: boolean;
  error?: string;
}

/**
 * 기본 검증 규칙들
 */
export const validationRules = {
  // 닉네임 검증
  nickname: (value: string): ValidationResult => {
    if (!value || value.trim().length === 0) {
      return { isValid: false, error: '닉네임을 입력해주세요.' };
    }

    if (value.length < 2) {
      return { isValid: false, error: '닉네임은 2자 이상이어야 합니다.' };
    }

    if (value.length > 20) {
      return { isValid: false, error: '닉네임은 20자 이하여야 합니다.' };
    }

    // 특수문자 검증 (한글, 영문, 숫자, 일부 특수문자만 허용)
    const nicknameRegex = /^[가-힣a-zA-Z0-9._-]+$/;
    if (!nicknameRegex.test(value)) {
      return {
        isValid: false,
        error: '닉네임은 한글, 영문, 숫자, 점(.), 하이픈(-), 언더스코어(_)만 사용할 수 있습니다.'
      };
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

  // 비밀번호 검증 (익명 게시글/댓글용)
  anonymousPassword: (value: string): ValidationResult => {
    if (!value || value.trim().length === 0) {
      return { isValid: false, error: '비밀번호를 입력해주세요.' };
    }

    if (value.length < 4) {
      return { isValid: false, error: '비밀번호는 4자 이상이어야 합니다.' };
    }

    if (value.length > 20) {
      return { isValid: false, error: '비밀번호는 20자 이하여야 합니다.' };
    }

    return { isValid: true };
  },

  // 게시글 제목 검증
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

    return { isValid: true };
  },

  // 게시글/댓글 내용 검증
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
interface FieldValidation {
  value: string;
  rules: ((value: string, ...args: any[]) => ValidationResult)[];
  args?: any[];
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
    for (const rule of rules) {
      const result = rule(value, ...args);
      if (!result.isValid && result.error) {
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
  validationFn: (value: string, ...args: any[]) => ValidationResult,
  ...args: any[]
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
  let timeoutId: NodeJS.Timeout;

  return (
    value: string,
    callback: (result: ValidationResult) => void
  ) => {
    clearTimeout(timeoutId);

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
 * 커스텀 검증 규칙 빌더
 */
export class ValidationBuilder {
  private rules: ((value: string) => ValidationResult)[] = [];

  required(message: string = '필수 입력 항목입니다.') {
    this.rules.push((value: string) => {
      if (!value || value.trim().length === 0) {
        return { isValid: false, error: message };
      }
      return { isValid: true };
    });
    return this;
  }

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
    return this;
  }

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
    return this;
  }

  pattern(regex: RegExp, message: string) {
    this.rules.push((value: string) => {
      if (value && !regex.test(value)) {
        return { isValid: false, error: message };
      }
      return { isValid: true };
    });
    return this;
  }

  custom(validationFn: (value: string) => ValidationResult) {
    this.rules.push(validationFn);
    return this;
  }

  build() {
    return (value: string): ValidationResult => {
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
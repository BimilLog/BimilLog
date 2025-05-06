import { validateNoXSS } from './inputValidation';

/**
 * 게시글 제목 유효성 검사
 * @param title 게시글 제목
 * @returns [isValid, errorMessage] - 유효성 검사 결과 및 오류 메시지
 */
export const validatePostTitle = (title: string): [boolean, string | null] => {
  if (!title.trim()) {
    return [false, "제목을 입력해주세요."];
  }

  if (title.trim().length > 30) {
    return [false, "제목은 30자 이내로 작성해주세요."];
  }

  if (!validateNoXSS(title)) {
    return [false, "특수문자(<, >, &, \", ', \\)는 사용이 불가능합니다."];
  }

  return [true, null];
};

/**
 * 게시글 내용 유효성 검사
 * @param content 게시글 내용
 * @returns [isValid, errorMessage] - 유효성 검사 결과 및 오류 메시지
 */
export const validatePostContent = (content: string): [boolean, string | null] => {
  if (!content.trim()) {
    return [false, "내용을 입력해주세요."];
  }

  if (content.trim().length > 1000) {
    return [false, "내용은 1000자 이내로 작성해주세요."];
  }

  if (!validateNoXSS(content)) {
    return [false, "특수문자(<, >, &, \", ', \\)는 사용이 불가능합니다."];
  }

  return [true, null];
}; 
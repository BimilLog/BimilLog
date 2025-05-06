/**
 * 백엔드 API 요청을 위한 fetch 클라이언트 유틸리티
 * 모든 요청에 공통 헤더를 자동으로 추가합니다.
 */

type FetchOptions = RequestInit & {
  skipIdentifierHeader?: boolean;
};

/**
 * 향상된 fetch API - 모든 요청에 X-Frontend-Identifier 헤더를 자동으로 추가합니다
 * @param url API 요청 URL
 * @param options fetch 옵션
 * @returns fetch 응답
 */
export const fetchClient = async (url: string, options: FetchOptions = {}) => {
  const { skipIdentifierHeader = false, headers = {}, ...restOptions } = options;
  
  // 환경 변수에서 식별자 가져오기
  const headerValue = process.env.NEXT_PUBLIC_SECRET_IDENTIFIER;
  
  // 기본 헤더 설정
  const defaultHeaders: Record<string, string> = {
    ...(headers as Record<string, string>),
  };
  
  // 식별자 헤더 추가 (skipIdentifierHeader이 true가 아닐 경우)
  if (!skipIdentifierHeader && headerValue) {
    defaultHeaders['X-Frontend-Identifier'] = headerValue;
  }
  
  // fetch 요청 실행
  return fetch(url, {
    ...restOptions,
    headers: defaultHeaders,
    credentials: options.credentials || 'include', // 기본적으로 credentials 포함
  });
};

export default fetchClient; 
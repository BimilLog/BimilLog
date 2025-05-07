// fetchClient.ts

import Cookies from 'js-cookie';

type FetchOptions = RequestInit & {
  skipIdentifierHeader?: boolean;
  skipCsrfHeader?: boolean;
};

export const fetchClient = async (url: string, options: FetchOptions = {}) => {
  const {
    skipIdentifierHeader = false,
    skipCsrfHeader = false,
    headers = {},
    ...restOptions
  } = options;

  const headerValue = process.env.NEXT_PUBLIC_SECRET_IDENTIFIER;

  const defaultHeaders: Record<string, string> = {
    ...(headers as Record<string, string>),
  };

  if (!skipIdentifierHeader && headerValue) {
    defaultHeaders['X-Frontend-Identifier'] = headerValue;
  }

  if (!skipCsrfHeader) {
    const csrfToken = Cookies.get('XSRF-TOKEN');
    if (csrfToken) {
      defaultHeaders['X-XSRF-TOKEN'] = csrfToken;
    }
  }

  return fetch(url, {
    ...restOptions,
    headers: defaultHeaders,
    credentials: 'include', // ← 무조건 include로 고정
  });
};

export default fetchClient;

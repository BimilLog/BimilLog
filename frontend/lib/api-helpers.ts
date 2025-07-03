import type { ApiResponse } from './api';

// 전역 이벤트를 사용한 리로그인 알림
export const triggerReloginRequired = () => {
  const event = new CustomEvent('needsRelogin', {
    detail: {
      title: '다른 기기에서 로그아웃됨',
      message: '다른 기기에서 로그아웃 하셨습니다. 다시 로그인 해주세요.'
    }
  });
  window.dispatchEvent(event);
};

// API 응답을 래핑하는 헬퍼 함수
export const handleApiResponse = <T>(response: ApiResponse<T>): ApiResponse<T> => {
  // needsRelogin 플래그가 있으면 전역 이벤트 발생
  if (response.needsRelogin) {
    triggerReloginRequired();
  }
  
  return response;
};

// API 호출을 래핑하는 헬퍼 함수
export const apiCall = async <T>(
  apiFunction: () => Promise<ApiResponse<T>>
): Promise<ApiResponse<T>> => {
  try {
    const response = await apiFunction();
    return handleApiResponse(response);
  } catch (error) {
    console.error('API call failed:', error);
    return {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error'
    };
  }
};

// 사용 예시를 위한 타입 정의
export type ApiCallFunction<T> = () => Promise<ApiResponse<T>>; 
/**
 * 카카오 OAuth 인증 관련 유틸리티
 */

/**
 * 카카오 친구 목록 동의를 위한 OAuth URL 생성
 */
export const generateKakaoConsentUrl = (): string => {
  const clientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
  const redirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;
  
  if (!clientId) {
    console.error('NEXT_PUBLIC_KAKAO_CLIENT_ID 환경변수가 설정되지 않았습니다.');
    throw new Error('카카오 클라이언트 ID가 설정되지 않았습니다.');
  }
  
  if (!redirectUri) {
    console.error('NEXT_PUBLIC_KAKAO_REDIRECT_URI 환경변수가 설정되지 않았습니다.');
    throw new Error('카카오 리다이렉트 URI가 설정되지 않았습니다.');
  }

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'friends', // 친구 목록 동의 스코프
    prompt: 'consent', // 강제 동의 화면 표시
  });

  return `https://kauth.kakao.com/oauth/authorize?${params.toString()}`;
};

/**
 * 카카오 친구 동의를 위한 로그아웃 후 리다이렉트
 */
export const logoutAndRedirectToConsent = (): void => {
  try {
    const consentUrl = generateKakaoConsentUrl();
    
    // 동의 URL을 세션 스토리지에 저장 (로그아웃 후 사용)
    sessionStorage.setItem('kakaoConsentUrl', consentUrl);
    
    // 현재 페이지 URL을 저장 (동의 완료 후 돌아올 페이지)
    sessionStorage.setItem('returnUrl', window.location.pathname);
    
    // 로그아웃 페이지로 이동 (동의 처리를 위한 특별한 플래그 포함)
    window.location.href = '/logout?consent=true';
    
  } catch (error) {
    console.error('카카오 동의 처리 실패:', error);
    throw error;
  }
}; 
export interface KakaoAuthConfig {
  authUrl: string;
  clientId: string;
  redirectUri: string;
}

export class KakaoAuthManager {
  private config: KakaoAuthConfig;

  constructor(config?: Partial<KakaoAuthConfig>) {
    this.config = {
      authUrl: config?.authUrl || process.env.NEXT_PUBLIC_KAKAO_AUTH_URL || "",
      clientId: config?.clientId || process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID || "",
      redirectUri: config?.redirectUri || process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI || ""
    };
    
    this.validateConfig();
  }

  private validateConfig(): void {
    if (!this.config.authUrl || !this.config.clientId || !this.config.redirectUri) {
      console.error("Kakao Auth configuration is incomplete");
    }
  }

  buildAuthUrl(postAuthRedirectUrl?: string): string {
    const params = new URLSearchParams({
      response_type: "code",
      client_id: this.config.clientId,
      redirect_uri: this.config.redirectUri
    });

    if (postAuthRedirectUrl) {
      params.append("state", encodeURIComponent(postAuthRedirectUrl));
    }

    return `${this.config.authUrl}?${params.toString()}`;
  }

  extractCodeFromUrl(searchParams: URLSearchParams): string | null {
    return searchParams.get("code");
  }

  extractErrorFromUrl(searchParams: URLSearchParams): string | null {
    return searchParams.get("error");
  }

  extractStateFromUrl(searchParams: URLSearchParams): string | null {
    const state = searchParams.get("state");
    return state ? decodeURIComponent(state) : null;
  }

  isCallbackUrl(pathname: string): boolean {
    return pathname === "/auth/callback";
  }

  redirectToKakaoAuth(postAuthRedirectUrl?: string): void {
    const authUrl = this.buildAuthUrl(postAuthRedirectUrl);
    window.location.href = authUrl;
  }
}

export const kakaoAuthManager = new KakaoAuthManager();

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

/**
 * 카카오 SDK 공유 기능
 */

// 카카오 SDK 전역 타입 선언
declare global {
  interface Window {
    Kakao: any;
  }
}

// 카카오 SDK 로드 및 초기화
export const initializeKakao = (): Promise<boolean> => {
  return new Promise((resolve) => {
    // 서버사이드에서는 실행하지 않음
    if (typeof window === 'undefined') {
      resolve(false);
      return;
    }

    // 이미 초기화되어 있으면 성공 반환
    if (window.Kakao?.isInitialized?.()) {
      resolve(true);
      return;
    }

    // 카카오 SDK가 이미 로드되어 있으면 초기화만 진행
    if (window.Kakao) {
      const appKey = process.env.NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY;
      if (appKey) {
        window.Kakao.init(appKey);
        resolve(true);
      } else {
        console.error('카카오 앱 키가 설정되지 않았습니다.');
        resolve(false);
      }
      return;
    }

    // 카카오 SDK 스크립트 로드
    const script = document.createElement('script');
    script.src = 'https://t1.kakaocdn.net/kakao_js_sdk/2.7.2/kakao.min.js';
    script.integrity = 'sha384-TiCUE00h649CAMonG018J2ujOgDKW/kVWlChEuu4jK2vxfAAD0eZxzCKakxg55G4';
    script.crossOrigin = 'anonymous';

    script.onload = () => {
      const appKey = process.env.NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY;
      if (appKey && window.Kakao) {
        window.Kakao.init(appKey);
        resolve(true);
      } else {
        console.error('카카오 SDK 로드 후 초기화 실패');
        resolve(false);
      }
    };

    script.onerror = () => {
      console.error('카카오 SDK 스크립트 로드 실패');
      resolve(false);
    };

    document.head.appendChild(script);
  });
};

// 롤링페이퍼 공유하기 (피드 A형)
export const shareRollingPaper = async (
  userName: string,
  messageCount: number = 0
): Promise<boolean> => {
  const isInitialized = await initializeKakao();
  if (!isInitialized) {
    console.error('카카오 SDK 초기화 실패');
    return false;
  }

  try {
    const shareUrl = `${window.location.origin}/rolling-paper/${encodeURIComponent(userName)}`;

    window.Kakao.Share.sendDefault({
      objectType: 'feed',
      content: {
        title: `${userName}님의 롤링페이퍼`,
        description: `${userName}님에게 따뜻한 메시지를 남겨보세요! 현재 ${messageCount}개의 메시지가 있어요.`,
        imageUrl: 'https://postfiles.pstatic.net/MjAyNTA2MjZfODgg/MDAxNzUwOTI0NDQ5NDU4.zZz8zqcDJtERdyJ3uCHQdqMPCq8f1nAYN5nHYY4E1Q0g._A1ZRNw0ez8hbO96WyW8laMX3QZPKSr2PXZoVagjCU8g.PNG/log.png?type=w3840',
        link: {
          mobileWebUrl: shareUrl,
          webUrl: shareUrl,
        },
      },
      buttons: [
        {
          title: '메시지 남기기',
          link: {
            mobileWebUrl: shareUrl,
            webUrl: shareUrl,
          },
        },
      ],
    });

    return true;
  } catch (error) {
    console.error('카카오톡 공유 실패:', error);
    return false;
  }
};

// 게시글 공유하기 (피드 A형)
export const sharePost = async (
  postId: number,
  title: string,
  author: string,
  content: string,
  likes: number = 0
): Promise<boolean> => {
  const isInitialized = await initializeKakao();
  if (!isInitialized) {
    console.error('카카오 SDK 초기화 실패');
    return false;
  }

  try {
    const shareUrl = `${window.location.origin}/board/post/${postId}`;
    const description = content.length > 100 ? content.substring(0, 100) + '...' : content;

    window.Kakao.Share.sendDefault({
      objectType: 'feed',
      content: {
        title: `${title}`,
        description: `${author}님이 작성한 글입니다.\n\n${description}\n\n${likes}개의 추천`,
        imageUrl: 'https://postfiles.pstatic.net/MjAyNTA2MjZfODgg/MDAxNzUwOTI0NDQ5NDU4.zZz8zqcDJtERdyJ3uCHQdqMPCq8f1nAYN5nHYY4E1Q0g._A1ZRNw0ez8hbO96WyW8laMX3QZPKSr2PXZoVagjCU8g.PNG/log.png?type=w3840',
        link: {
          mobileWebUrl: shareUrl,
          webUrl: shareUrl,
        },
      },
      buttons: [
        {
          title: '게시글 보기',
          link: {
            mobileWebUrl: shareUrl,
            webUrl: shareUrl,
          },
        },
      ],
    });

    return true;
  } catch (error) {
    console.error('카카오톡 공유 실패:', error);
    return false;
  }
};

// 서비스 공유하기 (피드 A형)
export const shareService = async (): Promise<boolean> => {
  const isInitialized = await initializeKakao();
  if (!isInitialized) {
    console.error('카카오 SDK 초기화 실패');
    return false;
  }

  try {
    const shareUrl = window.location.origin;

    window.Kakao.Share.sendDefault({
      objectType: 'feed',
      content: {
        title: '비밀로그 - 익명 롤링페이퍼 서비스',
        description: '친구들에게 익명으로 따뜻한 메시지를 받아보세요! 나만의 롤링페이퍼를 만들고 소중한 추억을 남겨보세요.',
        imageUrl: 'https://postfiles.pstatic.net/MjAyNTA2MjZfODgg/MDAxNzUwOTI0NDQ5NDU4.zZz8zqcDJtERdyJ3uCHQdqMPCq8f1nAYN5nHYY4E1Q0g._A1ZRNw0ez8hbO96WyW8laMX3QZPKSr2PXZoVagjCU8g.PNG/log.png?type=w3840',
        link: {
          mobileWebUrl: shareUrl,
          webUrl: shareUrl,
        },
      },
      buttons: [
        {
          title: '지금 시작하기',
          link: {
            mobileWebUrl: shareUrl,
            webUrl: shareUrl,
          },
        },
      ],
    });

    return true;
  } catch (error) {
    console.error('카카오톡 공유 실패:', error);
    return false;
  }
};

// 대체 공유 기능 (카카오톡 공유 실패 시)
export const fallbackShare = (url: string, title: string, text: string) => {
  if (navigator.share) {
    // Web Share API 사용
    navigator.share({
      title,
      text,
      url,
    }).catch(() => {
      // Web Share API 실패 시 클립보드 복사
      copyToClipboard(url);
    });
  } else {
    // Web Share API 미지원 시 클립보드 복사
    copyToClipboard(url);
  }
};

// 클립보드에 복사
const copyToClipboard = (text: string) => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text).then(() => {
      alert('링크가 클립보드에 복사되었습니다!');
    }).catch(() => {
      promptCopy(text);
    });
  } else {
    promptCopy(text);
  }
};

// 복사 프롬프트
const promptCopy = (text: string) => {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  document.body.appendChild(textArea);
  textArea.select();

  try {
    document.execCommand('copy');
    alert('링크가 클립보드에 복사되었습니다!');
  } catch {
    prompt('아래 링크를 복사해주세요:', text);
  }

  document.body.removeChild(textArea);
};
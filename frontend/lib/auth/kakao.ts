import { logger } from '@/lib/utils'

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
      logger.error("Kakao Auth configuration is incomplete");
    }
  }

  buildAuthUrl(postAuthRedirectUrl?: string): string {
    const params = new URLSearchParams({
      response_type: "code",
      client_id: this.config.clientId,
      redirect_uri: this.config.redirectUri
    });

    // state 매개변수에 인증 후 리다이렉트할 URL 인코딩하여 저장
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
    // state 매개변수 디코딩: 인코딩된 리다이렉트 URL 복원
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
    logger.error('NEXT_PUBLIC_KAKAO_CLIENT_ID 환경변수가 설정되지 않았습니다.');
    throw new Error('카카오 클라이언트 ID가 설정되지 않았습니다.');
  }

  if (!redirectUri) {
    logger.error('NEXT_PUBLIC_KAKAO_REDIRECT_URI 환경변수가 설정되지 않았습니다.');
    throw new Error('카카오 리다이렉트 URI가 설정되지 않았습니다.');
  }

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'friends', // 카카오 친구 목록 권한 요청
    prompt: 'consent', // 이미 동의한 권한도 다시 동의 화면 표시 (권한 재설정용)
  });

  return `https://kauth.kakao.com/oauth/authorize?${params.toString()}`;
};

/**
 * 카카오 친구 동의를 위한 로그아웃 후 리다이렉트
 */
// 친구 동의 처리의 복잡한 플로우: 로그아웃 → 카카오 동의 → 로그인 → 원래 페이지 복귀
export const logoutAndRedirectToConsent = (): void => {
  try {
    const consentUrl = generateKakaoConsentUrl();

    // 1단계: 동의 URL을 세션에 저장 (로그아웃 후에도 유지되어야 함)
    sessionStorage.setItem('kakaoConsentUrl', consentUrl);

    // 2단계: 현재 페이지를 저장 (전체 플로우 완료 후 돌아올 위치)
    sessionStorage.setItem('returnUrl', window.location.pathname);

    // 3단계: 특별한 로그아웃 처리 (consent=true 플래그로 일반 로그아웃과 구분)
    // 이 플래그로 로그아웃 페이지에서 일반적인 홈 리다이렉트가 아닌 동의 플로우를 진행
    window.location.href = '/logout?consent=true';

  } catch (error) {
    logger.error('카카오 동의 처리 실패:', error);
    throw error;
  }
};

/**
 * 카카오 SDK 공유 기능
 */

// 카카오 SDK 타입 정의
interface KakaoSDK {
  init: (appKey: string) => void;
  isInitialized: () => boolean;
  Share: {
    sendDefault: (options: {
      objectType: string;
      content: {
        title: string;
        description: string;
        imageUrl: string;
        link: {
          mobileWebUrl: string;
          webUrl: string;
        };
      };
      buttons: Array<{
        title: string;
        link: {
          mobileWebUrl: string;
          webUrl: string;
        };
      }>;
    }) => void;
  };
}

// 카카오 SDK 전역 타입 선언
declare global {
  interface Window {
    Kakao: KakaoSDK;
  }
}

// 카카오 SDK 로드 및 초기화 - 비동기로 SDK를 로드하고 초기화하는 복잡한 처리 과정
// 카카오 SDK 초기화 상태 관리: 3단계 초기화 체크 (SSR 방지 → 이미 초기화됨 → 로드됨 → 로드 필요)
export const initializeKakao = (): Promise<boolean> => {
  return new Promise((resolve) => {
    // 1단계: SSR 환경 체크 - 서버사이드에서는 window 객체가 없음
    if (typeof window === 'undefined') {
      resolve(false);
      return;
    }

    // 2단계: 이미 초기화 완료된 상태 체크 - 재초기화 방지
    if (window.Kakao?.isInitialized?.()) {
      resolve(true);
      return;
    }

    // 3단계: SDK는 로드되었지만 초기화되지 않은 상태 - 초기화만 진행
    if (window.Kakao) {
      const appKey = process.env.NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY;

      if (appKey) {
        try {
          window.Kakao.init(appKey);
          resolve(true);
        } catch (error) {
          logger.error('카카오 SDK 초기화 실패:', error);
          resolve(false);
        }
      } else {
        logger.error('카카오 앱 키가 설정되지 않았습니다.');
        resolve(false);
      }
      return;
    }

    // 4단계: SDK가 없는 상태 - 동적 스크립트 로드 후 초기화
    const script = document.createElement('script');
    script.src = 'https://t1.kakaocdn.net/kakao_js_sdk/2.7.2/kakao.min.js';
    script.integrity = 'sha384-TiCUE00h649CAMonG018J2ujOgDKW/kVWlChEuu4jK2vxfAAD0eZxzCKakxg55G4';
    script.crossOrigin = 'anonymous';

    // 로드 성공 시: SDK 로드 → 초기화 → 완료 신호 전송
    script.onload = () => {
      const appKey = process.env.NEXT_PUBLIC_KAKAO_JAVA_SCRIPT_KEY;

      if (appKey && window.Kakao) {
        try {
          window.Kakao.init(appKey);
          resolve(true);
        } catch (error) {
          logger.error('카카오 SDK 로드 후 초기화 실패:', error);
          resolve(false);
        }
      } else {
        logger.error('카카오 SDK 로드 후 초기화 조건 불충족');
        resolve(false);
      }
    };

    // 로드 실패 시: 네트워크 오류나 CDN 문제로 스크립트를 불러올 수 없음
    script.onerror = (error) => {
      logger.error('카카오 SDK 스크립트 로드 실패:', error);
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
    logger.error('카카오 SDK 초기화 실패');
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
    logger.error('카카오톡 공유 실패:', error);
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
    logger.error('카카오 SDK 초기화 실패');
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
    logger.error('카카오톡 공유 실패:', error);
    return false;
  }
};

// 서비스 공유하기 (피드 A형)
export const shareService = async (): Promise<boolean> => {
  const isInitialized = await initializeKakao();
  if (!isInitialized) {
    logger.error('카카오 SDK 초기화 실패');
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
    logger.error('카카오톡 공유 실패:', error);
    return false;
  }
};

// fallback 공유 메커니즘: 카카오 공유 실패 시 대체 수단 제공 (2단계 폴백 체인)
export const fallbackShare = (url: string, title: string, text: string) => {
  if (navigator.share) {
    // 1순위 폴백: 모바일 브라우저의 네이티브 공유 기능 사용
    navigator.share({
      title,
      text,
      url,
    }).catch(() => {
      // 2순위 폴백: Web Share API 실패 시 클립보드 복사로 최종 대응
      copyToClipboard(url);
    });
  } else {
    // 데스크톱이나 구형 브라우저: Web Share API 미지원 시 바로 클립보드 복사
    copyToClipboard(url);
  }
};

// 클립보드 복사: 모던 API → 레거시 API 폴백 체인
const copyToClipboard = (text: string) => {
  if (navigator.clipboard) {
    // 모던 브라우저: Clipboard API 사용
    navigator.clipboard.writeText(text).then(() => {
      alert('링크가 클립보드에 복사되었습니다!');
    }).catch(() => {
      // Clipboard API 실패 시 레거시 방식으로 폴백
      promptCopy(text);
    });
  } else {
    // 구형 브라우저: 바로 레거시 방식 사용
    promptCopy(text);
  }
};

// 레거시 복사 방식: 임시 textarea 생성 → 선택 → 복사 명령 실행
const promptCopy = (text: string) => {
  // DOM에 임시 textarea 엘리먼트 생성
  const textArea = document.createElement('textarea');
  textArea.value = text;
  document.body.appendChild(textArea);
  textArea.select(); // 텍스트 선택

  try {
    // 구형 브라우저용 복사 명령 실행
    document.execCommand('copy');
    alert('링크가 클립보드에 복사되었습니다!');
  } catch {
    // execCommand도 실패 시 사용자에게 직접 복사 요청
    prompt('아래 링크를 복사해주세요:', text);
  }

  // 임시 엘리먼트 정리
  document.body.removeChild(textArea);
};
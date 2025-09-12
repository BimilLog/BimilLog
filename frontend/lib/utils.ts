import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}



export function formatDate(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (seconds < 60) return "방금 전";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}일 전`;

  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).replace(/\. /g, '.').slice(0, -1);
}

/**
 * User-Agent 기반 모바일/태블릿 감지
 */
export function isMobileOrTablet(): boolean {
  if (typeof window === 'undefined') return false
  
  const userAgent = navigator.userAgent.toLowerCase()
  const mobileKeywords = [
    'mobile', 'android', 'iphone', 'ipad', 'ipod', 
    'blackberry', 'windows phone', 'opera mini'
  ]
  
  return mobileKeywords.some(keyword => userAgent.includes(keyword))
}

/**
 * HTML 문자열에서 텍스트만 추출
 */
export function stripHtml(html: string): string {
  // <br> 태그를 줄바꿈으로 변환
  let result = html.replace(/<br\s*\/?>/gi, '\n');
  // <p> 태그 끝을 줄바꿈으로 변환
  result = result.replace(/<\/p>/gi, '\n');
  // 다른 HTML 태그들 제거
  result = result.replace(/<[^>]*>?/gm, '');
  // 연속된 줄바꿈을 정리 (3개 이상을 2개로)
  result = result.replace(/\n{3,}/g, '\n\n');
  return result;
}

/**
 * 비밀번호 validation (4자리 숫자)
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
 * 사용자 이름에서 이니셜 추출 (최대 2자)
 * @param name 사용자 이름
 * @returns 이니셜 (예: "홍길동" -> "홍길", "John Doe" -> "JD")
 */
export function getInitials(name: string): string {
  if (!name || !name.trim()) return '';
  
  const trimmedName = name.trim();
  
  // 한글/한자 이름인 경우 (공백 없이 연속된 문자)
  if (/^[\u4e00-\u9fff\uac00-\ud7af]+$/.test(trimmedName)) {
    return trimmedName.slice(0, 2);
  }
  
  // 영문 이름인 경우 (공백으로 구분된 단어들)
  const words = trimmedName.split(/\s+/).filter(word => word.length > 0);
  if (words.length >= 2) {
    return (words[0][0] + words[1][0]).toUpperCase();
  }
  
  // 단일 단어인 경우 첫 2글자
  return trimmedName.slice(0, 2).toUpperCase();
}

/**
 * FCM 토큰 가져오기 (모바일/태블릿에서만)
 */
export async function getFCMToken(): Promise<string | null> {
  // 모바일/태블릿이 아니면 FCM 토큰을 가져오지 않음
  if (!isMobileOrTablet()) {
    if (process.env.NODE_ENV === 'development') {
      console.log('데스크톱 환경 - FCM 토큰 건너뛰기');
    }
    return null
  }

  try {
    // Firebase 관련 모듈 동적 import (SSR 안전)
    const { getMessaging, getToken } = await import('firebase/messaging')
    const { initializeApp, getApps } = await import('firebase/app')
    
    // Firebase 앱이 이미 초기화되었는지 확인
    let app
    if (getApps().length === 0) {
      // Firebase 설정
      const firebaseConfig = {
        apiKey: "AIzaSyDQHWI_zhIjqp_SJz0Fdv7xtG6mIZfwBhU",
        authDomain: "growfarm-6cd79.firebaseapp.com",
        projectId: "growfarm-6cd79",
        storageBucket: "growfarm-6cd79.firebasestorage.app",
        messagingSenderId: "763805350293",
        appId: "1:763805350293:web:68b1b3ca3a294b749b1e9c",
        measurementId: "G-G9C4KYCEEJ"
      }
      app = initializeApp(firebaseConfig)
    } else {
      app = getApps()[0]
    }

    const messaging = getMessaging(app)
    
    // 서비스워커 등록
    const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js')
    
    // FCM 토큰 가져오기 (서버->클라이언트 일방향이므로 VAPID 키 불필요)
    const token = await getToken(messaging, {
      serviceWorkerRegistration: registration
    })

    if (token) {
      if (process.env.NODE_ENV === 'development') {
        console.log('FCM 토큰 획득 성공:', token.substring(0, 20) + '...');
      }
      return token
    } else {
      if (process.env.NODE_ENV === 'development') {
        console.log('FCM 토큰 획득 실패 - 브라우저 알림 권한을 확인해주세요.');
      }
      return null
    }
  } catch (error) {
    console.error('FCM 토큰 가져오기 실패:', error)
    return null
  }
}

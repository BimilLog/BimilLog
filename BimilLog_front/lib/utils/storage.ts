/**
 * Type-safe localStorage/sessionStorage 유틸리티
 * 프로젝트 전반에 걸쳐 일관된 스토리지 접근을 위한 통합 유틸리티
 */

// 타입 정의
export interface RecentVisit {
  nickname: string;
  visitedAt: string;
  displayName: string;
}


// 안전한 Storage 접근을 위한 헬퍼 함수
function isStorageAvailable(type: 'localStorage' | 'sessionStorage'): boolean {
  if (typeof window === 'undefined') return false;
  
  try {
    const storage = window[type];
    const test = '__storage_test__';
    storage.setItem(test, test);
    storage.removeItem(test);
    return true;
  } catch {
    return false;
  }
}

/**
 * localStorage 유틸리티
 */
export const localStorage = {
  // 브라우저 가이드 숨김 상태
  getBrowserGuideHidden(): boolean {
    if (!isStorageAvailable('localStorage')) return false;
    
    try {
      return window.localStorage.getItem('browser-guide-hidden') === 'true';
    } catch {
      return false;
    }
  },

  setBrowserGuideHidden(value: boolean): void {
    if (!isStorageAvailable('localStorage')) return;
    
    try {
      window.localStorage.setItem('browser-guide-hidden', value.toString());
    } catch {
      // 에러 무시
    }
  },

  // 브라우저 가이드 숨김 만료 시간
  getBrowserGuideHideUntil(): number | null {
    if (!isStorageAvailable('localStorage')) return null;
    
    try {
      const value = window.localStorage.getItem('browser-guide-hide-until');
      return value ? parseInt(value, 10) : null;
    } catch {
      return null;
    }
  },

  setBrowserGuideHideUntil(timestamp: number): void {
    if (!isStorageAvailable('localStorage')) return;
    
    try {
      window.localStorage.setItem('browser-guide-hide-until', timestamp.toString());
    } catch {
      // 에러 무시
    }
  },

  // 최근 방문한 롤링페이퍼 목록
  getRecentVisits(): RecentVisit[] {
    if (!isStorageAvailable('localStorage')) return [];
    
    try {
      const stored = window.localStorage.getItem('recent-visits');
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  },

  setRecentVisits(visits: RecentVisit[]): void {
    if (!isStorageAvailable('localStorage')) return;
    
    try {
      window.localStorage.setItem('recent-visits', JSON.stringify(visits));
    } catch {
      // 에러 무시
    }
  },

  addRecentVisit(nickname: string): void {
    const displayName = decodeURIComponent(nickname);
    const newVisit: RecentVisit = {
      nickname,
      visitedAt: new Date().toISOString(),
      displayName,
    };

    const current = this.getRecentVisits();
    const filtered = current.filter(v => v.nickname !== nickname);
    const updated = [newVisit, ...filtered].slice(0, 2); // 최대 2개
    this.setRecentVisits(updated);
  },

  removeRecentVisit(nickname: string): void {
    const current = this.getRecentVisits();
    const filtered = current.filter(v => v.nickname !== nickname);
    this.setRecentVisits(filtered);
  },

  clearRecentVisits(): void {
    if (!isStorageAvailable('localStorage')) return;
    
    try {
      window.localStorage.removeItem('recent-visits');
    } catch {
      // 에러 무시
    }
  },

  // 30일 이전 방문 기록 정리
  cleanupOldVisits(): void {
    const visits = this.getRecentVisits();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    
    const filtered = visits.filter(visit => {
      try {
        return new Date(visit.visitedAt) > thirtyDaysAgo;
      } catch {
        return false; // 잘못된 날짜 형식은 제거
      }
    });
    
    this.setRecentVisits(filtered);
  }
};

/**
 * sessionStorage 유틸리티
 */
export const sessionStorage = {
  // 임시 사용자 UUID (회원가입 과정)
  getTempUserUuid(): string | null {
    if (!isStorageAvailable('sessionStorage')) return null;
    
    try {
      return window.sessionStorage.getItem('tempUserUuid');
    } catch {
      return null;
    }
  },

  setTempUserUuid(uuid: string): void {
    if (!isStorageAvailable('sessionStorage')) return;
    
    try {
      window.sessionStorage.setItem('tempUserUuid', uuid);
    } catch {
      // 에러 무시
    }
  },

  removeTempUserUuid(): void {
    if (!isStorageAvailable('sessionStorage')) return;
    
    try {
      window.sessionStorage.removeItem('tempUserUuid');
    } catch {
      // 에러 무시
    }
  },

  // 리다이렉트 URL (인증 후 돌아갈 페이지)
  getReturnUrl(): string | null {
    if (!isStorageAvailable('sessionStorage')) return null;
    
    try {
      return window.sessionStorage.getItem('returnUrl');
    } catch {
      return null;
    }
  },

  setReturnUrl(url: string): void {
    if (!isStorageAvailable('sessionStorage')) return;
    
    try {
      window.sessionStorage.setItem('returnUrl', url);
    } catch {
      // 에러 무시
    }
  },

  removeReturnUrl(): void {
    if (!isStorageAvailable('sessionStorage')) return;
    
    try {
      window.sessionStorage.removeItem('returnUrl');
    } catch {
      // 에러 무시
    }
  },

  // 카카오 동의 URL
  getKakaoConsentUrl(): string | null {
    if (!isStorageAvailable('sessionStorage')) return null;
    
    try {
      return window.sessionStorage.getItem('kakaoConsentUrl');
    } catch {
      return null;
    }
  },

  setKakaoConsentUrl(url: string): void {
    if (!isStorageAvailable('sessionStorage')) return;
    
    try {
      window.sessionStorage.setItem('kakaoConsentUrl', url);
    } catch {
      // 에러 무시
    }
  },

  removeKakaoConsentUrl(): void {
    if (!isStorageAvailable('sessionStorage')) return;
    
    try {
      window.sessionStorage.removeItem('kakaoConsentUrl');
    } catch {
      // 에러 무시
    }
  },

  // 모든 인증 관련 세션 데이터 정리
  clearAuthSession(): void {
    this.removeTempUserUuid();
    this.removeReturnUrl();
    this.removeKakaoConsentUrl();
  }
};

/**
 * 통합 스토리지 유틸리티
 */
export const storage = {
  local: localStorage,
  session: sessionStorage,

  // 모든 스토리지 정리 (개발용)
  clearAll(): void {
    if (typeof window === 'undefined') return;

    try {
      window.localStorage.clear();
      window.sessionStorage.clear();
    } catch {
      // 에러 무시
    }
  }
};

// 직접 export - RecentVisits 컴포넌트용
export const getRecentVisits = () => localStorage.getRecentVisits();
export const removeRecentVisit = (nickname: string) => localStorage.removeRecentVisit(nickname);
export const clearRecentVisits = () => localStorage.clearRecentVisits();

// 기본 내보내기
export default storage;
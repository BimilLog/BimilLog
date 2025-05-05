// authStore.ts
// 인증 관련 유틸리티 함수와 전역 상태 통합 관리

import { create } from 'zustand';
import { UserDTO } from '@/components/types/schema';

// API 엔드포인트 상수
const API_BASE_URL = 'http://localhost:8080';
const API_ENDPOINTS = {
  ME: `${API_BASE_URL}/auth/me`,
  LOGOUT: `${API_BASE_URL}/auth/logout`,
  LOGIN: `${API_BASE_URL}/auth/login`,
  SIGNUP: `${API_BASE_URL}/auth/signUp`,
};

// 인증 스토어 상태 인터페이스
interface AuthState {
  user: UserDTO | null;
  isLoading: boolean;
  isInitialized: boolean;
  setUser: (user: UserDTO | null) => void;
  logout: () => Promise<void>;
  checkAuth: () => Promise<UserDTO | null>;
}

/**
 * 인증 관련 Zustand 스토어 생성
 */
const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  isLoading: false,
  isInitialized: false,
  
  // 사용자 정보 설정
  setUser: (user) => set({ user }),
  
  // 로그아웃
  logout: async () => {
    try {
      set({ isLoading: true });
      const response = await fetch(API_ENDPOINTS.LOGOUT, {
        method: 'POST',
        credentials: 'include',
      });
      
      if (response.ok) {
        set({ user: null });
        
        // 로그아웃 성공 시 메인 페이지로 이동
        window.location.href = '/';
      }
    } catch (error) {
      console.error('로그아웃 실패:', error);
    } finally {
      set({ isLoading: false });
    }
  },
  
  // 사용자 인증 상태 확인
  checkAuth: async () => {
    // 이미 로딩 중이면 기존 요청 결과 기다림
    if (get().isLoading) return get().user;
    
    set({ isLoading: true });
    
    try {
      const response = await fetch(API_ENDPOINTS.ME, {
        method: 'POST',
        credentials: 'include',
      });
  
      if (!response.ok) {
        set({ user: null });
        return null;
      }
  
      // 응답 데이터를 직접 사용자 객체로 사용
      const userData = await response.json();
      
      set({ user: userData, isInitialized: true });
      return userData;
    } catch (error) {
      console.error('사용자 정보 불러오기 실패:', error);
      set({ user: null });
      return null;
    } finally {
      set({ isLoading: false });
    }
  }
}));

export default useAuthStore; 
import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { authQuery, authCommand, userCommand, sseManager, type Member } from '@/lib/api';
import { logger } from '@/lib/utils';
import { fcmManager } from '@/lib/auth/fcm';

interface AuthState {
  user: Member | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isLoggingOut: boolean;

  // Actions
  setUser: (user: Member | null) => void;
  setLoading: (loading: boolean) => void;
  
  // API Actions
  refreshUser: () => Promise<void>;
  login: (postAuthRedirectUrl?: string) => void;
  logout: () => Promise<void>;
  signUp: (userName: string) => Promise<{ success: boolean; error?: string }>;
  updateUserName: (userName: string) => Promise<boolean>;

  
  // Event Handler
  handleNeedsRelogin: (title: string, message: string) => void;
}

export const useAuthStore = create<AuthState>()(
  devtools(
    persist(
      (set, get) => ({
        user: null,
        isLoading: true,
        isAuthenticated: false,
        isLoggingOut: false,
        
        setUser: (user) => 
          set({ 
            user, 
            isAuthenticated: !!user 
          }),
        
        setLoading: (isLoading) => 
          set({ isLoading }),
        
        refreshUser: async () => {
          try {
            const response = await authQuery.getCurrentUser();

            if (response.success && response.data) {
              set({
                user: response.data,
                isAuthenticated: true
              });

              // 기존회원(로그인) 또는 신규회원(회원가입 완료) 시 SSE 연결
              // memberName이 있어야만 실시간 알림을 받을 수 있음 (회원가입 완료 상태)
              if (response.data.memberName?.trim()) {
                logger.log(`사용자 인증 완료 (${response.data.memberName}) - SSE 연결 시작`);
                sseManager.connect();
              }
            } else {
            set({
              user: null,
              isAuthenticated: false
            });
            sseManager.disconnect({ resetToast: true });
          }
        } catch (error) {
          logger.error("Failed to fetch user:", error);
          set({
            user: null,
            isAuthenticated: false
          });
          sseManager.disconnect({ resetToast: true });
        } finally {
          set({ isLoading: false });
        }
      },
        
        login: (postAuthRedirectUrl?: string) => {
          // 카카오 OAuth URL 생성을 위한 환경변수들
          const kakaoAuthUrl = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL;
          const kakaoClientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
          const kakaoRedirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;
          const responseType = "code";

          // 기본 카카오 OAuth URL 생성
          let url = `${kakaoAuthUrl}?response_type=${responseType}&client_id=${kakaoClientId}&redirect_uri=${kakaoRedirectUri}`;

          // 로그인 후 특정 페이지로 이동하고 싶은 경우 state 파라미터에 추가
          if (postAuthRedirectUrl) {
            url += `&state=${encodeURIComponent(postAuthRedirectUrl)}`;
          }
          window.location.href = url;
        },
        
        logout: async () => {
          if (get().isLoggingOut) {
            logger.warn("이미 로그아웃 처리 중입니다. 추가 요청을 무시합니다.");
            return;
          }

          set({ isLoggingOut: true });

          try {
            logger.log("로그아웃 시작...");

            const response = await authCommand.logout();

            logger.log("로그아웃 API 응답:", response);
          } catch (error) {
            logger.error("Logout failed:", error);
            throw error; // 에러를 다시 throw하여 호출자가 처리할 수 있도록
          } finally {
            logger.log("로그아웃 정리 작업 시작");

            // 1. FCM 캐시 정리
            fcmManager.clearCache();
            logger.log("FCM 캐시 정리 완료");

            // 2. localStorage FCM 관련 데이터 완전 삭제
            if (typeof window !== "undefined") {
              localStorage.removeItem("fcm_token");
              localStorage.removeItem("notification_permission_asked");
              localStorage.removeItem("notification_permission_skipped");
              logger.log("FCM localStorage 정리 완료");
            }

            // 3. SSE 연결 해제
            sseManager.disconnect({ resetToast: true });
            logger.log("SSE 연결 해제 완료");

            // 4. 사용자 상태 초기화 (Zustand persist가 이를 localStorage에 저장)
            set({
              user: null,
              isAuthenticated: false,
              isLoading: false,
              isLoggingOut: false
            });
            logger.log("사용자 상태 초기화 완료");
          }
        },
        
        signUp: async (userName: string) => {
          try {
            const response = await authCommand.signUp(userName);
            if (response.success) {
              await get().refreshUser();
              return { success: true };
            }
            return {
              success: false,
              error: response.error || "회원가입에 실패했습니다."
            };
          } catch (error) {
            logger.error("SignUp failed:", error);
            return {
              success: false,
              error: "회원가입 중 오류가 발생했습니다."
            };
          }
        },
        
        updateUserName: async (userName: string) => {
          const { user } = get();
          if (!user) {
            logger.error("User not available for username update");
            return false;
          }
          
          try {
            const response = await userCommand.updateUserName(userName);
            if (response.success) {
              await get().refreshUser();
              return true;
            }
            return false;
          } catch (error) {
            logger.error("Update username failed:", error);
            return false;
          }
        },
        
        handleNeedsRelogin: (title: string, message: string) => {
          // SSE 연결 해제하고 사용자 상태 초기화
          sseManager.disconnect({ resetToast: true });
          set({ 
            user: null, 
            isAuthenticated: false,
            isLoggingOut: false
          });
          
          // 바로 로그인 페이지로 리다이렉트
          window.location.href = "/login";
        },
      }),
      {
        name: 'auth-storage',
        // persist 설정: user 정보만 localStorage에 저장
        // isLoading, isAuthenticated는 매번 새로 계산되므로 저장하지 않음
        partialize: (state) => ({
          user: state.user
        }),
      }
    )
  )
);

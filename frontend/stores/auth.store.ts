import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { authQuery, authCommand, userQuery, userCommand, sseManager, type User } from '@/lib/api';

interface AuthState {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  
  // Actions
  setUser: (user: User | null) => void;
  setLoading: (loading: boolean) => void;
  
  // API Actions
  refreshUser: () => Promise<void>;
  login: (postAuthRedirectUrl?: string) => void;
  logout: () => Promise<void>;
  signUp: (userName: string, uuid: string) => Promise<{ success: boolean; error?: string }>;
  updateUserName: (userName: string) => Promise<boolean>;
  deleteAccount: () => Promise<boolean>;
  
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
              if (response.data.userName?.trim()) {
                if (process.env.NODE_ENV === 'development') {
                  console.log(`사용자 인증 완료 (${response.data.userName}) - SSE 연결 시작`);
                }
                sseManager.connect();
              }
            } else {
              set({ 
                user: null, 
                isAuthenticated: false 
              });
              sseManager.disconnect();
            }
          } catch (error) {
            console.error("Failed to fetch user:", error);
            set({ 
              user: null, 
              isAuthenticated: false 
            });
            sseManager.disconnect();
          } finally {
            set({ isLoading: false });
          }
        },
        
        login: (postAuthRedirectUrl?: string) => {
          const kakaoAuthUrl = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL;
          const kakaoClientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
          const kakaoRedirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;
          const responseType = "code";
          
          let url = `${kakaoAuthUrl}?response_type=${responseType}&client_id=${kakaoClientId}&redirect_uri=${kakaoRedirectUri}`;
          
          if (postAuthRedirectUrl) {
            url += `&state=${encodeURIComponent(postAuthRedirectUrl)}`;
          }
          window.location.href = url;
        },
        
        logout: async () => {
          try {
            if (process.env.NODE_ENV === 'development') {
              console.log("로그아웃 시작...");
            }
            
            const response = await authCommand.logout();
            
            if (process.env.NODE_ENV === 'development') {
              console.log("로그아웃 API 응답:", response);
            }
          } catch (error) {
            console.error("Logout failed:", error);
          } finally {
            // 항상 SSE 연결 해제하고 상태 초기화
            sseManager.disconnect();
            set({ 
              user: null, 
              isAuthenticated: false 
            });
          }
        },
        
        signUp: async (userName: string, uuid: string) => {
          try {
            const response = await authCommand.signUp(userName, uuid);
            if (response.success) {
              await get().refreshUser();
              return { success: true };
            }
            return { 
              success: false, 
              error: response.error || "회원가입에 실패했습니다." 
            };
          } catch (error) {
            console.error("SignUp failed:", error);
            return { 
              success: false, 
              error: "회원가입 중 오류가 발생했습니다." 
            };
          }
        },
        
        updateUserName: async (userName: string) => {
          const { user } = get();
          if (!user) {
            console.error("User not available for username update");
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
            console.error("Update username failed:", error);
            return false;
          }
        },
        
        deleteAccount: async () => {
          try {
            const response = await authCommand.withdraw();
            if (response.success) {
              sseManager.disconnect();
              set({ 
                user: null, 
                isAuthenticated: false 
              });
              window.location.href = "/";
              return true;
            }
            return false;
          } catch (error) {
            console.error("Delete account failed:", error);
            return false;
          }
        },
        
        handleNeedsRelogin: (title: string, message: string) => {
          // SSE 연결 해제하고 사용자 상태 초기화
          sseManager.disconnect();
          set({ 
            user: null, 
            isAuthenticated: false 
          });
          
          // 바로 로그인 페이지로 리다이렉트
          window.location.href = "/login";
        },
      }),
      {
        name: 'auth-storage',
        partialize: (state) => ({ 
          user: state.user 
        }),
      }
    )
  )
);
import { create, type StoreApi } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { authQuery, authCommand, userCommand, sseManager, type Member, type SocialProvider } from '@/lib/api';
import { logger } from '@/lib/utils';
import { fcmManager } from '@/lib/auth/fcm';
import { registerFcmTokenAction } from '@/lib/actions/notification';

interface AuthState {
  user: Member | null;
  provider: SocialProvider | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isLoggingOut: boolean;

  setUser: (user: Member | null) => void;
  setProvider: (provider: SocialProvider | null) => void;
  setLoading: (loading: boolean) => void;

  refreshUser: () => Promise<void>;
  login: (postAuthRedirectUrl?: string) => void;
  logout: () => Promise<void>;
  signUp: (userName: string) => Promise<{ success: boolean; error?: string }>;
  updateUserName: (userName: string) => Promise<boolean>;

  handleNeedsRelogin: (title: string, message: string) => void;
}

const REFRESH_TTL_MS = 10_000;
let lastSuccessfulRefreshAt = 0;
let refreshPromise: Promise<void> | null = null;
let rehydrateStoreSet: StoreApi<AuthState>['setState'] | null = null;
let fcmAutoRegisterPromise: Promise<void> | null = null;
let lastRegisteredFcmToken: string | null = null;

const resetRefreshCache = () => {
  lastSuccessfulRefreshAt = 0;
};

const autoRegisterFcmToken = async () => {
  if (typeof window === 'undefined') return;
  if (!fcmManager.isSupported()) return;
  if (!('Notification' in window) || Notification.permission !== 'granted') return;

  let token = window.localStorage.getItem('fcm_token');

  if (!token) {
    try {
      token = (await fcmManager.ensureTokenForLogin()) ?? null;
    } catch (error) {
      logger.warn('Failed to fetch FCM token for auto-registration:', error);
      return;
    }

    if (!token) {
      logger.log('FCM auto-registration skipped: unable to retrieve token');
      return;
    }

    window.localStorage.setItem('fcm_token', token);
  }

  if (token === lastRegisteredFcmToken) {
    return;
  }

  try {
    const result = await registerFcmTokenAction(token);
    if (result.success) {
      lastRegisteredFcmToken = token;
      logger.log('FCM token automatically registered with server');
    } else {
      logger.warn('FCM token auto-registration failed:', result.error);
    }
  } catch (error) {
    logger.warn('FCM token auto-registration error:', error);
  }
};

const scheduleFcmAutoRegister = () => {
  if (fcmAutoRegisterPromise) {
    return fcmAutoRegisterPromise;
  }

  fcmAutoRegisterPromise = autoRegisterFcmToken().finally(() => {
    fcmAutoRegisterPromise = null;
  });

  return fcmAutoRegisterPromise;
};

export const useAuthStore = create<AuthState>()(
  devtools(
    persist(
      (set, get) => {
        const performSessionCleanup = (
          {
            resetToast = true,
            preserveLogoutState = false,
          }: { resetToast?: boolean; preserveLogoutState?: boolean } = {}
        ) => {
          fcmManager.clearCache();
          lastRegisteredFcmToken = null;

          if (typeof window !== 'undefined') {
            localStorage.removeItem('fcm_token');
            localStorage.removeItem('notification_permission_asked');
            localStorage.removeItem('notification_permission_skipped');
          }

          sseManager.disconnect({ resetToast });
          resetRefreshCache();

          const nextState: Partial<AuthState> = {
            user: null,
            provider: null,
            isAuthenticated: false,
            isLoading: false,
          };

          if (!preserveLogoutState) {
            nextState.isLoggingOut = false;
          }

          set(nextState);
        };

        const shouldUseCachedUser = () => {
          if (!get().user) return false;
          return Date.now() - lastSuccessfulRefreshAt < REFRESH_TTL_MS;
        };

        rehydrateStoreSet = set;

        return {
          user: null,
          provider: null,
          isLoading: true,
          isAuthenticated: false,
          isLoggingOut: false,

          setUser: (user) =>
            set({
              user,
              isAuthenticated: !!user,
            }),

          setProvider: (provider) => set({ provider }),

          setLoading: (isLoading) => set({ isLoading }),

          refreshUser: async () => {
            if (refreshPromise) {
              await refreshPromise;
              return;
            }

            if (shouldUseCachedUser()) {
              set({
                isLoading: false,
                isAuthenticated: true,
              });
              return;
            }

            const runRefresh = async () => {
              if (!get().isLoading) {
                set({ isLoading: true });
              }

              try {
                const response = await authQuery.getCurrentUser();

                if (response.success && response.data) {
                  set({
                    user: response.data,
                    isAuthenticated: true,
                  });

                  lastSuccessfulRefreshAt = Date.now();

                  if (response.data.memberName?.trim()) {
                    logger.log(`User authenticated (${response.data.memberName}) - opening SSE connection`);
                    sseManager.connect();
                  }

                  scheduleFcmAutoRegister();
                } else {
                  performSessionCleanup({ resetToast: true, preserveLogoutState: true });
                }
              } catch (error) {
                logger.error('Failed to fetch user:', error);
                performSessionCleanup({ resetToast: true, preserveLogoutState: true });
              } finally {
                set({ isLoading: false });
              }
            };

            const pending = runRefresh();
            refreshPromise = pending.finally(() => {
              refreshPromise = null;
            });

            await pending;
          },

          login: (postAuthRedirectUrl?: string) => {
            const kakaoAuthUrl = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL;
            const kakaoClientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
            const kakaoRedirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;
            const responseType = 'code';

            let url = `${kakaoAuthUrl}?response_type=${responseType}&client_id=${kakaoClientId}&redirect_uri=${kakaoRedirectUri}`;

            if (postAuthRedirectUrl) {
              url += `&state=${encodeURIComponent(postAuthRedirectUrl)}`;
            }
            window.location.href = url;
          },

          logout: async () => {
            if (get().isLoggingOut) {
              logger.warn('Logout already in progress; ignoring duplicate request.');
              return;
            }

            set({ isLoggingOut: true });

            try {
              logger.log('Attempting logout...');
              const response = await authCommand.logout();
              logger.log('Logout API response:', response);

              performSessionCleanup({ resetToast: true });
            } catch (error) {
              logger.error('Logout failed:', error);
              set({ isLoggingOut: false });
              throw error;
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
                error: response.error || '회원가입에 실패했습니다.',
              };
            } catch (error) {
              logger.error('SignUp failed:', error);
              return {
                success: false,
                error: '회원가입 중 오류가 발생했습니다.',
              };
            }
          },

          updateUserName: async (userName: string) => {
            const { user } = get();
            if (!user) {
              logger.error('User not available for username update');
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
              logger.error('Update username failed:', error);
              return false;
            }
          },

          handleNeedsRelogin: (_title, _message) => {
            performSessionCleanup({ resetToast: true });
            window.location.href = '/login';
          },
        };
      },
      {
        name: 'auth-storage',
        partialize: (state) => ({
          user: state.user,
          provider: state.provider,
        }),
        onRehydrateStorage: () => (state) => {
          rehydrateStoreSet?.({
            isAuthenticated: !!state?.user,
            isLoading: false,
          });
        },
      }
    )
  )
);

// SSEManager에 인증 상태 체커 등록
// SSE 재연결 시 인증 만료된 사용자의 무한 재시도를 방지
sseManager.setAuthChecker(() => {
  const state = useAuthStore.getState();
  return state.isAuthenticated && !!state.user?.memberName?.trim();
});

export const __authStoreInternals = {
  reset: () => {
    resetRefreshCache();
    refreshPromise = null;
  },
};

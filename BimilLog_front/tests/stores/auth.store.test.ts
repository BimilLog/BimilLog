import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/lib/api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/api')>('@/lib/api');
  return {
    ...actual,
    authQuery: {
      getCurrentUser: vi.fn(),
    },
    authCommand: {
      logout: vi.fn(),
    },
    userCommand: {
      updateUserName: vi.fn(),
    },
    sseManager: {
      connect: vi.fn(),
      disconnect: vi.fn(),
      setAuthChecker: vi.fn(),
    },
  };
});

vi.mock('@/lib/utils', () => ({
  logger: {
    log: vi.fn(),
    error: vi.fn(),
    warn: vi.fn(),
  },
}));

vi.mock('@/lib/auth/fcm', () => ({
  fcmManager: {
    clearCache: vi.fn(),
  },
}));

import { useAuthStore, __authStoreInternals } from '@/stores/auth.store';
import { authQuery, authCommand, sseManager } from '@/lib/api';
import { fcmManager } from '@/lib/auth/fcm';

const baseUser = {
  memberId: 1,
  settingId: 10,
  socialNickname: 'kakao',
  thumbnailImage: 'https://example.com/profile.png',
  memberName: '재익',
  role: 'USER' as const,
};

const resetStoreState = () => {
  (useAuthStore as unknown as { persist?: { clearStorage?: () => void } }).persist?.clearStorage?.();
  useAuthStore.setState({
    user: null,
    isLoading: true,
    isAuthenticated: false,
    isLoggingOut: false,
  });
  if ('localStorage' in globalThis) {
    localStorage.clear();
  }
  __authStoreInternals.reset();
};

describe('useAuthStore', () => {
  beforeEach(() => {
    resetStoreState();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('refreshUser succeeds and opens SSE connection', async () => {
    vi.mocked(authQuery.getCurrentUser).mockResolvedValue({
      success: true,
      data: baseUser,
    });

    await useAuthStore.getState().refreshUser();

    const state = useAuthStore.getState();
    expect(state.user?.memberName).toBe('재익');
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
    expect(vi.mocked(sseManager.connect)).toHaveBeenCalledTimes(1);
    expect(vi.mocked(sseManager.disconnect)).not.toHaveBeenCalled();
  });

  it('refreshUser failure clears auth state and disconnects SSE', async () => {
    vi.mocked(authQuery.getCurrentUser).mockResolvedValue({
      success: false,
      error: 'Unauthorized',
    });

    await useAuthStore.getState().refreshUser();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
    expect(vi.mocked(sseManager.disconnect)).toHaveBeenCalledWith({ resetToast: true });
  });

  it('refreshUser uses cached state within TTL to avoid duplicate calls', async () => {
    const nowSpy = vi.spyOn(Date, 'now').mockReturnValue(0);
    vi.mocked(authQuery.getCurrentUser).mockResolvedValue({
      success: true,
      data: baseUser,
    });

    await useAuthStore.getState().refreshUser();
    expect(authQuery.getCurrentUser).toHaveBeenCalledTimes(1);

    nowSpy.mockReturnValue(1000);
    await useAuthStore.getState().refreshUser();
    expect(authQuery.getCurrentUser).toHaveBeenCalledTimes(1);

    nowSpy.mockRestore();
  });

  it('logout clears client state only when API succeeds', async () => {
    useAuthStore.setState({
      user: baseUser,
      isAuthenticated: true,
      isLoading: false,
      isLoggingOut: false,
    });
    vi.mocked(authCommand.logout).mockResolvedValue({ success: true });

    await useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoggingOut).toBe(false);
    expect(vi.mocked(fcmManager.clearCache)).toHaveBeenCalled();
    expect(vi.mocked(sseManager.disconnect)).toHaveBeenCalledWith({ resetToast: true });
  });

  it('logout preserves state when API fails', async () => {
    useAuthStore.setState({
      user: baseUser,
      isAuthenticated: true,
      isLoading: false,
      isLoggingOut: false,
    });
    vi.mocked(authCommand.logout).mockRejectedValue(new Error('network'));

    await expect(useAuthStore.getState().logout()).rejects.toThrow('network');

    const state = useAuthStore.getState();
    expect(state.user).toEqual(baseUser);
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoggingOut).toBe(false);
    expect(vi.mocked(sseManager.disconnect)).not.toHaveBeenCalled();
  });
});

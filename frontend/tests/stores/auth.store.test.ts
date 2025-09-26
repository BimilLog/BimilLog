import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mockGetCurrentUser = vi.fn();
const mockLogout = vi.fn();
const mockSignUp = vi.fn();
const mockUpdateUserName = vi.fn();
const mockSseConnect = vi.fn();
const mockSseDisconnect = vi.fn();

vi.mock("@/lib/api", () => ({
  authQuery: {
    getCurrentUser: mockGetCurrentUser,
  },
  authCommand: {
    logout: mockLogout,
    signUp: mockSignUp,
  },
  userCommand: {
    updateUserName: mockUpdateUserName,
  },
  sseManager: {
    connect: mockSseConnect,
    disconnect: mockSseDisconnect,
  },
}));

const mockLogger = {
  log: vi.fn(),
  error: vi.fn(),
  warn: vi.fn(),
};

vi.mock("@/lib/utils", () => ({
  logger: mockLogger,
}));

import { useAuthStore } from "@/stores/auth.store";

const resetStoreState = () => {
  (useAuthStore as unknown as { persist?: { clearStorage?: () => void } }).persist?.clearStorage?.();
  useAuthStore.setState({
    user: null,
    isLoading: true,
    isAuthenticated: false,
  });
  if ("localStorage" in globalThis) {
    localStorage.clear();
  }
};

describe("useAuthStore", () => {
  beforeEach(() => {
    resetStoreState();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("refreshUser 성공 시 사용자 정보를 저장하고 SSE를 연결한다", async () => {
    mockGetCurrentUser.mockResolvedValue({
      success: true,
      data: {
        userId: 1,
        settingId: 10,
        socialNickname: "카카오",
        thumbnailImage: "https://example.com/profile.png",
        userName: "홍길동",
        role: "USER",
      },
    });

    await useAuthStore.getState().refreshUser();

    const state = useAuthStore.getState();
    expect(state.user?.userName).toBe("홍길동");
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
    expect(mockSseConnect).toHaveBeenCalledTimes(1);
    expect(mockSseDisconnect).not.toHaveBeenCalled();
  });

  it("refreshUser 실패 시 인증 상태를 초기화하고 SSE 연결을 해제한다", async () => {
    mockGetCurrentUser.mockResolvedValue({
      success: false,
      error: "Unauthorized",
    });

    await useAuthStore.getState().refreshUser();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
    expect(mockSseDisconnect).toHaveBeenCalledTimes(1);
    expect(mockSseConnect).not.toHaveBeenCalled();
  });
});

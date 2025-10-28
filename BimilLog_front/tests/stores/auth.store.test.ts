import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/lib/api", () => ({
  authQuery: {
    getCurrentUser: vi.fn(),
  },
  authCommand: {
    logout: vi.fn(),
    signUp: vi.fn(),
  },
  userCommand: {
    updateUserName: vi.fn(),
  },
  sseManager: {
    connect: vi.fn(),
    disconnect: vi.fn(),
  },
}));

vi.mock("@/lib/utils", () => ({
  logger: {
    log: vi.fn(),
    error: vi.fn(),
    warn: vi.fn(),
  },
}));

import { useAuthStore } from "@/stores/auth.store";
import { authQuery, authCommand, sseManager } from "@/lib/api";

const resetStoreState = () => {
  (useAuthStore as unknown as { persist?: { clearStorage?: () => void } }).persist?.clearStorage?.();
  useAuthStore.setState({
    user: null,
    isLoading: true,
    isAuthenticated: false,
    isLoggingOut: false,
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
    vi.mocked(authQuery.getCurrentUser).mockResolvedValue({
      success: true,
      data: {
        memberId: 1,
        settingId: 10,
        socialNickname: "카카오",
        thumbnailImage: "https://example.com/profile.png",
        memberName: "홍길동",
        role: "USER",
      },
    });

    await useAuthStore.getState().refreshUser();

    const state = useAuthStore.getState();
    expect(state.user?.memberName).toBe("홍길동");
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
    expect(vi.mocked(sseManager.connect)).toHaveBeenCalledTimes(1);
    expect(vi.mocked(sseManager.disconnect)).not.toHaveBeenCalled();
  });

  it("refreshUser 실패 시 인증 상태를 초기화하고 SSE 연결을 해제한다", async () => {
    vi.mocked(authQuery.getCurrentUser).mockResolvedValue({
      success: false,
      error: "Unauthorized",
    });

    await useAuthStore.getState().refreshUser();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
    expect(vi.mocked(sseManager.disconnect)).toHaveBeenCalledTimes(1);
    expect(vi.mocked(sseManager.disconnect)).toHaveBeenCalledWith({ resetToast: true });
    expect(vi.mocked(sseManager.connect)).not.toHaveBeenCalled();
  });
});

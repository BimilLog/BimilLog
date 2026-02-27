import { renderHook, waitFor, act } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { useNotifications } from "@/hooks/features/useNotifications";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";

// Mock modules first
vi.mock("@/lib/api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api")>("@/lib/api");
  return {
    ...actual,
    sseManager: {
      isConnected: vi.fn(() => false),
      getConnectionState: vi.fn(() => "DISCONNECTED"),
      connect: vi.fn(),
      disconnect: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addStatusListener: vi.fn(),
      removeStatusListener: vi.fn(),
      hasShownConnectedToast: vi.fn(() => false),
      markConnectedToastShown: vi.fn(),
    },
  };
});

vi.mock("@/hooks", () => ({
  useAuth: vi.fn(() => ({
    isAuthenticated: false,
    user: null,
  })),
}));

vi.mock("@/lib/utils", () => ({
  logger: {
    log: vi.fn(),
    error: vi.fn(),
    warn: vi.fn(),
  },
}));

// Import mocked modules for use in tests
import { sseManager } from "@/lib/api";
import { useAuth } from "@/hooks";

// Notification API 모킹
const mockNotification = vi.fn();
global.Notification = mockNotification as any;
Object.defineProperty(global.Notification, 'permission', {
  writable: true,
  value: "default"
});
global.Notification.requestPermission = vi.fn(() => Promise.resolve("granted" as NotificationPermission));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => {
    const { createElement } = React;
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
};

describe("useNotifications", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("인증되지 않은 사용자는 SSE에 연결하지 않는다", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      user: null,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      updateUserName: vi.fn(),
      refreshUser: vi.fn(),
    });

    const { result } = renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isSSEConnected).toBe(false);
    expect(result.current.connectionState).toBe("DISCONNECTED");
    expect(vi.mocked(sseManager.addEventListener)).not.toHaveBeenCalled();
  });

  it("닉네임이 없는 사용자는 SSE에 연결하지 않는다", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: {
        memberId: 1,
        settingId: 1,
        socialNickname: "test",
        thumbnailImage: "",
        memberName: "",
        role: "USER",
      },
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      updateUserName: vi.fn(),
      refreshUser: vi.fn(),
    });

    const { result } = renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isSSEConnected).toBe(false);
    expect(result.current.connectionState).toBe("DISCONNECTED");
    expect(vi.mocked(sseManager.addEventListener)).not.toHaveBeenCalled();
  });

  it("인증된 사용자는 SSE에 연결하고 이벤트 리스너를 등록한다", async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: {
        memberId: 1,
        settingId: 1,
        socialNickname: "test",
        thumbnailImage: "",
        memberName: "테스트사용자",
        role: "USER",
      },
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      updateUserName: vi.fn(),
      refreshUser: vi.fn(),
    });

    const { result } = renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    // removeEventListener가 먼저 호출되어 기존 리스너 제거
    expect(vi.mocked(sseManager).removeEventListener).toHaveBeenCalledWith("notification");

    // addEventListener가 호출되어 새 리스너 등록
    expect(vi.mocked(sseManager).addEventListener).toHaveBeenCalledWith(
      "notification",
      expect.any(Function)
    );

    const statusListener = vi.mocked(sseManager).addStatusListener.mock.calls.at(-1)?.[0];
    expect(statusListener).toBeDefined();
    act(() => {
      statusListener?.("connected");
    });

    expect(result.current.isSSEConnected).toBe(true);
    expect(result.current.connectionState).toBe("CONNECTED");
  });

  it("알림을 수신하면 브라우저 알림을 표시한다", async () => {
    Object.defineProperty(global.Notification, 'permission', {
      writable: true,
      value: "granted"
    });

    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: {
        memberId: 1,
        settingId: 1,
        socialNickname: "test",
        thumbnailImage: "",
        memberName: "테스트사용자",
        role: "USER",
      },
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      updateUserName: vi.fn(),
      refreshUser: vi.fn(),
    });

    renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    // addEventListener 호출 확인
    expect(vi.mocked(sseManager).addEventListener).toHaveBeenCalled();

    // 등록된 이벤트 핸들러 가져오기
    const eventHandler = vi.mocked(sseManager).addEventListener.mock.calls[0][1];

    // 알림 데이터로 이벤트 핸들러 실행
    const notificationData = {
      id: 1,
      notificationType: "COMMENT" as const,
      content: "새로운 댓글이 달렸습니다",
      url: "/post/123",
      createdAt: new Date().toISOString(),
      read: false,
    };

    eventHandler(notificationData);

    // Notification 생성자 호출 확인
    expect(mockNotification).toHaveBeenCalledWith("새로운 댓글이 달렸습니다", {
      body: "/post/123",
      icon: "/favicon.ico",
    });
  });

  it("30초마다 연결 상태를 확인한다", async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: {
        memberId: 1,
        settingId: 1,
        socialNickname: "test",
        thumbnailImage: "",
        memberName: "테스트사용자",
        role: "USER",
      },
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      updateUserName: vi.fn(),
      refreshUser: vi.fn(),
    });

    // 초기 상태: DISCONNECTED
    vi.mocked(sseManager).isConnected.mockReturnValue(false);
    vi.mocked(sseManager).getConnectionState.mockReturnValue("DISCONNECTED");

    const { result } = renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    const initialCalls = vi.mocked(sseManager).getConnectionState.mock.calls.length;

    act(() => {
      vi.advanceTimersByTime(1000);
    });
    const afterFirstTick = vi.mocked(sseManager).getConnectionState.mock.calls.length;
    expect(afterFirstTick).toBeGreaterThan(initialCalls);

    // 연결 상태 변경 시뮬레이션
    vi.mocked(sseManager).isConnected.mockReturnValue(true);
    vi.mocked(sseManager).getConnectionState.mockReturnValue("CONNECTED");

    act(() => {
      vi.advanceTimersByTime(30000);
    });
    const afterSecondTick = vi.mocked(sseManager).getConnectionState.mock.calls.length;
    expect(afterSecondTick).toBeGreaterThan(afterFirstTick);
  });

  it("컴포넌트 언마운트 시 리스너와 인터벌을 정리한다", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: {
        memberId: 1,
        settingId: 1,
        socialNickname: "test",
        thumbnailImage: "",
        memberName: "테스트사용자",
        role: "USER",
      },
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      updateUserName: vi.fn(),
      refreshUser: vi.fn(),
    });

    const { unmount } = renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    // 초기 리스너 등록 확인
    expect(vi.mocked(sseManager).addEventListener).toHaveBeenCalled();

    // 언마운트
    unmount();

    // 리스너 제거 확인 (초기 제거 + 언마운트 시 제거)
    expect(vi.mocked(sseManager).removeEventListener).toHaveBeenCalledWith("notification");
    expect(vi.mocked(sseManager).removeEventListener).toHaveBeenCalledTimes(2);
  });

  it("사용자가 로그아웃하면 SSE 연결을 해제한다", async () => {
    const { result, rerender } = renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    // 초기 상태: 로그인
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: {
        memberId: 1,
        settingId: 1,
        socialNickname: "test",
        thumbnailImage: "",
        memberName: "테스트사용자",
        role: "USER",
      },
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      updateUserName: vi.fn(),
      refreshUser: vi.fn(),
    });
    rerender();

    const statusListener = vi.mocked(sseManager).addStatusListener.mock.calls.at(-1)?.[0];
    expect(statusListener).toBeDefined();
    act(() => {
      statusListener?.("connected");
    });

    // 로그아웃 시뮬레이션
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      user: null,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      updateUserName: vi.fn(),
      refreshUser: vi.fn(),
    });

    rerender();

    const latestStatusListener = vi.mocked(sseManager).addStatusListener.mock.calls.at(-1)?.[0];
    expect(latestStatusListener).toBeDefined();
    act(() => {
      latestStatusListener?.("disconnected");
    });

    expect(result.current.isSSEConnected).toBe(false);
    expect(result.current.connectionState).toBe("DISCONNECTED");
    expect(vi.mocked(sseManager).removeEventListener).toHaveBeenCalledWith("notification");
  });

  it("알림 권한이 거부된 경우 브라우저 알림을 표시하지 않는다", () => {
    Object.defineProperty(global.Notification, 'permission', {
      writable: true,
      value: "denied"
    });

    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      user: {
        memberId: 1,
        settingId: 1,
        socialNickname: "test",
        thumbnailImage: "",
        memberName: "테스트사용자",
        role: "USER",
      },
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      updateUserName: vi.fn(),
      refreshUser: vi.fn(),
    });

    renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });

    const eventHandler = vi.mocked(sseManager).addEventListener.mock.calls[0][1];
    const notificationData = {
      id: 1,
      notificationType: "COMMENT" as const,
      content: "새로운 댓글이 달렸습니다",
      url: "/post/123",
      createdAt: new Date().toISOString(),
      read: false,
    };

    // 이벤트 핸들러 실행
    eventHandler(notificationData);

    // Notification이 생성되지 않음
    expect(mockNotification).not.toHaveBeenCalled();
  });
});

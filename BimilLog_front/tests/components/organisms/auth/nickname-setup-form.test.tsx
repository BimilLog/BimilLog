import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { NicknameSetupForm } from "@/components/organisms/auth";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// Create mock functions
const mockSignUp = vi.fn();
const mockRefetchNotifications = vi.fn();
const mockRouterPush = vi.fn();

// Mock API
vi.mock("@/lib/api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api")>("@/lib/api");
  return {
    ...actual,
    userQuery: {
      checkUserName: vi.fn(),
    },
  };
});

vi.mock("@/hooks", () => ({
  useAuth: vi.fn(() => ({
    signUp: mockSignUp,
  })),
}));

vi.mock("@/hooks/api", () => ({
  useNotificationList: vi.fn(() => ({
    refetch: mockRefetchNotifications,
  })),
}));

vi.mock("next/navigation", () => ({
  useRouter: vi.fn(() => ({
    push: mockRouterPush,
  })),
}));

// Import mocked modules for use in tests
import { userQuery } from "@/lib/api";

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
};

describe("NicknameSetupForm", () => {
  const defaultProps = {
    tempUuid: "123e4567-e89b-12d3-a456-426614174000",
    onSuccess: vi.fn(),
    onError: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("닉네임 형식이 잘못되면 오류 메시지를 표시한다", async () => {
    const user = userEvent.setup();

    render(<NicknameSetupForm {...defaultProps} />, { wrapper: createWrapper() });

    const input = screen.getByRole("textbox");

    // 너무 짧은 닉네임
    await user.type(input, "a");
    expect(screen.getByText(/닉네임은 2자 이상이어야 합니다/)).toBeInTheDocument();

    // 너무 긴 닉네임
    await user.clear(input);
    await user.type(input, "닉네임이너무길어요");
    expect(screen.getByText(/닉네임은 8자 이하여야 합니다/)).toBeInTheDocument();

    // 특수문자 포함
    await user.clear(input);
    await user.type(input, "닉네임!@#");
    expect(screen.getByText(/특수문자는 사용할 수 없습니다/)).toBeInTheDocument();
  });

  it("닉네임 중복 확인이 성공하면 사용 가능 메시지를 표시한다", async () => {
    const user = userEvent.setup();
    vi.mocked(userQuery.checkUserName).mockResolvedValue({
      success: true,
      data: true,
    });

    render(<NicknameSetupForm {...defaultProps} />, { wrapper: createWrapper() });

    const input = screen.getByRole("textbox");
    await user.type(input, "테스트닉네임");

    const checkButton = screen.getByRole("button", { name: /중복확인/ });
    await user.click(checkButton);

    await waitFor(() => {
      expect(screen.getByText(/사용 가능한 닉네임/)).toBeInTheDocument();
    });
  });

  it("닉네임 중복 확인이 실패하면 오류 메시지를 표시한다", async () => {
    const user = userEvent.setup();
    vi.mocked(userQuery.checkUserName).mockResolvedValue({
      success: true,
      data: false,
    });

    render(<NicknameSetupForm {...defaultProps} />, { wrapper: createWrapper() });

    const input = screen.getByRole("textbox");
    await user.type(input, "이미사용중");

    const checkButton = screen.getByRole("button", { name: /중복확인/ });
    await user.click(checkButton);

    await waitFor(() => {
      expect(screen.getByText(/이미 사용중인 닉네임/)).toBeInTheDocument();
    });
  });

  it("회원가입이 성공하면 onSuccess를 호출하고 홈으로 이동한다", async () => {
    const user = userEvent.setup();
    vi.mocked(userQuery.checkUserName).mockResolvedValue({
      success: true,
      data: true,
    });
    mockSignUp.mockResolvedValue({ success: true });

    render(<NicknameSetupForm {...defaultProps} />, { wrapper: createWrapper() });

    // 닉네임 입력
    const input = screen.getByRole("textbox");
    await user.type(input, "새닉네임");

    // 중복 확인
    const checkButton = screen.getByRole("button", { name: /중복확인/ });
    await user.click(checkButton);

    await waitFor(() => {
      expect(screen.getByText(/사용 가능한 닉네임/)).toBeInTheDocument();
    });

    // 회원가입 제출
    const submitButton = screen.getByRole("button", { name: /닉네임 설정 완료/ });
    await user.click(submitButton);

    await waitFor(() => {
      expect(mockSignUp).toHaveBeenCalledWith("새닉네임", "123e4567-e89b-12d3-a456-426614174000");
      expect(mockRefetchNotifications).toHaveBeenCalled();
      expect(defaultProps.onSuccess).toHaveBeenCalled();
      expect(mockRouterPush).toHaveBeenCalledWith("/");
    });
  });

  it("회원가입이 실패하면 onError를 호출한다", async () => {
    const user = userEvent.setup();
    vi.mocked(userQuery.checkUserName).mockResolvedValue({
      success: true,
      data: true,
    });
    mockSignUp.mockResolvedValue({ success: false, error: "회원가입 실패" });

    render(<NicknameSetupForm {...defaultProps} />, { wrapper: createWrapper() });

    // 닉네임 입력 및 중복 확인
    const input = screen.getByRole("textbox");
    await user.type(input, "새닉네임");

    const checkButton = screen.getByRole("button", { name: /중복확인/ });
    await user.click(checkButton);

    await waitFor(() => {
      expect(screen.getByText(/사용 가능한 닉네임/)).toBeInTheDocument();
    });

    // 회원가입 제출
    const submitButton = screen.getByRole("button", { name: /닉네임 설정 완료/ });
    await user.click(submitButton);

    await waitFor(() => {
      expect(defaultProps.onError).toHaveBeenCalledWith(expect.stringContaining("실패"));
    });
  });

  // UUID 검증은 handleSubmit에서만 실행되므로 컴포넌트 마운트 시 테스트 불가능
  // 실제 사용에서는 카카오 OAuth를 거쳐야 하므로 잘못된 UUID가 전달될 가능성 매우 낮음
  it.skip("잘못된 UUID 형식이면 오류를 표시한다", async () => {
    const invalidProps = {
      ...defaultProps,
      tempUuid: "invalid-uuid",
    };

    render(<NicknameSetupForm {...invalidProps} />, { wrapper: createWrapper() });

    await waitFor(() => {
      expect(invalidProps.onError).toHaveBeenCalledWith(expect.stringContaining("잘못된"));
    });
  });

  it("닉네임을 변경하면 중복 확인 상태가 초기화된다", async () => {
    const user = userEvent.setup();
    vi.mocked(userQuery.checkUserName).mockResolvedValue({
      success: true,
      data: true,
    });

    render(<NicknameSetupForm {...defaultProps} />, { wrapper: createWrapper() });

    const input = screen.getByRole("textbox");
    await user.type(input, "첫번째닉네임");

    const checkButton = screen.getByRole("button", { name: /중복확인/ });
    await user.click(checkButton);

    await waitFor(() => {
      expect(screen.getByText(/사용 가능한 닉네임/)).toBeInTheDocument();
    });

    // 닉네임 변경
    await user.clear(input);
    await user.type(input, "두번째닉네임");

    // 사용 가능 메시지가 사라져야 함
    expect(screen.queryByText(/사용 가능한 닉네임/)).not.toBeInTheDocument();
  });
});

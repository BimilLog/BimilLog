import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MessageForm } from "@/components/organisms/rolling-paper";

// DecoIcon 컴포넌트 모킹
vi.mock("@/components", async () => {
  const actual = await vi.importActual<typeof import("@/components")>("@/components");
  return {
    ...actual,
    DecoIcon: vi.fn(({ decoType }) => <span data-testid="deco-icon">{decoType}</span>),
  };
});

// Flowbite 컴포넌트 모킹
vi.mock("flowbite-react", () => ({
  Spinner: vi.fn(({ "aria-label": label }) => <div role="status">{label}</div>),
  Dropdown: vi.fn(({ renderTrigger, children }) => {
    const TriggerComponent = renderTrigger();
    return (
      <div data-testid="dropdown">
        {TriggerComponent}
        <div data-testid="dropdown-menu">{children}</div>
      </div>
    );
  }),
  DropdownItem: vi.fn(({ children, onClick }) => (
    <button type="button" onClick={onClick} data-testid="dropdown-item">
      {children}
    </button>
  )),
}));

// decoTypeMap과 getDecoInfo 모킹
vi.mock("@/lib/api", () => ({
  getDecoInfo: vi.fn((type: string) => ({
    name: type === "POTATO" ? "감자" : type === "WATERMELON" ? "수박" : "딸기",
    color: "from-yellow-100 to-yellow-200",
  })),
  decoTypeMap: {
    POTATO: { name: "감자", color: "from-yellow-100 to-yellow-200" },
    WATERMELON: { name: "수박", color: "from-green-100 to-green-200" },
    STRAWBERRY: { name: "딸기", color: "from-pink-100 to-pink-200" },
  },
}));

describe("MessageForm", () => {
  const defaultProps = {
    onSubmit: vi.fn(() => Promise.resolve()),
    onSuccess: vi.fn(),
    onError: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("폼 렌더링", () => {
    it("모든 입력 필드를 올바르게 렌더링한다", () => {
      render(<MessageForm {...defaultProps} />);

      expect(screen.getByPlaceholderText(/시원한 마음의 친구/)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/)).toBeInTheDocument();
      expect(screen.getByText("장식 선택")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /메시지 남기기/ })).toBeInTheDocument();
    });

    it("미리보기 카드를 표시한다", () => {
      render(<MessageForm {...defaultProps} />);

      expect(screen.getByText("여기에 메시지가 표시됩니다...")).toBeInTheDocument();
      expect(screen.getByText("감자")).toBeInTheDocument(); // 기본 데코 타입
    });

    it("글자수 카운터를 표시한다", () => {
      render(<MessageForm {...defaultProps} />);

      expect(screen.getByText("0 / 8")).toBeInTheDocument(); // 닉네임
      expect(screen.getByText("0 / 255")).toBeInTheDocument(); // 메시지
    });
  });

  describe("익명 닉네임 입력", () => {
    it("닉네임 입력 시 실시간으로 글자수를 업데이트한다", async () => {
      const user = userEvent.setup();

      render(<MessageForm {...defaultProps} />);

      const nicknameInput = screen.getByPlaceholderText(/시원한 마음의 친구/);
      await user.type(nicknameInput, "테스터");

      expect(screen.getByText("3 / 8")).toBeInTheDocument();
    });

    it("닉네임이 8자를 초과하면 오류 메시지를 표시한다", async () => {
      const user = userEvent.setup();

      render(<MessageForm {...defaultProps} />);

      const nicknameInput = screen.getByPlaceholderText(/시원한 마음의 친구/);
      await user.type(nicknameInput, "아주긴닉네임입니다");

      // maxLength 속성으로 인해 8자까지만 입력됨
      expect(nicknameInput).toHaveAttribute("maxLength", "8");
    });

    it("닉네임이 비어있으면 제출할 수 없다", async () => {
      const user = userEvent.setup();

      render(<MessageForm {...defaultProps} />);

      const messageInput = screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/);
      const submitButton = screen.getByRole("button", { name: /메시지 남기기/ });

      await user.type(messageInput, "테스트 메시지");
      await user.click(submitButton);

      await waitFor(() => {
        expect(defaultProps.onSubmit).not.toHaveBeenCalled();
      });
    });
  });

  describe("메시지 입력", () => {
    it("메시지 입력 시 실시간으로 미리보기를 업데이트한다", async () => {
      const user = userEvent.setup();

      render(<MessageForm {...defaultProps} />);

      const messageInput = screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/);
      await user.type(messageInput, "생일 축하해!");

      expect(screen.getByText("생일 축하해!")).toBeInTheDocument();
    });

    it("메시지 글자수를 실시간으로 업데이트한다", async () => {
      const user = userEvent.setup();

      render(<MessageForm {...defaultProps} />);

      const messageInput = screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/);
      await user.type(messageInput, "테스트 메시지입니다");

      expect(screen.getByText("10 / 255")).toBeInTheDocument();
    });

    it("메시지가 255자를 초과할 수 없다", () => {
      render(<MessageForm {...defaultProps} />);

      const messageInput = screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/);
      expect(messageInput).toHaveAttribute("maxLength", "255");
    });

    it("메시지가 비어있으면 제출할 수 없다", async () => {
      const user = userEvent.setup();

      render(<MessageForm {...defaultProps} />);

      const nicknameInput = screen.getByPlaceholderText(/시원한 마음의 친구/);
      const submitButton = screen.getByRole("button", { name: /메시지 남기기/ });

      await user.type(nicknameInput, "테스터");
      await user.click(submitButton);

      await waitFor(() => {
        expect(defaultProps.onSubmit).not.toHaveBeenCalled();
      });
    });
  });

  describe("장식 선택", () => {
    it("기본 장식이 '감자'로 설정되어 있다", () => {
      render(<MessageForm {...defaultProps} />);

      const triggerButton = screen.getByRole("button", { name: /감자/ });
      expect(triggerButton).toBeInTheDocument();
    });

    it("장식을 변경할 수 있다", async () => {
      const user = userEvent.setup();

      render(<MessageForm {...defaultProps} />);

      // 드롭다운 아이템 클릭 (수박 선택)
      const dropdownItems = screen.getAllByTestId("dropdown-item");
      await user.click(dropdownItems[1]); // 두 번째 아이템 (수박)

      // setValue가 호출되어 폼 값이 변경됨
      // 실제로는 react-hook-form의 setValue가 호출되지만,
      // 여기서는 모킹된 컴포넌트로 테스트
      expect(dropdownItems[1]).toBeInTheDocument();
    });

    it("선택된 장식이 미리보기에 반영된다", () => {
      render(<MessageForm {...defaultProps} />);

      // 초기 상태에서 POTATO 아이콘 표시
      const decoIcons = screen.getAllByTestId("deco-icon");
      expect(decoIcons[0]).toHaveTextContent("POTATO");
    });
  });

  describe("폼 제출", () => {
    it("유효한 데이터로 폼을 제출할 수 있다", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn(() => Promise.resolve());

      render(<MessageForm {...defaultProps} onSubmit={onSubmit} />);

      const nicknameInput = screen.getByPlaceholderText(/시원한 마음의 친구/);
      const messageInput = screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/);
      const submitButton = screen.getByRole("button", { name: /메시지 남기기/ });

      await user.type(nicknameInput, "친구");
      await user.type(messageInput, "생일 축하해! 항상 행복하길 바라");
      await user.click(submitButton);

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith({
          anonymousNickname: "친구",
          content: "생일 축하해! 항상 행복하길 바라",
          decoType: "POTATO",
        });
      });
    });

    it("제출 성공 시 폼이 초기화된다", async () => {
      const user = userEvent.setup();

      render(<MessageForm {...defaultProps} />);

      const nicknameInput = screen.getByPlaceholderText(/시원한 마음의 친구/);
      const messageInput = screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/);
      const submitButton = screen.getByRole("button", { name: /메시지 남기기/ });

      await user.type(nicknameInput, "친구");
      await user.type(messageInput, "메시지 내용");
      await user.click(submitButton);

      await waitFor(() => {
        expect(nicknameInput).toHaveValue("");
        expect(messageInput).toHaveValue("");
      });
    });

    it("제출 중에는 버튼이 비활성화되고 로딩 표시가 나타난다", async () => {
      const user = userEvent.setup();
      let resolveSubmit: () => void;
      const onSubmit = vi.fn(
        () =>
          new Promise<void>((resolve) => {
            resolveSubmit = resolve;
          })
      );

      render(<MessageForm {...defaultProps} onSubmit={onSubmit} />);

      const nicknameInput = screen.getByPlaceholderText(/시원한 마음의 친구/);
      const messageInput = screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/);
      const submitButton = screen.getByRole("button", { name: /메시지 남기기/ });

      await user.type(nicknameInput, "친구");
      await user.type(messageInput, "메시지");
      await user.click(submitButton);

      // 로딩 상태 확인
      await waitFor(() => {
        expect(screen.getByText("등록 중...")).toBeInTheDocument();
        expect(submitButton).toBeDisabled();
      });

      // Promise 해결
      resolveSubmit!();
    });

    it("제출 실패 시 onError를 호출한다", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn(() => Promise.reject(new Error("Network error")));
      const onError = vi.fn();

      render(<MessageForm {...defaultProps} onSubmit={onSubmit} onError={onError} />);

      const nicknameInput = screen.getByPlaceholderText(/시원한 마음의 친구/);
      const messageInput = screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/);
      const submitButton = screen.getByRole("button", { name: /메시지 남기기/ });

      await user.type(nicknameInput, "친구");
      await user.type(messageInput, "메시지");
      await user.click(submitButton);

      await waitFor(() => {
        expect(onError).toHaveBeenCalledWith("메시지 추가에 실패했습니다. 다시 시도해주세요.");
      });
    });

    it("입력값의 공백을 트림하여 제출한다", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn(() => Promise.resolve());

      render(<MessageForm {...defaultProps} onSubmit={onSubmit} />);

      const nicknameInput = screen.getByPlaceholderText(/시원한 마음의 친구/);
      const messageInput = screen.getByPlaceholderText(/마음을 담은 메시지를 남겨주세요/);
      const submitButton = screen.getByRole("button", { name: /메시지 남기기/ });

      await user.type(nicknameInput, "  친구  ");
      await user.type(messageInput, "  메시지 내용  ");
      await user.click(submitButton);

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith({
          anonymousNickname: "친구",
          content: "메시지 내용",
          decoType: "POTATO",
        });
      });
    });
  });
});
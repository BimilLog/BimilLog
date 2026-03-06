import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { CommentForm } from "@/components/organisms/board";

// LazyEditor 모킹 - CommentForm은 LazyEditor를 사용하므로 textarea로 대체
vi.mock("@/lib/utils/lazy-components", () => ({
  LazyEditor: vi.fn(({ value, onChange }: { value: string; onChange: (v: string) => void }) => (
    <textarea
      data-testid="comment-editor"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder="댓글을 입력하세요"
    />
  )),
}));

// stripHtmlTags 모킹 - 테스트에서는 plain text 그대로 반환
vi.mock("@/lib/utils/sanitize", () => ({
  stripHtmlTags: (html: string) => html,
}));

describe("CommentForm", () => {
  const defaultProps = {
    isAuthenticated: false,
    isSubmittingComment: false,
    onSubmit: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("비로그인 사용자", () => {
    it("댓글과 비밀번호 입력 필드를 모두 표시한다", () => {
      render(<CommentForm {...defaultProps} isAuthenticated={false} />);

      expect(screen.getByTestId("comment-editor")).toBeInTheDocument();
      expect(screen.getByPlaceholderText("비밀번호 (1000~9999)")).toBeInTheDocument();
    });

    it("비밀번호가 4자리 숫자가 아니면 오류 메시지를 표시한다", async () => {
      const user = userEvent.setup();

      render(<CommentForm {...defaultProps} isAuthenticated={false} />);

      const passwordInput = screen.getByPlaceholderText("비밀번호 (1000~9999)");
      const commentEditor = screen.getByTestId("comment-editor");
      const submitButton = screen.getByRole("button", { name: /작성/ });

      await user.type(passwordInput, "123");
      await user.type(commentEditor, "테스트 댓글");
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText("1000~9999 사이의 숫자를 입력해주세요")).toBeInTheDocument();
      });

      expect(defaultProps.onSubmit).not.toHaveBeenCalled();
    });

    it("댓글과 비밀번호가 모두 유효할 때만 제출 버튼이 활성화된다", async () => {
      const user = userEvent.setup();

      render(<CommentForm {...defaultProps} isAuthenticated={false} />);

      const passwordInput = screen.getByPlaceholderText("비밀번호 (1000~9999)");
      const commentEditor = screen.getByTestId("comment-editor");
      const submitButton = screen.getByRole("button", { name: /작성/ });

      expect(submitButton).toBeDisabled();

      await user.type(commentEditor, "테스트 댓글");
      expect(submitButton).toBeDisabled();

      await user.type(passwordInput, "1234");
      expect(submitButton).toBeEnabled();
    });

    it("유효한 폼을 제출하면 onSubmit을 호출하고 폼을 초기화한다", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();

      render(<CommentForm {...defaultProps} isAuthenticated={false} onSubmit={onSubmit} />);

      const passwordInput = screen.getByPlaceholderText("비밀번호 (1000~9999)");
      const commentEditor = screen.getByTestId("comment-editor");
      const submitButton = screen.getByRole("button", { name: /작성/ });

      await user.type(commentEditor, "테스트 댓글입니다");
      await user.type(passwordInput, "5678");
      await user.click(submitButton);

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith("테스트 댓글입니다", "5678");
        expect(commentEditor).toHaveValue("");
        expect(passwordInput).toHaveValue("");
      });
    });

    it("비밀번호는 최대 4자리까지만 입력 가능하다", async () => {
      render(<CommentForm {...defaultProps} isAuthenticated={false} />);

      const passwordInput = screen.getByPlaceholderText("비밀번호 (1000~9999)");
      expect(passwordInput).toHaveAttribute("maxLength", "4");
    });
  });

  describe("로그인 사용자", () => {
    it("비밀번호 필드를 표시하지 않는다", () => {
      render(<CommentForm {...defaultProps} isAuthenticated={true} />);

      expect(screen.getByTestId("comment-editor")).toBeInTheDocument();
      expect(screen.queryByPlaceholderText(/비밀번호/)).not.toBeInTheDocument();
    });

    it("댓글만 입력하면 제출 가능하다", async () => {
      const user = userEvent.setup();

      render(<CommentForm {...defaultProps} isAuthenticated={true} />);

      const commentEditor = screen.getByTestId("comment-editor");
      const submitButton = screen.getByRole("button", { name: /작성/ });

      expect(submitButton).toBeDisabled();

      await user.type(commentEditor, "로그인 사용자 댓글");
      expect(submitButton).toBeEnabled();
    });

    it("댓글 제출 시 비밀번호 없이 onSubmit을 호출한다", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();

      render(<CommentForm {...defaultProps} isAuthenticated={true} onSubmit={onSubmit} />);

      const commentEditor = screen.getByTestId("comment-editor");
      const submitButton = screen.getByRole("button", { name: /작성/ });

      await user.type(commentEditor, "로그인 사용자의 댓글");
      await user.click(submitButton);

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith("로그인 사용자의 댓글", "");
        expect(commentEditor).toHaveValue("");
      });
    });
  });

  describe("공통 기능", () => {
    it("빈 댓글은 제출할 수 없다", async () => {
      render(<CommentForm {...defaultProps} isAuthenticated={true} />);

      const submitButton = screen.getByRole("button", { name: /작성/ });
      expect(submitButton).toBeDisabled();
    });

    it("댓글 제출 중일 때 제출 버튼이 비활성화된다", async () => {
      const user = userEvent.setup();

      render(
        <CommentForm
          {...defaultProps}
          isAuthenticated={true}
          isSubmittingComment={true}
        />
      );

      const commentEditor = screen.getByTestId("comment-editor");
      const submitButton = screen.getByRole("button", { name: /작성/ });

      await user.type(commentEditor, "테스트 댓글");
      expect(submitButton).toBeDisabled();
    });

    it("댓글 작성 카드 헤더가 올바르게 표시된다", () => {
      render(<CommentForm {...defaultProps} />);

      expect(screen.getByText("댓글 작성")).toBeInTheDocument();
    });

    it("제출 버튼에 Send 아이콘이 포함된다", () => {
      render(<CommentForm {...defaultProps} />);

      const submitButton = screen.getByRole("button", { name: /작성/ });
      const sendIcon = submitButton.querySelector("svg");

      expect(sendIcon).toBeInTheDocument();
      expect(sendIcon).toHaveClass("stroke-blue-600", "fill-blue-100");
    });

    it("폼 제출 후 입력 필드가 초기화된다", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();

      render(
        <CommentForm
          {...defaultProps}
          isAuthenticated={false}
          onSubmit={onSubmit}
        />
      );

      const passwordInput = screen.getByPlaceholderText("비밀번호 (1000~9999)");
      const commentEditor = screen.getByTestId("comment-editor");
      const submitButton = screen.getByRole("button", { name: /작성/ });

      await user.type(commentEditor, "테스트 댓글");
      await user.type(passwordInput, "1234");
      await user.click(submitButton);

      await waitFor(() => {
        expect(commentEditor).toHaveValue("");
        expect(passwordInput).toHaveValue("");
      });
    });

    it("다양한 스타일로 댓글을 꾸며보세요 텍스트가 표시된다", () => {
      render(<CommentForm {...defaultProps} />);

      expect(screen.getByText("다양한 스타일로 댓글을 꾸며보세요.")).toBeInTheDocument();
    });
  });
});

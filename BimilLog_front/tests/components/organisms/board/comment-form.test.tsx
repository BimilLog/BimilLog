import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { CommentForm } from "@/components/organisms/board";

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

      expect(screen.getByPlaceholderText(/댓글을 입력하세요/)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/비밀번호.*4자리 숫자/)).toBeInTheDocument();
    });

    it("비밀번호가 4자리 숫자가 아니면 오류 메시지를 표시한다", async () => {
      const user = userEvent.setup();

      render(<CommentForm {...defaultProps} isAuthenticated={false} />);

      const passwordInput = screen.getByPlaceholderText(/비밀번호.*4자리 숫자/);
      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);
      const submitButton = screen.getByRole("button");

      // 잘못된 비밀번호 입력
      await user.type(passwordInput, "abc");
      await user.type(commentInput, "테스트 댓글");
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText("4자리 숫자를 입력해주세요")).toBeInTheDocument();
      });

      expect(defaultProps.onSubmit).not.toHaveBeenCalled();
    });

    it("댓글과 비밀번호가 모두 유효할 때만 제출 버튼이 활성화된다", async () => {
      const user = userEvent.setup();

      render(<CommentForm {...defaultProps} isAuthenticated={false} />);

      const passwordInput = screen.getByPlaceholderText(/비밀번호.*4자리 숫자/);
      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);
      const submitButton = screen.getByRole("button");

      // 초기 상태: 버튼 비활성화
      expect(submitButton).toBeDisabled();

      // 댓글만 입력: 버튼 여전히 비활성화
      await user.type(commentInput, "테스트 댓글");
      expect(submitButton).toBeDisabled();

      // 비밀번호도 입력: 버튼 활성화
      await user.type(passwordInput, "1234");
      expect(submitButton).toBeEnabled();
    });

    it("유효한 폼을 제출하면 onSubmit을 호출하고 폼을 초기화한다", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();

      render(<CommentForm {...defaultProps} isAuthenticated={false} onSubmit={onSubmit} />);

      const passwordInput = screen.getByPlaceholderText(/비밀번호.*4자리 숫자/);
      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);
      const submitButton = screen.getByRole("button");

      await user.type(commentInput, "테스트 댓글입니다");
      await user.type(passwordInput, "5678");
      await user.click(submitButton);

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith("테스트 댓글입니다", "5678");
        expect(commentInput).toHaveValue("");
        expect(passwordInput).toHaveValue("");
      });
    });

    it("비밀번호는 최대 4자리까지만 입력 가능하다", async () => {
      const user = userEvent.setup();

      render(<CommentForm {...defaultProps} isAuthenticated={false} />);

      const passwordInput = screen.getByPlaceholderText(/비밀번호.*4자리 숫자/);
      await user.type(passwordInput, "123456");

      expect(passwordInput).toHaveAttribute("maxLength", "4");
    });
  });

  describe("로그인 사용자", () => {
    it("비밀번호 필드를 표시하지 않는다", () => {
      render(<CommentForm {...defaultProps} isAuthenticated={true} />);

      expect(screen.getByPlaceholderText(/댓글을 입력하세요/)).toBeInTheDocument();
      expect(screen.queryByPlaceholderText(/비밀번호/)).not.toBeInTheDocument();
    });

    it("댓글만 입력하면 제출 가능하다", async () => {
      const user = userEvent.setup();

      render(<CommentForm {...defaultProps} isAuthenticated={true} />);

      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);
      const submitButton = screen.getByRole("button");

      // 초기 상태: 버튼 비활성화
      expect(submitButton).toBeDisabled();

      // 댓글 입력: 버튼 활성화
      await user.type(commentInput, "로그인 사용자 댓글");
      expect(submitButton).toBeEnabled();
    });

    it("댓글 제출 시 비밀번호 없이 onSubmit을 호출한다", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();

      render(<CommentForm {...defaultProps} isAuthenticated={true} onSubmit={onSubmit} />);

      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);
      const submitButton = screen.getByRole("button");

      await user.type(commentInput, "로그인 사용자의 댓글");
      await user.click(submitButton);

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith("로그인 사용자의 댓글", "");
        expect(commentInput).toHaveValue("");
      });
    });
  });

  describe("공통 기능", () => {
    it("빈 댓글은 제출할 수 없다", async () => {
      const user = userEvent.setup();

      render(<CommentForm {...defaultProps} isAuthenticated={true} />);

      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);
      const submitButton = screen.getByRole("button");

      // 공백만 입력
      await user.type(commentInput, "   ");
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

      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);
      const submitButton = screen.getByRole("button");

      await user.type(commentInput, "테스트 댓글");

      // isSubmittingComment가 true이므로 버튼이 비활성화
      expect(submitButton).toBeDisabled();
    });

    it("댓글 필드가 필수 입력 필드임을 표시한다", async () => {
      const user = userEvent.setup();

      render(<CommentForm {...defaultProps} isAuthenticated={true} />);

      const submitButton = screen.getByRole("button");
      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);

      // 빈 상태로 제출 시도
      await user.click(submitButton);

      // HTML5 validation이나 react-hook-form의 필수 필드 검증
      expect(commentInput).toBeRequired();
    });

    it("댓글 작성 카드 헤더가 올바르게 표시된다", () => {
      render(<CommentForm {...defaultProps} />);

      expect(screen.getByText("댓글 작성")).toBeInTheDocument();
    });

    it("제출 버튼에 Send 아이콘이 포함된다", () => {
      render(<CommentForm {...defaultProps} />);

      const submitButton = screen.getByRole("button");
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

      const passwordInput = screen.getByPlaceholderText(/비밀번호.*4자리 숫자/);
      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);
      const submitButton = screen.getByRole("button");

      // 폼 작성 및 제출
      await user.type(commentInput, "테스트 댓글");
      await user.type(passwordInput, "1234");
      await user.click(submitButton);

      await waitFor(() => {
        // 모든 입력 필드가 비워졌는지 확인
        expect(commentInput).toHaveValue("");
        expect(passwordInput).toHaveValue("");
      });
    });

    it("Enter 키로 댓글을 제출할 수 있다", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();

      render(
        <CommentForm
          {...defaultProps}
          isAuthenticated={true}
          onSubmit={onSubmit}
        />
      );

      const commentInput = screen.getByPlaceholderText(/댓글을 입력하세요/);

      await user.type(commentInput, "엔터로 제출하는 댓글");
      await user.keyboard("{Enter}");

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith("엔터로 제출하는 댓글", "");
      });
    });
  });
});
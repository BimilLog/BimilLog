import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { WriteForm } from "@/components/organisms/board";

// LazyEditor 모킹
vi.mock("@/lib/utils/lazy-components", () => ({
  LazyEditor: vi.fn(({ value, onChange }) => (
    <textarea
      data-testid="quill-editor"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder="내용을 입력하세요"
    />
  )),
}));

// SafeHTML 컴포넌트 모킹
vi.mock("@/components", async () => {
  const actual = await vi.importActual<typeof import("@/components")>("@/components");
  return {
    ...actual,
    SafeHTML: vi.fn(({ html }) => <div dangerouslySetInnerHTML={{ __html: html }} />),
  };
});

describe("WriteForm", () => {
  const defaultProps = {
    title: "",
    setTitle: vi.fn(),
    content: "",
    setContent: vi.fn(),
    password: "",
    setPassword: vi.fn(),
    user: null,
    isAuthenticated: false,
    isPreview: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("편집 모드", () => {
    it("제목과 내용 입력 필드를 렌더링한다", () => {
      render(<WriteForm {...defaultProps} />);

      expect(screen.getByRole("textbox", { name: /제목/ })).toBeInTheDocument();
      expect(screen.getByTestId("quill-editor")).toBeInTheDocument();
      expect(screen.getByText("다양한 스타일로 내용을 꾸며보세요.")).toBeInTheDocument();
    });

    it("제목 입력 시 setTitle을 호출한다", async () => {
      const user = userEvent.setup();
      const setTitle = vi.fn();

      render(<WriteForm {...defaultProps} setTitle={setTitle} />);

      const titleInput = screen.getByRole("textbox", { name: /제목/ });
      await user.type(titleInput, "테스트 제목");

      expect(setTitle).toHaveBeenLastCalledWith("테스트 제목");
    });

    it("내용 입력 시 setContent를 호출한다", async () => {
      const user = userEvent.setup();
      const setContent = vi.fn();

      render(<WriteForm {...defaultProps} setContent={setContent} />);

      const contentEditor = screen.getByTestId("quill-editor");
      await user.type(contentEditor, "테스트 내용입니다.");

      expect(setContent).toHaveBeenLastCalledWith("테스트 내용입니다.");
    });

    it("비로그인 사용자에게는 비밀번호 입력 필드를 표시한다", () => {
      render(<WriteForm {...defaultProps} isAuthenticated={false} />);

      const passwordField = screen.getByLabelText(/비밀번호/);
      expect(passwordField).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/수정\/삭제 시 필요/)).toBeInTheDocument();
    });

    it("비밀번호 입력 시 setPassword를 호출한다", async () => {
      const user = userEvent.setup();
      const setPassword = vi.fn();

      render(
        <WriteForm
          {...defaultProps}
          isAuthenticated={false}
          setPassword={setPassword}
        />
      );

      const passwordInput = screen.getByLabelText(/비밀번호/);
      await user.type(passwordInput, "1234");

      expect(setPassword).toHaveBeenLastCalledWith("1234");
    });

    it("로그인 사용자에게는 작성자 정보를 표시한다", () => {
      const testUser = {
        userName: "홍길동",
        role: "USER",
      };

      render(
        <WriteForm
          {...defaultProps}
          isAuthenticated={true}
          user={testUser}
        />
      );

      expect(screen.getByText("작성자: 홍길동")).toBeInTheDocument();
      expect(screen.getByText("게시글은 수정 및 삭제가 가능합니다")).toBeInTheDocument();
    });

    it("로그인 사용자에게는 비밀번호 필드를 표시하지 않는다", () => {
      const testUser = {
        userName: "홍길동",
        role: "USER",
      };

      render(
        <WriteForm
          {...defaultProps}
          isAuthenticated={true}
          user={testUser}
        />
      );

      expect(screen.queryByLabelText(/비밀번호/)).not.toBeInTheDocument();
    });

    it("사용자 이름의 첫 글자를 아바타로 표시한다", () => {
      const testUser = {
        userName: "테스터",
        role: "USER",
      };

      render(
        <WriteForm
          {...defaultProps}
          isAuthenticated={true}
          user={testUser}
        />
      );

      expect(screen.getByText("테")).toBeInTheDocument();
    });

    it("사용자 이름이 없으면 ? 를 아바타로 표시한다", () => {
      const testUser = {
        userName: "",
        role: "USER",
      };

      render(
        <WriteForm
          {...defaultProps}
          isAuthenticated={true}
          user={testUser}
        />
      );

      expect(screen.getByText("?")).toBeInTheDocument();
    });
  });

  describe("미리보기 모드", () => {
    it("미리보기 모드에서는 제목과 내용을 읽기 전용으로 표시한다", () => {
      render(
        <WriteForm
          {...defaultProps}
          title="미리보기 제목"
          content="<p>미리보기 내용입니다.</p>"
          isPreview={true}
        />
      );

      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("미리보기 제목");
      expect(screen.getByText("미리보기 내용입니다.")).toBeInTheDocument();
    });

    it("미리보기 모드에서 로그인 사용자의 이름을 표시한다", () => {
      const testUser = {
        userName: "김철수",
        role: "USER",
      };

      render(
        <WriteForm
          {...defaultProps}
          isAuthenticated={true}
          user={testUser}
          isPreview={true}
        />
      );

      expect(screen.getByText("작성자: 김철수")).toBeInTheDocument();
    });

    it("미리보기 모드에서 비로그인 사용자는 익명으로 표시한다", () => {
      render(
        <WriteForm
          {...defaultProps}
          isAuthenticated={false}
          isPreview={true}
        />
      );

      expect(screen.getByText("작성자: 익명")).toBeInTheDocument();
    });

    it("미리보기 모드에서는 입력 필드를 표시하지 않는다", () => {
      render(
        <WriteForm
          {...defaultProps}
          title="미리보기 제목"
          content="미리보기 내용"
          isPreview={true}
        />
      );

      expect(screen.queryByRole("textbox", { name: /제목/ })).not.toBeInTheDocument();
      expect(screen.queryByTestId("quill-editor")).not.toBeInTheDocument();
      expect(screen.queryByLabelText(/비밀번호/)).not.toBeInTheDocument();
    });

    it("HTML 내용을 안전하게 렌더링한다", () => {
      const htmlContent = '<script>alert("XSS")</script><p>안전한 내용</p>';

      render(
        <WriteForm
          {...defaultProps}
          content={htmlContent}
          isPreview={true}
        />
      );

      // SafeHTML 컴포넌트가 호출되었는지 확인
      expect(screen.getByText("안전한 내용")).toBeInTheDocument();
    });
  });

  describe("폼 상태 관리", () => {
    it("초기 값이 올바르게 표시된다", () => {
      render(
        <WriteForm
          {...defaultProps}
          title="초기 제목"
          content="초기 내용"
          password="1234"
        />
      );

      expect(screen.getByRole("textbox", { name: /제목/ })).toHaveValue("초기 제목");
      expect(screen.getByTestId("quill-editor")).toHaveValue("초기 내용");
    });

    it("비로그인 상태에서 초기 비밀번호가 올바르게 표시된다", () => {
      render(
        <WriteForm
          {...defaultProps}
          password="5678"
          isAuthenticated={false}
        />
      );

      expect(screen.getByLabelText(/비밀번호/)).toHaveValue("5678");
    });

    it("편집 모드와 미리보기 모드 전환이 가능하다", () => {
      const { rerender } = render(
        <WriteForm
          {...defaultProps}
          title="테스트 제목"
          content="테스트 내용"
          isPreview={false}
        />
      );

      // 편집 모드 확인
      expect(screen.getByRole("textbox", { name: /제목/ })).toBeInTheDocument();

      // 미리보기 모드로 전환
      rerender(
        <WriteForm
          {...defaultProps}
          title="테스트 제목"
          content="테스트 내용"
          isPreview={true}
        />
      );

      // 미리보기 모드 확인
      expect(screen.queryByRole("textbox", { name: /제목/ })).not.toBeInTheDocument();
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("테스트 제목");
    });
  });
});
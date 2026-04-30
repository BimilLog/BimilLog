"use client";

import React, { Component, type ErrorInfo, type ReactNode } from "react";
import { ErrorFallback } from "./error-fallback";
import { errorLogger } from "@/lib/error-logger";

const MAX_RESET_COUNT = 3;
const RESET_COUNTER_RESET_MS = 30_000; // 30초 동안 같은 에러 반복 시 카운트 누적

interface ErrorBoundaryProps {
  children: ReactNode;
  /** 커스텀 에러 UI를 렌더링할 컴포넌트 */
  fallback?: ReactNode | ((props: ErrorFallbackProps) => ReactNode);
  /** 에러 발생 시 호출되는 콜백 */
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
  /** 에러 발생 컨텍스트 (로깅용) */
  context?: string;
}

export interface ErrorFallbackProps {
  error: Error;
  resetErrorBoundary: () => void;
  /** 무한 루프 가드: true 면 재시도 버튼 비활성화 */
  retryDisabled?: boolean;
  /** 현재까지 reset 시도 횟수 */
  resetCount?: number;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  /** 사용자가 누른 누적 reset 횟수 */
  resetCount: number;
}

/**
 * React Error Boundary 컴포넌트
 *
 * 하위 컴포넌트 트리에서 발생하는 렌더링 에러를 잡아
 * 앱 전체가 크래시되는 것을 방지합니다.
 *
 * 라운드 13 — 무한 루프 가드:
 *   `resetErrorBoundary` 를 3회 누른 뒤에도 동일 에러가 throw 되면
 *   `retryDisabled=true` 를 fallback 에 전달해 사용자가 무한히
 *   같은 동작을 반복하지 않도록 합니다 (F-13-BUG-9).
 */
export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  private resetCounterTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null, resetCount: 0 };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // 에러 로깅 (백엔드 전송)
    errorLogger.logError(error, {
      componentStack: errorInfo.componentStack ?? undefined,
      context: this.props.context,
      type: "ErrorBoundary",
      resetCount: this.state.resetCount,
    });

    console.error(
      `[ErrorBoundary${this.props.context ? `:${this.props.context}` : ""}]`,
      error,
      errorInfo
    );

    // 커스텀 onError 콜백
    this.props.onError?.(error, errorInfo);
  }

  componentWillUnmount(): void {
    if (this.resetCounterTimer) {
      clearTimeout(this.resetCounterTimer);
      this.resetCounterTimer = null;
    }
  }

  resetErrorBoundary = (): void => {
    // 무한 루프 가드: MAX 도달 시 재시도 무시
    if (this.state.resetCount >= MAX_RESET_COUNT) {
      return;
    }

    // 30초 안에 다시 throw 되면 카운트 누적 / 30초 무사 통과 시 0 으로 복귀
    if (this.resetCounterTimer) {
      clearTimeout(this.resetCounterTimer);
    }
    this.resetCounterTimer = setTimeout(() => {
      this.setState({ resetCount: 0 });
      this.resetCounterTimer = null;
    }, RESET_COUNTER_RESET_MS);

    this.setState((prev) => ({
      hasError: false,
      error: null,
      resetCount: prev.resetCount + 1,
    }));
  };

  render(): ReactNode {
    if (this.state.hasError && this.state.error) {
      const { fallback } = this.props;
      const retryDisabled = this.state.resetCount >= MAX_RESET_COUNT;
      const fallbackProps: ErrorFallbackProps = {
        error: this.state.error,
        resetErrorBoundary: this.resetErrorBoundary,
        retryDisabled,
        resetCount: this.state.resetCount,
      };

      // render prop 패턴
      if (typeof fallback === "function") {
        return fallback(fallbackProps);
      }

      // ReactNode fallback
      if (fallback) {
        return fallback;
      }

      // 기본 ErrorFallback
      return (
        <ErrorFallback
          error={this.state.error}
          resetErrorBoundary={this.resetErrorBoundary}
          retryDisabled={retryDisabled}
          resetCount={this.state.resetCount}
        />
      );
    }

    return this.props.children;
  }
}

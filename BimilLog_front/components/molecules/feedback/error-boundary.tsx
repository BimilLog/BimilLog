"use client";

import React, { Component, type ErrorInfo, type ReactNode } from "react";
import { ErrorFallback } from "./error-fallback";
import { errorLogger } from "@/lib/error-logger";

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
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/**
 * React Error Boundary 컴포넌트
 *
 * 하위 컴포넌트 트리에서 발생하는 렌더링 에러를 잡아
 * 앱 전체가 크래시되는 것을 방지합니다.
 *
 * @example
 * // 기본 사용
 * <ErrorBoundary>
 *   <MyComponent />
 * </ErrorBoundary>
 *
 * @example
 * // 커스텀 fallback
 * <ErrorBoundary fallback={<div>오류 발생</div>}>
 *   <MyComponent />
 * </ErrorBoundary>
 *
 * @example
 * // render prop 패턴
 * <ErrorBoundary fallback={({ error, resetErrorBoundary }) => (
 *   <div>
 *     <p>{error.message}</p>
 *     <button onClick={resetErrorBoundary}>다시 시도</button>
 *   </div>
 * )}>
 *   <MyComponent />
 * </ErrorBoundary>
 */
export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // 에러 로깅 (백엔드 전송)
    errorLogger.logError(error, {
      componentStack: errorInfo.componentStack ?? undefined,
      context: this.props.context,
      type: "ErrorBoundary",
    });

    console.error(
      `[ErrorBoundary${this.props.context ? `:${this.props.context}` : ""}]`,
      error,
      errorInfo
    );

    // 커스텀 onError 콜백
    this.props.onError?.(error, errorInfo);
  }

  resetErrorBoundary = (): void => {
    this.setState({ hasError: false, error: null });
  };

  render(): ReactNode {
    if (this.state.hasError && this.state.error) {
      const { fallback } = this.props;
      const fallbackProps: ErrorFallbackProps = {
        error: this.state.error,
        resetErrorBoundary: this.resetErrorBoundary,
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
        />
      );
    }

    return this.props.children;
  }
}

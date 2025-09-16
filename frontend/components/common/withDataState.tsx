"use client";

import React, { ComponentType } from 'react';
import { Loading } from '@/components/molecules/feedback/loading';
import { EmptyState } from '@/components/molecules/feedback/empty-state';

/**
 * 데이터 상태 관리를 위한 공통 인터페이스
 */
interface DataStateProps {
  isLoading?: boolean;
  isError?: boolean;
  error?: Error | null;
  isEmpty?: boolean;
  hasData?: boolean;
}

/**
 * Loading/Error/Empty 상태를 자동으로 처리하는 HOC 옵션
 */
interface WithDataStateOptions {
  loadingComponent?: ComponentType;
  errorComponent?: ComponentType<{ error: Error }>;
  emptyComponent?: ComponentType;
  showLoadingOnError?: boolean;
  loadingType?: "default" | "page" | "card" | "button";
  emptyMessage?: string;
  loadingMessage?: string;
}

/**
 * HOC: 데이터 상태에 따른 조건부 렌더링 자동화
 */
export function withDataState<P extends DataStateProps>(
  WrappedComponent: ComponentType<P>,
  options: WithDataStateOptions = {}
) {
  const {
    loadingComponent: LoadingComponent,
    errorComponent: ErrorComponent,
    emptyComponent: EmptyComponent,
    showLoadingOnError = false,
    loadingType = "default",
    emptyMessage = "데이터가 없습니다.",
    loadingMessage = "로딩 중..."
  } = options;

  return function WithDataStateComponent(props: P) {
    const {
      isLoading = false,
      isError = false,
      error = null,
      isEmpty = false,
      hasData = false,
      ...restProps
    } = props;

    // 로딩 상태 (에러 상태에서도 로딩 표시할지 옵션으로 결정)
    if (isLoading && (!isError || showLoadingOnError)) {
      if (LoadingComponent) {
        return <LoadingComponent />;
      }
      return <Loading type={loadingType} message={loadingMessage} />;
    }

    // 에러 상태
    if (isError && error) {
      if (ErrorComponent) {
        return <ErrorComponent error={error} />;
      }
      return (
        <EmptyState
          type="error"
          title="오류가 발생했습니다"
          description={error.message || "잠시 후 다시 시도해주세요."}
          onRetry={() => window.location.reload()}
          showRetry={true}
        />
      );
    }

    // 빈 데이터 상태
    if (isEmpty || (!hasData && !isLoading)) {
      if (EmptyComponent) {
        return <EmptyComponent />;
      }
      return <EmptyState type="custom" title={emptyMessage || "내용이 없어요"} />;
    }

    // 정상 상태에서 컴포넌트 렌더링
    return <WrappedComponent {...(restProps as P)} />;
  };
}

/**
 * 페이지 전체를 위한 데이터 상태 HOC
 */
export function withPageDataState<P extends DataStateProps>(
  WrappedComponent: ComponentType<P>,
  options: Omit<WithDataStateOptions, 'loadingType'> = {}
) {
  return withDataState(WrappedComponent, {
    ...options,
    loadingType: "page"
  });
}

/**
 * 카드형 컨테이너를 위한 데이터 상태 HOC
 */
export function withCardDataState<P extends DataStateProps>(
  WrappedComponent: ComponentType<P>,
  options: Omit<WithDataStateOptions, 'loadingType'> = {}
) {
  return withDataState(WrappedComponent, {
    ...options,
    loadingType: "card"
  });
}

/**
 * 리스트 컴포넌트용 특화 HOC
 */
export function withListDataState<P extends DataStateProps & { data?: unknown[] }>(
  WrappedComponent: ComponentType<P>,
  emptyMessage?: string
) {
  return withDataState(WrappedComponent, {
    emptyMessage: emptyMessage || "목록이 비어있습니다.",
    loadingType: "default"
  });
}

/**
 * Render Props 패턴으로 데이터 상태 관리
 */
interface DataStateRenderProps extends DataStateProps {
  children: (state: {
    isReady: boolean;
    shouldRender: boolean;
  }) => React.ReactNode;
}

export function DataStateProvider({
  children,
  isLoading = false,
  isError = false,
  error = null,
  isEmpty = false,
  hasData = false
}: DataStateRenderProps) {
  const isReady = !isLoading && !isError;
  const shouldRender = isReady && hasData && !isEmpty;

  // 로딩 상태
  if (isLoading) {
    return <Loading type="default" />;
  }

  // 에러 상태
  if (isError && error) {
    return (
      <EmptyState
        type="error"
        title="오류가 발생했습니다"
        description={error.message || "잠시 후 다시 시도해주세요."}
        onRetry={() => window.location.reload()}
        showRetry={true}
      />
    );
  }

  // 빈 상태
  if (isEmpty || (!hasData && !isLoading)) {
    return <EmptyState type="custom" title="데이터가 없습니다." />;
  }

  // Render Props로 상태 전달
  return <>{children({ isReady, shouldRender })}</>;
}

/**
 * Hook: 조건부 렌더링 로직 재사용
 */
export function useConditionalRender(dataState: DataStateProps) {
  const {
    isLoading = false,
    isError = false,
    error = null,
    isEmpty = false,
    hasData = false
  } = dataState;

  const renderLoading = (component?: ComponentType, type: string = "default") => {
    if (!isLoading) return null;

    if (component) {
      const LoadingComponent = component;
      return <LoadingComponent />;
    }

    return <Loading type={type as "default" | "page" | "button" | "card"} />;
  };

  const renderError = (component?: ComponentType<{ error: Error }>) => {
    if (!isError || !error) return null;

    if (component) {
      const ErrorComponent = component;
      return <ErrorComponent error={error} />;
    }

    return (
      <EmptyState
        type="error"
        title="오류가 발생했습니다"
        description={error.message || "잠시 후 다시 시도해주세요."}
        onRetry={() => window.location.reload()}
        showRetry={true}
      />
    );
  };

  const renderEmpty = (component?: ComponentType, message?: string) => {
    if (!isEmpty && hasData) return null;

    if (component) {
      const EmptyComponent = component;
      return <EmptyComponent />;
    }

    return <EmptyState type="custom" title={message || "데이터가 없습니다."} />;
  };

  const shouldRenderContent = !isLoading && !isError && hasData && !isEmpty;

  return {
    renderLoading,
    renderError,
    renderEmpty,
    shouldRenderContent,
    isReady: !isLoading && !isError
  };
}
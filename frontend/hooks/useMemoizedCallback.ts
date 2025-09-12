import { useCallback, useRef, DependencyList } from 'react';

/**
 * 안정적인 함수 참조를 유지하면서 의존성이 변경될 때만 함수를 재생성하는 훅
 * useCallback과 유사하지만 더 안정적인 참조를 제공
 */
export function useMemoizedCallback<T extends (...args: any[]) => any>(
  callback: T,
  deps: DependencyList
): T {
  const callbackRef = useRef<T>(callback);
  const memoizedCallback = useRef<T>();

  callbackRef.current = callback;

  if (!memoizedCallback.current) {
    memoizedCallback.current = ((...args) => callbackRef.current(...args)) as T;
  }

  return useCallback(memoizedCallback.current, deps);
}

/**
 * 비용이 큰 계산을 메모이제이션하는 훅
 * 의존성이 변경될 때만 재계산
 */
export function useExpensiveComputation<T>(
  computeFn: () => T,
  deps: DependencyList,
  shouldRecompute?: (prevDeps: DependencyList, nextDeps: DependencyList) => boolean
): T {
  const resultRef = useRef<T>();
  const depsRef = useRef<DependencyList>();

  const needsRecompute = () => {
    if (!depsRef.current) return true;
    if (shouldRecompute) {
      return shouldRecompute(depsRef.current, deps);
    }
    return deps.some((dep, i) => dep !== depsRef.current![i]);
  };

  if (needsRecompute()) {
    resultRef.current = computeFn();
    depsRef.current = deps;
  }

  return resultRef.current!;
}
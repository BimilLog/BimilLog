import React, { memo } from 'react';

// Simple deep equality check
function deepEqual<T = unknown>(obj1: T, obj2: T): boolean {
  if (obj1 === obj2) return true;

  if (obj1 == null || obj2 == null) return false;
  if (typeof obj1 !== typeof obj2) return false;

  if (typeof obj1 !== 'object') return obj1 === obj2;

  const keys1 = Object.keys(obj1 as Record<string, unknown>);
  const keys2 = Object.keys(obj2 as Record<string, unknown>);

  if (keys1.length !== keys2.length) return false;

  for (const key of keys1) {
    if (!keys2.includes(key)) return false;
    if (!deepEqual(
      (obj1 as Record<string, unknown>)[key],
      (obj2 as Record<string, unknown>)[key]
    )) return false;
  }

  return true;
}

// Deep comparison memo wrapper
export function withDeepMemo<P extends object>(
  Component: React.ComponentType<P>,
  propsAreEqual?: (prevProps: P, nextProps: P) => boolean
) {
  return memo(Component, propsAreEqual || deepEqual);
}

// Shallow comparison memo wrapper with specific props to ignore
export function withSelectiveMemo<P extends object>(
  Component: React.ComponentType<P>,
  keysToCompare?: (keyof P)[],
  keysToIgnore?: (keyof P)[]
) {
  return memo(Component, (prevProps: P, nextProps: P) => {
    if (keysToCompare) {
      return keysToCompare.every(key => prevProps[key] === nextProps[key]);
    }
    
    if (keysToIgnore) {
      const filteredPrevProps = { ...prevProps };
      const filteredNextProps = { ...nextProps };
      
      keysToIgnore.forEach(key => {
        delete filteredPrevProps[key];
        delete filteredNextProps[key];
      });
      
      return deepEqual(filteredPrevProps, filteredNextProps);
    }
    
    return deepEqual(prevProps, nextProps);
  });
}

// Performance monitoring wrapper
export function withPerformanceMonitor<P extends object>(
  Component: React.ComponentType<P>,
  componentName: string
) {
  return (props: P) => {
    const renderStartTime = performance.now();
    
    React.useEffect(() => {
      const renderEndTime = performance.now();
      const renderTime = renderEndTime - renderStartTime;
      
      if (renderTime > 16) { // More than one frame (60fps)
        console.warn(`Slow render detected in ${componentName}: ${renderTime.toFixed(2)}ms`);
      }
    });
    
    return <Component {...props} />;
  };
}
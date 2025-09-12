import React, { memo } from 'react';
import { isEqual } from 'lodash-es';

// Deep comparison memo wrapper
export function withDeepMemo<P extends object>(
  Component: React.ComponentType<P>,
  propsAreEqual?: (prevProps: P, nextProps: P) => boolean
) {
  return memo(Component, propsAreEqual || isEqual);
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
      
      return isEqual(filteredPrevProps, filteredNextProps);
    }
    
    return isEqual(prevProps, nextProps);
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
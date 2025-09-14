"use client";

import { useEffect } from "react";

// Web Vitals 타입 정의
interface WebVitalsMetric {
  id: string;
  name: "CLS" | "FID" | "FCP" | "LCP" | "TTFB" | "INP";
  value: number;
  rating: "good" | "needs-improvement" | "poor";
  delta: number;
  entries: PerformanceEntry[];
  navigationType: "navigate" | "reload" | "back-forward" | "back-forward-cache" | "prerender" | "restore";
}

// Google Analytics로 Web Vitals 전송
function sendToGoogleAnalytics(metric: WebVitalsMetric) {
  // gtag이 로드되었는지 확인
  if (typeof window !== 'undefined' && (window as any).gtag) {
    (window as any).gtag('event', metric.name, {
      event_category: 'Web Vitals',
      event_label: metric.id,
      value: Math.round(metric.name === 'CLS' ? metric.value * 1000 : metric.value),
      custom_map: { metric_rating: metric.rating }
    });
  }
}

// 콘솔에 Web Vitals 로그 출력 (개발 환경)
function logWebVital(metric: WebVitalsMetric) {
  if (process.env.NODE_ENV === 'development') {
    console.group(`🔍 Web Vital: ${metric.name}`);
    console.log(`📊 Value: ${metric.value.toFixed(2)}`);
    console.log(`⭐ Rating: ${metric.rating}`);
    console.log(`🔄 Delta: ${metric.delta.toFixed(2)}`);
    console.log(`🆔 ID: ${metric.id}`);
    console.groupEnd();
  }
}

// 사용자 정의 이벤트로 Web Vitals 전송 (향후 확장성)
function sendToCustomAnalytics(metric: WebVitalsMetric) {
  // 향후 사용자 정의 분석 시스템이나 다른 서비스로 전송할 수 있음
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('web-vital', {
      detail: metric
    }));
  }
}

export function WebVitalsReporter() {
  useEffect(() => {
    // Web Vitals 측정 시작
    async function measureWebVitals() {
      try {
        // web-vitals 라이브러리를 동적으로 import (번들 크기 최적화)
        const webVitals = await import('web-vitals');

        // 각 Web Vital 메트릭에 대해 콜백 등록
        const reportWebVital = (metric: any) => {
          // 개발 환경에서 콘솔 출력
          logWebVital(metric);

          // Google Analytics로 전송
          sendToGoogleAnalytics(metric);

          // 사용자 정의 분석으로 전송
          sendToCustomAnalytics(metric);
        };

        // Core Web Vitals 측정
        webVitals.onCLS(reportWebVital); // Cumulative Layout Shift
        webVitals.onLCP(reportWebVital); // Largest Contentful Paint

        // Other Web Vitals 측정
        webVitals.onFCP(reportWebVital); // First Contentful Paint
        webVitals.onTTFB(reportWebVital); // Time to First Byte
        webVitals.onINP(reportWebVital); // Interaction to Next Paint (FID의 후속 메트릭)

      } catch (error) {
        console.warn('Web Vitals measurement failed:', error);
      }
    }

    measureWebVitals();
  }, []);

  // 렌더링하지 않는 컴포넌트 (측정 목적)
  return null;
}

// Web Vitals 데이터를 수집하는 훅
export function useWebVitals() {
  useEffect(() => {
    const handleWebVital = (event: CustomEvent<WebVitalsMetric>) => {
      const metric = event.detail;

      // 여기서 추가적인 로직을 수행할 수 있음
      // 예: 특정 임계값 초과 시 알림, 로그 수집 등
      if (metric.rating === 'poor') {
        console.warn(`⚠️ Poor ${metric.name} detected:`, metric.value);
      }
    };

    if (typeof window !== 'undefined') {
      window.addEventListener('web-vital', handleWebVital as EventListener);

      return () => {
        window.removeEventListener('web-vital', handleWebVital as EventListener);
      };
    }
  }, []);
}


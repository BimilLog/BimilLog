"use client";

import { useState, useEffect } from "react";

/**
 * 디바이스 타입을 감지하는 Hook
 */
export function useDevice() {
  const [isMobile, setIsMobile] = useState(false);
  const [isTablet, setIsTablet] = useState(false);
  const [isDesktop, setIsDesktop] = useState(false);

  useEffect(() => {
    const checkDevice = () => {
      const width = window.innerWidth;
      setIsMobile(width < 768);
      setIsTablet(width >= 768 && width < 1024);
      setIsDesktop(width >= 1024);
    };

    checkDevice();
    window.addEventListener("resize", checkDevice);
    return () => window.removeEventListener("resize", checkDevice);
  }, []);

  return {
    isMobile,
    isTablet,
    isDesktop,
    isTouch: isMobile || isTablet,
  };
}
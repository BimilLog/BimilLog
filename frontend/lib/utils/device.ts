/**
 * Device detection utilities
 */

/**
 * Check if the current device is iOS (iPhone, iPad, iPod)
 * @returns true if running on iOS device, false otherwise
 */
export function isIOS(): boolean {
  if (typeof navigator === "undefined") return false;

  return /iPad|iPhone|iPod/.test(navigator.userAgent) &&
         !(window as unknown as { MSStream?: unknown }).MSStream;
}

/**
 * Check if the current browser is Safari
 * @returns true if running on Safari, false otherwise
 */
export function isSafari(): boolean {
  if (typeof navigator === "undefined") return false;

  return /Safari/i.test(navigator.userAgent) && !/Chrome/i.test(navigator.userAgent);
}

/**
 * User-Agent 기반 모바일/태블릿 감지
 * @returns true if running on mobile or tablet device, false otherwise
 */
export function isMobileOrTablet(): boolean {
  if (typeof navigator === "undefined") return false;

  const userAgent = navigator.userAgent.toLowerCase();
  const mobileKeywords = [
    'mobile', 'android', 'iphone', 'ipad', 'ipod',
    'blackberry', 'windows phone', 'opera mini'
  ];

  return mobileKeywords.some(keyword => userAgent.includes(keyword));
}
interface AuthLoadingScreenProps {
  message?: string;
  subMessage?: string;
  /**
   * 회복(`recovery`) 흐름인지 표시. 회복 흐름은 spinner 톤을 차분하게(navy) 처리해
   * 일반 인증 성공 흐름과 시각적으로 구분한다.
   */
  variant?: "primary" | "recovery";
  /**
   * 회귀 스크린샷 식별용 marker. 시각에는 영향 없음 (사용자 비노출).
   *
   * - `primary`: 일반 인증 처리 화면 (mock/실주행 콜백)
   * - `recovery`: 회복(navy) 흐름
   * - `suspense-fallback`: Next.js Suspense fallback 으로 표시되는 화면
   *
   * iter-2 NEEDS_FIX-4 대응: Suspense fallback 와 본 화면이 픽셀 동일해서
   * 회귀 캡처에서 구분이 어려운 점을 데이터 속성으로 해소.
   */
  screen?: "primary" | "recovery" | "suspense-fallback";
}

/**
 * 인증 처리 중에 표시되는 풀스크린 로딩 화면.
 *
 * - paper/stamp 메타포 보존: 배경 `bg-paper-soft/85 dark:bg-paper-card/90`
 *   (round-1 토큰 활용. 라이트/다크 모두 paper 톤 유지)
 * - 디자인 시스템 컬러: spinner 는 stamp-red 단색 (Flowbite Spinner pink → 직접 SVG)
 * - 변경 근거: ui-review iter-1 NEEDS_FIX 4 (L-6 / D-8),
 *   iter-2 NEEDS_FIX-2 (spinner 두께/alpha 보강), NEEDS_FIX-4 (data-screen marker)
 */
export function AuthLoadingScreen({
  message = "로딩 중...",
  subMessage,
  variant = "primary",
  screen,
}: AuthLoadingScreenProps) {
  const ringColor =
    variant === "recovery"
      ? "border-postal-navy/40 border-t-postal-navy"
      : "border-stamp-red/40 border-t-stamp-red";

  // screen prop 미지정 시 variant 로 fallback (suspense-fallback 만 호출처에서 명시)
  const screenMarker = screen ?? variant;

  return (
    <div
      role="status"
      aria-live="polite"
      data-screen={screenMarker}
      className="fixed inset-0 z-[100] flex items-center justify-center bg-paper-50 dark:bg-paper-50"
    >
      <div className="flex flex-col items-center">
        {/* paper/stamp 메타포에 어울리는 단색 spinner — Flowbite pink 대체.
            iter-2: ring 두께 3 → 4px, alpha 30 → 40% 로 시인성 보강. */}
        <span
          aria-hidden="true"
          className={`inline-block h-12 w-12 animate-spin rounded-full border-4 ${ringColor}`}
        />
        <h2 className="mt-6 text-2xl font-display text-stamp-red dark:text-stamp-red font-bold mb-2 tracking-tight">
          {message}
        </h2>
        {subMessage && (
          <p className="text-ink-soft dark:text-foreground/80 font-body">
            {subMessage}
          </p>
        )}
      </div>
    </div>
  );
}

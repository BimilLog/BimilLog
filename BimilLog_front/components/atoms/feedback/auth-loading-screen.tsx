interface AuthLoadingScreenProps {
  message?: string;
  subMessage?: string;
  /**
   * 회복(`recovery`) 흐름인지 표시. 회복 흐름은 spinner 톤을 차분하게(navy) 처리해
   * 일반 인증 성공 흐름과 시각적으로 구분한다.
   */
  variant?: "primary" | "recovery";
}

/**
 * 인증 처리 중에 표시되는 풀스크린 로딩 화면.
 *
 * - paper/stamp 메타포 보존: 배경 `bg-paper-soft/85 dark:bg-paper-card/90`
 *   (round-1 토큰 활용. 라이트/다크 모두 paper 톤 유지)
 * - 디자인 시스템 컬러: spinner 는 stamp-red 단색 (Flowbite Spinner pink → 직접 SVG)
 * - 변경 근거: ui-review iter-1 NEEDS_FIX 4 (L-6 / D-8)
 */
export function AuthLoadingScreen({
  message = "로딩 중...",
  subMessage,
  variant = "primary",
}: AuthLoadingScreenProps) {
  const ringColor =
    variant === "recovery"
      ? "border-postal-navy/30 border-t-postal-navy"
      : "border-stamp-red/30 border-t-stamp-red";

  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed inset-0 z-[100] flex items-center justify-center bg-paper-soft/85 dark:bg-paper-card/90 backdrop-blur-sm"
    >
      <div className="flex flex-col items-center">
        {/* paper/stamp 메타포에 어울리는 단색 spinner — Flowbite pink 대체 */}
        <span
          aria-hidden="true"
          className={`inline-block h-12 w-12 animate-spin rounded-full border-[3px] ${ringColor}`}
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

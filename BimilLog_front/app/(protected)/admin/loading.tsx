import { Spinner as FlowbiteSpinner } from "flowbite-react";

export default function Loading() {
  return (
    <div
      className="min-h-screen bg-paper flex items-center justify-center py-16 px-4"
      role="status"
      aria-live="polite"
    >
      <div className="flex flex-col items-center">
        <FlowbiteSpinner
          color="failure"
          size="xl"
          aria-label="관리자 대시보드 로딩 중"
        />
        <h2 className="mt-4 font-display text-lg font-semibold text-ink dark:text-foreground mb-2 break-keep">
          관리자 대시보드
        </h2>
        <p className="font-body text-ink-soft dark:text-muted-foreground break-keep">
          로딩 중...
        </p>
      </div>
    </div>
  );
}

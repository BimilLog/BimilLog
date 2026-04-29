import { Spinner as FlowbiteSpinner } from "flowbite-react";

export default function Loading() {
  return (
    <div className="min-h-screen bg-paper flex items-center justify-center px-4">
      <div className="flex flex-col items-center">
        <FlowbiteSpinner
          color="failure"
          size="xl"
          aria-label="롤링페이퍼 방문 로딩 중"
        />
        <h2 className="mt-4 font-display text-xl font-bold text-stamp-red mb-2 tracking-tight">
          롤링페이퍼 방문
        </h2>
        <p className="font-body text-ink-soft text-sm">
          친구를 찾는 중입니다...
        </p>
      </div>
    </div>
  );
}

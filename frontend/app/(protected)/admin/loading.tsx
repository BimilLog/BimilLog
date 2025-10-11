import { Spinner as FlowbiteSpinner } from "flowbite-react";

export default function Loading() {
  return (
    <div className="flex items-center justify-center py-16">
      <div className="flex flex-col items-center">
        <FlowbiteSpinner
          color="pink"
          size="xl"
          aria-label="관리자 대시보드 로딩 중"
        />
        <h2 className="mt-4 text-lg font-semibold text-brand-primary mb-2">관리자 대시보드</h2>
        <p className="text-brand-muted">로딩 중...</p>
      </div>
    </div>
  );
}

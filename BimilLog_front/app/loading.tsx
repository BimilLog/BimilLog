import { Spinner as FlowbiteSpinner } from "flowbite-react";

export default function Loading() {
  return (
    <div className="min-h-screen bg-paper flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <FlowbiteSpinner
          color="failure"
          size="xl"
          aria-label="페이지를 불러오는 중..."
        />
        <p className="font-body text-ink-soft text-sm font-medium">페이지를 불러오는 중...</p>
      </div>
    </div>
  );
}

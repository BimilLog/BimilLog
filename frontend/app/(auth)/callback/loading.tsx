import { Spinner as FlowbiteSpinner } from "flowbite-react";

export default function Loading() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <FlowbiteSpinner
          color="pink"
          size="xl"
          aria-label="로그인 처리 중..."
        />
        <p className="text-brand-muted text-sm font-medium">로그인 처리 중...</p>
      </div>
    </div>
  );
}

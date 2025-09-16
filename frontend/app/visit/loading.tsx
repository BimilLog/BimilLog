import { Spinner as FlowbiteSpinner } from "flowbite-react";

export default function Loading() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center px-4">
      <div className="flex flex-col items-center">
        <FlowbiteSpinner
          color="pink"
          size="xl"
          aria-label="롤링페이퍼 방문 로딩 중"
        />
        <h2 className="mt-4 text-xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent mb-2">
          롤링페이퍼 방문
        </h2>
        <p className="text-brand-muted text-sm">
          친구를 찾는 중입니다...
        </p>
      </div>
    </div>
  );
}

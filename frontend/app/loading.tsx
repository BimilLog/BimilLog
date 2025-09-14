import { Spinner } from "@/components";

export default function Loading() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <Spinner size="lg" />
        <p className="text-brand-muted text-sm font-medium">페이지를 불러오는 중...</p>
      </div>
    </div>
  );
}

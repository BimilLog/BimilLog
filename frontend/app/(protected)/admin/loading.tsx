import { Shield } from "lucide-react";

export default function Loading() {
  return (
    <div className="flex items-center justify-center py-16">
      <div className="text-center">
        <div className="flex justify-center mb-4">
          <Shield className="w-12 h-12 text-blue-600 animate-pulse" />
        </div>
        <h2 className="text-lg font-semibold text-gray-800 mb-2">관리자 대시보드</h2>
        <div className="flex items-center justify-center space-x-2">
          <div className="w-2 h-2 bg-blue-600 rounded-full animate-bounce" style={{ animationDelay: "0ms" }}></div>
          <div className="w-2 h-2 bg-blue-600 rounded-full animate-bounce" style={{ animationDelay: "150ms" }}></div>
          <div className="w-2 h-2 bg-blue-600 rounded-full animate-bounce" style={{ animationDelay: "300ms" }}></div>
        </div>
        <p className="text-gray-600 mt-4">로딩 중...</p>
      </div>
    </div>
  );
}

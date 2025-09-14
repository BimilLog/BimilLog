import { MessageSquare } from "lucide-react";

export default function Loading() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center px-4">
      <div className="text-center">
        <div className="w-16 h-16 bg-gradient-to-r from-pink-500 to-purple-600 rounded-2xl flex items-center justify-center mx-auto mb-4 animate-bounce">
          <MessageSquare className="w-9 h-9 text-white" />
        </div>
        <h2 className="text-xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent mb-2">
          롤링페이퍼 방문
        </h2>
        <p className="text-gray-600 text-sm">
          친구를 찾는 중입니다...
        </p>
      </div>
    </div>
  );
}

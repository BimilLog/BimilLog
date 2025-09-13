import { Button } from "@/components";
import { Shield } from "lucide-react";
import Link from "next/link";

export const AdminHeader: React.FC = () => {
  return (
    <div className="mb-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <Shield className="w-6 h-6 text-gradient bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600" />
            <h1 className="text-2xl font-bold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent">
              관리자 대시보드
            </h1>
          </div>
          <p className="mt-2 text-gray-600 text-sm">
            신고 관리 및 서비스 통계를 확인할 수 있습니다.
          </p>
        </div>
        <Link href="/" className="flex-shrink-0">
          <Button 
            variant="outline" 
            className="min-h-[48px] px-6 bg-white hover:bg-gray-50 border-gray-200 text-gray-700 font-medium"
          >
            홈으로 돌아가기
          </Button>
        </Link>
      </div>
    </div>
  );
};
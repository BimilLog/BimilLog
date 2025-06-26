import { Button } from "@/components/ui/button";
import { Shield } from "lucide-react";
import Link from "next/link";

export const AdminHeader: React.FC = () => {
  return (
    <div className="mb-8">
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center space-x-2">
            <Shield className="w-6 h-6 text-blue-600" />
            <h1 className="text-xl font-bold text-gray-800">관리자 대시보드</h1>
          </div>
        </div>
        <Link href="/">
          <Button variant="outline">홈으로 돌아가기</Button>
        </Link>
      </div>
    </div>
  );
};

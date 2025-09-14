import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { TrendingUp } from "lucide-react";
import type { Report } from "@/types/domains/admin";

interface AdminStatsProps {
  reports: Report[];
}

export const AdminStats: React.FC<AdminStatsProps> = React.memo(({ reports }) => {
  // TODO: Implement actual statistics using reports data
  void reports; // Suppress unused variable warning
  return (
    <Card className="bg-white/90 backdrop-blur-sm border-0 shadow-brand-lg hover:shadow-brand-xl transition-all duration-200 rounded-lg">
      <CardHeader>
        <CardTitle className="flex items-center gap-3">
          <div className="p-2 rounded-full bg-gradient-to-r from-green-100 to-emerald-100">
            <TrendingUp className="w-5 h-5 text-green-600" />
          </div>
          <span className="text-lg font-semibold text-brand-primary">서비스 통계</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex flex-col items-center justify-center py-12 px-4">
          <div className="mb-6 p-4 rounded-full bg-gradient-to-r from-gray-100 to-gray-200">
            <TrendingUp className="w-12 h-12 text-brand-secondary" />
          </div>
          <h3 className="text-lg font-medium text-brand-primary mb-2">통계 데이터 준비 중</h3>
          <p className="text-sm text-brand-secondary text-center max-w-md">
            사용자 활동, 롤링페이퍼 생성, 신고 처리 등의 <br />
            상세한 통계 데이터는 추후 구현 예정입니다.
          </p>
        </div>
      </CardContent>
    </Card>
  );
});
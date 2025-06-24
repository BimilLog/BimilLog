import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { CheckCircle, XCircle, UserX } from "lucide-react";
import { type Report, adminApi } from "@/lib/api";
import { useState } from "react";

interface ReportDetailModalProps {
  report: Report | null;
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
}

export const ReportDetailModal: React.FC<ReportDetailModalProps> = ({
  report,
  isOpen,
  onOpenChange,
}) => {
  const [isBanning, setIsBanning] = useState(false);

  const getStatusColor = (status: string) => {
    switch (status) {
      case "pending":
        return "bg-yellow-100 text-yellow-800 border-yellow-200";
      case "investigating":
        return "bg-blue-100 text-blue-800 border-blue-200";
      case "resolved":
        return "bg-green-100 text-green-800 border-green-200";
      case "rejected":
        return "bg-red-100 text-red-800 border-red-200";
      default:
        return "bg-gray-100 text-gray-800 border-gray-200";
    }
  };

  const getReportTypeLabel = (type: string) => {
    switch (type) {
      case "POST":
        return "게시글";
      case "COMMENT":
        return "댓글";
      case "ERROR":
        return "오류";
      case "IMPROVEMENT":
        return "개선사항";
      default:
        return "기타";
    }
  };

  const handleBanUser = async () => {
    if (!report || !report.targetId) return;

    const confirmBan = window.confirm(
      "정말로 이 사용자를 차단하시겠습니까? 이 작업은 되돌릴 수 없습니다."
    );

    if (!confirmBan) return;

    setIsBanning(true);
    try {
      const response = await adminApi.banUserByReport({
        targetId: report.targetId,
        reportType: report.reportType,
        content: report.content,
      });

      if (response.success) {
        alert("사용자가 성공적으로 차단되었습니다.");
        onOpenChange(false);
      } else {
        alert(response.error || "사용자 차단에 실패했습니다.");
      }
    } catch (error) {
      console.error("Ban user failed:", error);
      alert("사용자 차단 중 오류가 발생했습니다.");
    } finally {
      setIsBanning(false);
    }
  };

  if (!report) return null;

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>신고 상세 정보</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium text-gray-700">
                신고 유형
              </label>
              <p className="text-sm text-gray-900">
                {getReportTypeLabel(report.reportType)}
              </p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">
                대상 ID
              </label>
              <p className="text-sm text-gray-900">{report.targetId}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">
                신고자 ID
              </label>
              <p className="text-sm text-gray-900">{report.userId}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">상태</label>
              <Badge
                className={`text-xs ${getStatusColor(
                  report.status || "pending"
                )}`}
              >
                {report.status === "pending"
                  ? "대기중"
                  : report.status === "investigating"
                  ? "조사중"
                  : report.status === "resolved"
                  ? "해결됨"
                  : "반려됨"}
              </Badge>
            </div>
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">
              신고 내용
            </label>
            <div className="mt-1 p-3 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-900">{report.content}</p>
            </div>
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">
              관리자 메모
            </label>
            <Textarea
              placeholder="처리 내용을 기록하세요..."
              className="mt-1"
            />
          </div>

          <div className="flex space-x-2">
            <Button className="bg-green-600 hover:bg-green-700">
              <CheckCircle className="w-4 h-4 mr-2" />
              승인
            </Button>
            <Button
              variant="outline"
              className="border-red-200 text-red-600 hover:bg-red-50"
            >
              <XCircle className="w-4 h-4 mr-2" />
              반려
            </Button>
            {report.targetId && (
              <Button
                onClick={handleBanUser}
                disabled={isBanning}
                variant="destructive"
                className="bg-red-600 hover:bg-red-700"
              >
                <UserX className="w-4 h-4 mr-2" />
                {isBanning ? "처리 중..." : "회원탈퇴"}
              </Button>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

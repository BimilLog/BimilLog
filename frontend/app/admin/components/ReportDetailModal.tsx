import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { UserX } from "lucide-react";
import { type Report, adminApi } from "@/lib/api";
import { useState } from "react";
import { useToast } from "@/hooks/useToast";

interface ReportDetailModalProps {
  report: Report | null;
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  onReportUpdated?: () => void;
}

export const ReportDetailModal: React.FC<ReportDetailModalProps> = ({
  report,
  isOpen,
  onOpenChange,
  onReportUpdated,
}) => {
  const [isBanning, setIsBanning] = useState(false);
  const { showSuccess, showError } = useToast();


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
      // v2 API로 업데이트: banUser 메소드 사용 - targetId의 사용자를 차단
      const response = await adminApi.banUser({
        reporterId: report.reporterId || report.userId,
        reporterName: report.reporterName,
        reportType: report.reportType,
        targetId: report.targetId,
        content: report.content,
      });

      if (response.success) {
        showSuccess("사용자 차단", "사용자가 성공적으로 차단되었습니다.");
        onOpenChange(false);
        // 목록 새로고침 콜백 호출
        if (onReportUpdated) {
          onReportUpdated();
        }
      } else {
        showError("차단 실패", response.error || "사용자 차단에 실패했습니다.");
      }
    } catch (error) {
      console.error("Ban user failed:", error);
      showError("차단 실패", "사용자 차단 중 오류가 발생했습니다.");
    } finally {
      setIsBanning(false);
    }
  };

  const handleForceWithdrawUser = async () => {
    if (!report || !report.targetId) return;

    const confirmWithdraw = window.confirm(
      "정말로 이 사용자를 강제 탈퇴시키시겠습니까? 이 작업은 되돌릴 수 없습니다."
    );

    if (!confirmWithdraw) return;

    setIsBanning(true);
    try {
      // v2 API: forceWithdrawUser 사용 (ReportDTO 방식)
      const response = await adminApi.forceWithdrawUser({
        targetId: report.targetId,
        reportType: report.reportType,
        content: report.content
      });

      if (response.success) {
        showSuccess("사용자 탈퇴", "사용자가 성공적으로 탈퇴 처리되었습니다.");
        onOpenChange(false);
        // 목록 새로고침 콜백 호출
        if (onReportUpdated) {
          onReportUpdated();
        }
      } else {
        showError("탈퇴 실패", response.error || "사용자 탈퇴에 실패했습니다.");
      }
    } catch (error) {
      console.error("Force withdraw user failed:", error);
      showError("탈퇴 실패", "사용자 탈퇴 중 오류가 발생했습니다.");
    } finally {
      setIsBanning(false);
    }
  };

  if (!report) return null;

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>신고 상세 정보</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium text-gray-700">
                신고 유형
              </label>
              <p className="text-sm text-gray-900 break-words">
                {getReportTypeLabel(report.reportType)}
              </p>
            </div>
            {report.targetId && (
              <div>
                <label className="text-sm font-medium text-gray-700">
                  대상 ID
                </label>
                <p className="text-sm text-gray-900 break-all">{report.targetId}</p>
              </div>
            )}
            <div>
              <label className="text-sm font-medium text-gray-700">
                신고자
              </label>
              <p className="text-sm text-gray-900 break-words">
                {report.reporterName || `ID: ${report.reporterId || report.userId}`}
              </p>
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">처리 상태</label>
              <Badge className="text-xs bg-yellow-100 text-yellow-800 border-yellow-200">
                처리 대기
              </Badge>
            </div>
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">
              신고 내용
            </label>
            <div className="mt-1 p-3 bg-gray-50 rounded-lg max-h-40 overflow-y-auto">
              <p className="text-sm text-gray-900 whitespace-pre-wrap break-words">{report.content}</p>
            </div>
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">
              관리자 메모
            </label>
            <Textarea
              placeholder="처리 내용을 기록하세요..."
              className="mt-1 resize-none"
              rows={4}
            />
          </div>

          {report.targetId && report.reportType !== "ERROR" && report.reportType !== "IMPROVEMENT" && (
            <div className="flex flex-wrap gap-2">
              <Button
                onClick={handleBanUser}
                disabled={isBanning}
                variant="destructive"
                className="bg-orange-600 hover:bg-orange-700 flex-shrink-0"
              >
                <UserX className="w-4 h-4 mr-2" />
                {isBanning ? "처리 중..." : "사용자 제재"}
              </Button>
              <Button
                onClick={handleForceWithdrawUser}
                disabled={isBanning}
                variant="destructive"
                className="bg-red-600 hover:bg-red-700 flex-shrink-0"
              >
                <UserX className="w-4 h-4 mr-2" />
                {isBanning ? "처리 중..." : "강제 탈퇴"}
              </Button>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
};

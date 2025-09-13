"use client";

import { useState } from "react";
import { 
  Button, 
  Badge, 
  Textarea,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle
} from "@/components";
import { UserX, AlertTriangle } from "lucide-react";
import { type Report, adminCommand } from "@/lib/api";
import { useToast } from "@/hooks/useToast";
import {
  getReportTypeConfig,
  formatDateTime,
  hasActionableTarget
} from "@/lib/utils/admin";

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
  const [isProcessing, setIsProcessing] = useState(false);
  const [adminMemo, setAdminMemo] = useState("");
  const { showSuccess, showError } = useToast();

  const typeConfig = report ? getReportTypeConfig(report.reportType) : null;

  const handleBanUser = async () => {
    if (!report || !report.targetId) return;

    const confirmBan = window.confirm(
      "정말로 이 사용자를 제재하시겠습니까? 이 작업은 되돌릴 수 없습니다."
    );

    if (!confirmBan) return;

    setIsProcessing(true);
    try {
      const response = await adminCommand.banUser({
        reporterId: report.reporterId,
        reporterName: report.reporterName,
        reportType: report.reportType,
        targetId: report.targetId,
        content: report.content,
      });

      if (response.success) {
        showSuccess("사용자 제재 완료", "사용자가 성공적으로 제재되었습니다.");
        onOpenChange(false);
        onReportUpdated?.();
      } else {
        showError("제재 실패", response.error || "사용자 제재에 실패했습니다.");
      }
    } catch (error) {
      console.error("Ban user failed:", error);
      showError("제재 실패", "사용자 제재 중 오류가 발생했습니다.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleForceWithdrawUser = async () => {
    if (!report || !report.targetId) return;

    const confirmWithdraw = window.confirm(
      "정말로 이 사용자를 강제 탈퇴시키시겠습니까? 이 작업은 되돌릴 수 없습니다."
    );

    if (!confirmWithdraw) return;

    setIsProcessing(true);
    try {
      const response = await adminCommand.forceWithdrawUser({
        targetId: report.targetId,
        reportType: report.reportType,
        content: report.content
      });

      if (response.success) {
        showSuccess("강제 탈퇴 완료", "사용자가 성공적으로 탈퇴 처리되었습니다.");
        onOpenChange(false);
        onReportUpdated?.();
      } else {
        showError("탈퇴 실패", response.error || "사용자 탈퇴에 실패했습니다.");
      }
    } catch (error) {
      console.error("Force withdraw user failed:", error);
      showError("탈퇴 실패", "사용자 탈퇴 중 오류가 발생했습니다.");
    } finally {
      setIsProcessing(false);
    }
  };

  if (!report) return null;

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto bg-white">
        <DialogHeader>
          <DialogTitle className="text-xl font-semibold bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent">
            신고 상세 정보
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  신고 유형
                </label>
                <Badge className={`${typeConfig?.color} font-medium`}>
                  {typeConfig?.label}
                </Badge>
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  신고자 정보
                </label>
                <div className="p-3 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-900 font-medium">
                    {report.reporterName}
                  </p>
                  <p className="text-xs text-gray-600 mt-1">
                    ID: {report.reporterId}
                  </p>
                </div>
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  처리 상태
                </label>
                <Badge className="bg-yellow-100 text-yellow-800 border-yellow-200 font-medium">
                  처리 대기
                </Badge>
              </div>
            </div>

            <div className="space-y-4">
              {report.targetId && hasActionableTarget(report.reportType) && (
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    신고 대상 ID
                  </label>
                  <div className="p-3 bg-red-50 rounded-lg border border-red-200">
                    <p className="text-sm text-red-900 font-mono font-medium">
                      {report.targetId}
                    </p>
                  </div>
                </div>
              )}

              {report.targetTitle && (
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    신고된 콘텐츠 제목
                  </label>
                  <div className="p-3 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-900">
                      {report.targetTitle}
                    </p>
                  </div>
                </div>
              )}

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  신고 접수 시간
                </label>
                <p className="text-sm text-gray-700 font-mono">
                  {formatDateTime(report.createdAt)}
                </p>
              </div>
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-3">
              신고 내용
            </label>
            <div className="p-4 bg-gradient-to-br from-gray-50 to-gray-100 rounded-lg border border-gray-200 max-h-48 overflow-y-auto">
              <p className="text-sm text-gray-900 whitespace-pre-wrap leading-relaxed">
                {report.content}
              </p>
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-3">
              관리자 메모
            </label>
            <Textarea
              placeholder="처리 내용이나 특이사항을 기록하세요..."
              value={adminMemo}
              onChange={(e) => setAdminMemo(e.target.value)}
              className="min-h-[120px] resize-none bg-white border-gray-200 focus:border-purple-400 focus:ring-purple-400/20"
              rows={5}
            />
          </div>

          {report.targetId && hasActionableTarget(report.reportType) && (
            <div className="pt-4 border-t border-gray-200">
              <h4 className="text-sm font-semibold text-gray-700 mb-4">
                관리자 조치
              </h4>
              <div className="flex flex-col sm:flex-row gap-3">
                <Button
                  onClick={handleBanUser}
                  disabled={isProcessing}
                  variant="destructive"
                  className="min-h-[48px] bg-gradient-to-r from-orange-500 to-orange-600 hover:from-orange-600 hover:to-orange-700 font-medium"
                >
                  <UserX className="w-4 h-4 mr-2" />
                  {isProcessing ? "처리 중..." : "사용자 제재"}
                </Button>
                <Button
                  onClick={handleForceWithdrawUser}
                  disabled={isProcessing}
                  variant="destructive"
                  className="min-h-[48px] bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700 font-medium"
                >
                  <UserX className="w-4 h-4 mr-2" />
                  {isProcessing ? "처리 중..." : "강제 탈퇴"}
                </Button>
              </div>
              <p className="text-xs text-gray-500 mt-3 flex items-center gap-1">
                <AlertTriangle className="w-3 h-3 text-amber-500" />
                조치는 되돌릴 수 없습니다. 신중히 결정해주세요.
              </p>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
};
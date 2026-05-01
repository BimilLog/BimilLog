"use client";

import { useState } from "react";
import { adminCommand, type Report } from "@/lib/api";
import { useToast } from "@/hooks";
import { logger } from '@/lib/utils/logger';

interface UseReportActionsOptions {
  /**
   * 라운드 16 F-16-036 옵션 B: 차단/탈퇴 성공 시 같은 targetId+reportType 의
   * 다른 신고도 옵티미스틱하게 제거해 주는 콜백. 부모(useReports)에서 주입.
   * 반환값은 함께 정리된 신고 수 (자기 자신 포함).
   */
  onResolved?: (targetId: number, reportType: string) => number;
}

export function useReportActions(options: UseReportActionsOptions = {}) {
  const { onResolved } = options;
  const [isProcessing, setIsProcessing] = useState(false);
  const { showSuccess, showError } = useToast();

  const banUser = async (report: Report) => {
    if (!report.targetId) {
      showError("이용 막기 실패", "대상 ID가 없어요.");
      return false;
    }

    if (!report.targetAuthorName) {
      showError("이용 막기 실패", "신고 대상이 존재하지 않아 처리할 수 없어요.");
      return false;
    }

    setIsProcessing(true);
    try {
      const response = await adminCommand.banUser({
        reportType: report.reportType,
        targetId: report.targetId,
      });

      if (response.success) {
        // 라운드 16 F-16-036: 동일 targetId+reportType 의 모든 신고를 옵티미스틱 제거
        const removedCount = onResolved?.(report.targetId, report.reportType) ?? 0;
        const extra = removedCount > 1 ? `같은 대상 신고 ${removedCount - 1}건도 함께 정리했어요.` : "";
        showSuccess(
          "이용을 24시간 막았어요",
          `해당 사용자가 24시간 동안 서비스를 이용할 수 없어요. ${extra}`.trim(),
        );
        return true;
      } else {
        const errorMessage = response.error || "이용 막기에 실패했어요.";
        showError("이용 막기 실패", errorMessage);
        return false;
      }
    } catch (error) {
      logger.error("Ban user failed:", error);
      showError("이용 막기 실패", "처리 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.");
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  const forceWithdrawUser = async (report: Report) => {
    if (!report.targetId) {
      showError("탈퇴 처리 실패", "대상 ID가 없어요.");
      return false;
    }

    if (!report.targetAuthorName) {
      showError("탈퇴 처리 실패", "신고 대상이 존재하지 않아 처리할 수 없어요.");
      return false;
    }

    setIsProcessing(true);
    try {
      const response = await adminCommand.forceWithdrawUser({
        targetId: report.targetId,
        reportType: report.reportType,
      });

      if (response.success) {
        const removedCount = onResolved?.(report.targetId, report.reportType) ?? 0;
        const extra = removedCount > 1 ? `같은 대상 신고 ${removedCount - 1}건도 함께 정리했어요.` : "";
        showSuccess(
          "탈퇴 처리를 시작했어요",
          `데이터 정리는 백그라운드에서 약 10초 정도 소요돼요. ${extra}`.trim(),
        );
        return true;
      } else {
        const errorMessage = response.error || "탈퇴 처리에 실패했어요.";
        showError("탈퇴 처리 실패", errorMessage);
        return false;
      }
    } catch (error) {
      logger.error("Force withdraw user failed:", error);
      showError("탈퇴 처리 실패", "처리 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.");
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  return {
    isProcessing,
    banUser,
    forceWithdrawUser,
  };
}

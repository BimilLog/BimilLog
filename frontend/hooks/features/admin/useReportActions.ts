"use client";

import { useState } from "react";
import { adminCommand, type Report } from "@/lib/api";
import { useToast } from "@/hooks";
import { logger } from '@/lib/utils/logger';

export function useReportActions() {
  const [isProcessing, setIsProcessing] = useState(false);
  const { showSuccess, showError } = useToast();

  const banUser = async (report: Report) => {
    if (!report.targetId) {
      showError("제재 실패", "대상 ID가 없습니다.");
      return false;
    }

    if (!report.targetAuthorName) {
      showError("제재 실패", "신고 대상이 존재하지 않아 제재할 수 없습니다.");
      return false;
    }

    setIsProcessing(true);
    try {
      const response = await adminCommand.banUser({
        reportType: report.reportType,
        targetId: report.targetId,
      });

      if (response.success) {
        showSuccess(
          "사용자 제재 완료",
          "사용자가 24시간 동안 서비스 이용이 제한됩니다."
        );
        return true;
      } else {
        const errorMessage = response.error || "사용자 제재에 실패했습니다.";
        showError("제재 실패", errorMessage);
        return false;
      }
    } catch (error) {
      logger.error("Ban user failed:", error);
      showError("제재 실패", "사용자 제재 중 오류가 발생했습니다.");
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  const forceWithdrawUser = async (report: Report) => {
    if (!report.targetId) {
      showError("탈퇴 실패", "대상 ID가 없습니다.");
      return false;
    }

    if (!report.targetAuthorName) {
      showError("탈퇴 실패", "신고 대상이 존재하지 않아 탈퇴 처리할 수 없습니다.");
      return false;
    }

    setIsProcessing(true);
    try {
      const response = await adminCommand.forceWithdrawUser({
        targetId: report.targetId,
        reportType: report.reportType,
      });

      if (response.success) {
        showSuccess(
          "강제 탈퇴 처리 시작",
          "사용자 데이터 정리를 시작했습니다. 백그라운드에서 처리됩니다. (약 10초 소요)"
        );
        return true;
      } else {
        const errorMessage = response.error || "사용자 탈퇴에 실패했습니다.";
        showError("탈퇴 실패", errorMessage);
        return false;
      }
    } catch (error) {
      logger.error("Force withdraw user failed:", error);
      showError("탈퇴 실패", "사용자 탈퇴 중 오류가 발생했습니다.");
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
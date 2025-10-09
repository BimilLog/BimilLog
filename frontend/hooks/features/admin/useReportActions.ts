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
      // 신고 정보를 바탕으로 사용자 제재 처리
      const response = await adminCommand.banUser({
        reportType: report.reportType,
        targetId: report.targetId,
      });

      if (response.success) {
        showSuccess("사용자 제재 완료", "사용자가 성공적으로 제재되었습니다.");
        return true;
      } else {
        showError("제재 실패", response.error || "사용자 제재에 실패했습니다.");
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
      // 강제 탈퇴 처리: 사용자 계정 및 관련 데이터 완전 삭제
      const response = await adminCommand.forceWithdrawUser({
        targetId: report.targetId,
        reportType: report.reportType,
      });

      if (response.success) {
        showSuccess("강제 탈퇴 완료", "사용자가 성공적으로 탈퇴 처리되었습니다.");
        return true;
      } else {
        showError("탈퇴 실패", response.error || "사용자 탈퇴에 실패했습니다.");
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
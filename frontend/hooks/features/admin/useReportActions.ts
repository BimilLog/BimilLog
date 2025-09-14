"use client";

import { useState } from "react";
import { adminCommand, type Report } from "@/lib/api";
import { useToast } from "@/hooks";
import { logger } from '@/lib/utils/logger';

export function useReportActions(onSuccess?: () => void) {
  const [isProcessing, setIsProcessing] = useState(false);
  const { showSuccess, showError } = useToast();

  const banUser = async (report: Report) => {
    if (!report.targetId) {
      showError("제재 실패", "대상 ID가 없습니다.");
      return false;
    }

    // 제재 확인창: 되돌릴 수 없는 작업이므로 2중 확인
    const confirmBan = window.confirm(
      "정말로 이 사용자를 제재하시겠습니까? 이 작업은 되돌릴 수 없습니다."
    );

    if (!confirmBan) return false;

    setIsProcessing(true);
    try {
      // 신고 정보를 바탕으로 사용자 제재 처리
      const response = await adminCommand.banUser({
        reporterId: report.reporterId,
        reporterName: report.reporterName,
        reportType: report.reportType,
        targetId: report.targetId,
        content: report.content,
      });

      if (response.success) {
        showSuccess("사용자 제재 완료", "사용자가 성공적으로 제재되었습니다.");
        onSuccess?.(); // 콜백 실행하여 목록 새로고침 등 수행
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

    // 강제 탈퇴 확인창: 계정 완전 삭제 작업이므로 2중 확인
    const confirmWithdraw = window.confirm(
      "정말로 이 사용자를 강제 탈퇴시키시겠습니까? 이 작업은 되돌릴 수 없습니다."
    );

    if (!confirmWithdraw) return false;

    setIsProcessing(true);
    try {
      // 강제 탈퇴 처리: 사용자 계정 및 관련 데이터 완전 삭제
      const response = await adminCommand.forceWithdrawUser({
        targetId: report.targetId,
        reportType: report.reportType,
        content: report.content,
      });

      if (response.success) {
        showSuccess("강제 탈퇴 완료", "사용자가 성공적으로 탈퇴 처리되었습니다.");
        onSuccess?.(); // 콜백 실행하여 목록 새로고침 등 수행
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
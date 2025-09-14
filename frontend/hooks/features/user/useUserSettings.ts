"use client";

import { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useToast } from "@/hooks";
import { userQuery, userCommand, authCommand, Setting } from "@/lib/api";
import { logger } from '@/lib/utils/logger';

export function useSettings() {
  const [settings, setSettings] = useState<Setting | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { showSuccess, showError } = useToast();
  const router = useRouter();
  const isMounted = useRef(true);

  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await userQuery.getSettings();
      if (!isMounted.current) return;

      if (response.success && response.data) {
        setSettings(response.data);
      } else {
        const errorMessage = response.error || "설정을 불러오는 중 오류가 발생했습니다.";
        setError(errorMessage);
        showError("설정 로드 실패", errorMessage);
      }
    } catch (error) {
      if (!isMounted.current) return;
      logger.error("설정 로드 실패:", error);
      const errorMessage = "설정을 불러오는 중 오류가 발생했습니다. 페이지를 새로고침해주세요.";
      setError(errorMessage);
      showError("설정 로드 실패", errorMessage);
    } finally {
      if (isMounted.current) {
        setLoading(false);
      }
    }
  };

  const updateSettings = async (newSettings: Partial<Setting>) => {
    if (!settings || !isMounted.current) return;

    const fullSettings: Setting = {
      ...settings,
      ...newSettings,
    };

    try {
      setSaving(true);
      const response = await userCommand.updateSettings(fullSettings);
      if (!isMounted.current) return;

      if (response.success) {
        setSettings(fullSettings);
        showSuccess("설정 저장 완료", "알림 설정이 성공적으로 저장되었습니다.");
      } else {
        showError(
          "설정 저장 실패",
          response.error || "설정 저장 중 오류가 발생했습니다. 다시 시도해주세요."
        );
        setSettings(settings);
      }
    } catch (error) {
      if (!isMounted.current) return;
      logger.error("설정 저장 실패:", error);
      showError(
        "설정 저장 실패",
        "설정 저장 중 오류가 발생했습니다. 다시 시도해주세요."
      );
      setSettings(settings);
    } finally {
      if (isMounted.current) {
        setSaving(false);
      }
    }
  };

  const handleSingleToggle = (
    field: keyof Pick<
      Setting,
      "messageNotification" | "commentNotification" | "postFeaturedNotification"
    >,
    value: boolean
  ) => {
    updateSettings({ [field]: value });
  };

  const handleAllToggle = (enabled: boolean) => {
    updateSettings({
      messageNotification: enabled,
      commentNotification: enabled,
      postFeaturedNotification: enabled,
    });
  };

  // 회원탈퇴 처리: 사용자 확인 후 탈퇴 진행, 성공 시 홈으로 이동
  const handleWithdraw = async () => {
    if (
      !window.confirm(
        "정말로 탈퇴하시겠습니까?\n\n탈퇴 시 모든 데이터가 삭제되며, 복구할 수 없습니다.\n작성한 게시글과 댓글, 롤링페이퍼 메시지가 모두 삭제됩니다.\n\n이 작업은 되돌릴 수 없습니다."
      )
    ) {
      return;
    }

    try {
      setWithdrawing(true);
      const response = await authCommand.withdraw();
      if (!isMounted.current) return;

      if (response.success) {
        showSuccess("회원탈퇴 완료", "회원탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사했습니다.");
        // 2초 후 홈으로 이동하여 사용자가 메시지를 읽을 시간을 제공
        setTimeout(() => {
          if (isMounted.current) {
            router.push("/");
            window.location.reload();
          }
        }, 2000);
      } else {
        showError(
          "회원탈퇴 실패",
          response.error || "회원탈퇴 중 오류가 발생했습니다. 다시 시도해주세요."
        );
      }
    } catch (error) {
      if (!isMounted.current) return;
      logger.error("회원탈퇴 실패:", error);
      showError(
        "회원탈퇴 실패",
        error instanceof Error
          ? error.message
          : "회원탈퇴 중 오류가 발생했습니다. 다시 시도해주세요."
      );
    } finally {
      if (isMounted.current) {
        setWithdrawing(false);
      }
    }
  };

  const allEnabled = Boolean(
    settings &&
      settings.messageNotification === true &&
      settings.commentNotification === true &&
      settings.postFeaturedNotification === true
  );

  return {
    settings,
    loading,
    saving,
    withdrawing,
    error,
    allEnabled,
    updateSettings,
    handleSingleToggle,
    handleAllToggle,
    handleWithdraw,
    loadSettings,
  };
}
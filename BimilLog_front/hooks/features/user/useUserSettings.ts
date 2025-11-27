"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useToast } from "@/hooks";
import { userQuery, userCommand, Setting } from "@/lib/api";
import { logger } from '@/lib/utils/logger';
import { useDebouncedCallback } from "@/hooks/common";

type SettingField = keyof Setting;

export function useUserSettings() {
  const [settings, setSettings] = useState<Setting | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savingFields, setSavingFields] = useState<Record<SettingField, boolean>>({
    messageNotification: false,
    commentNotification: false,
    postFeaturedNotification: false,
    friendSendNotification: false,
  });
  const [savedFields, setSavedFields] = useState<Record<SettingField, boolean>>({
    messageNotification: false,
    commentNotification: false,
    postFeaturedNotification: false,
    friendSendNotification: false,
  });
  const [withdrawing, setWithdrawing] = useState(false);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { showSuccess, showError } = useToast();
  const router = useRouter();
  const isMounted = useRef(false);
  const previousSettingsRef = useRef<Setting | null>(null);

  useEffect(() => {
    isMounted.current = true;

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
        previousSettingsRef.current = response.data;
      } else {
        const errorMessage = response.error || "설정을 불러오는 중 오류가 발생했습니다.";
        setError(errorMessage);
        // 에러 발생 시 기본값으로 설정 (모두 활성화)
        const defaultSettings: Setting = {
          messageNotification: true,
          commentNotification: true,
          postFeaturedNotification: true,
          friendSendNotification: true,
        };
        setSettings(defaultSettings);
        previousSettingsRef.current = defaultSettings;
        showError("설정 로드 실패", errorMessage);
      }
    } catch (error) {
      if (!isMounted.current) return;
      logger.error("설정 로드 실패:", error);
      const errorMessage = "설정을 불러오는 중 오류가 발생했습니다. 기본 설정으로 표시됩니다.";
      setError(errorMessage);
      // 에러 발생 시 기본값으로 설정 (모두 활성화)
      const defaultSettings: Setting = {
        messageNotification: true,
        commentNotification: true,
        postFeaturedNotification: true,
        friendSendNotification: true,
      };
      setSettings(defaultSettings);
      previousSettingsRef.current = defaultSettings;
      showError("설정 로드 실패", errorMessage);
    } finally {
      if (isMounted.current) {
        setLoading(false);
      }
    }
  };

  // 디바운스된 API 호출 함수
  const debouncedApiCall = useDebouncedCallback(
    async (settingsToSave: Setting, previousSettings: Setting, changedFields: SettingField[]): Promise<void> => {
      if (!isMounted.current) return;

      try {
        setSaving(true);
        // 변경된 필드들을 저장 중으로 표시
        setSavingFields(prev => {
          const updated = { ...prev };
          changedFields.forEach(field => { updated[field] = true; });
          return updated;
        });

        const response = await userCommand.updateSettings(settingsToSave);
        if (!isMounted.current) return;

        if (response.success) {
          previousSettingsRef.current = settingsToSave;

          // 저장 중 상태 해제
          setSavingFields(prev => {
            const updated = { ...prev };
            changedFields.forEach(field => { updated[field] = false; });
            return updated;
          });

          // 저장 완료 표시 (1.5초간 체크마크)
          setSavedFields(prev => {
            const updated = { ...prev };
            changedFields.forEach(field => { updated[field] = true; });
            return updated;
          });

          // 1.5초 후 체크마크 자동 숨김
          setTimeout(() => {
            if (isMounted.current) {
              setSavedFields(prev => {
                const updated = { ...prev };
                changedFields.forEach(field => { updated[field] = false; });
                return updated;
              });
            }
          }, 1500);
        } else {
          // 실패 시 이전 상태로 롤백
          setSettings(previousSettings);
          previousSettingsRef.current = previousSettings;

          // 저장 중 상태 해제
          setSavingFields(prev => {
            const updated = { ...prev };
            changedFields.forEach(field => { updated[field] = false; });
            return updated;
          });

          showError(
            "설정 저장 실패",
            response.error || "설정 저장 중 오류가 발생했습니다. 다시 시도해주세요."
          );
        }
      } catch (error) {
        if (!isMounted.current) return;
        logger.error("설정 저장 실패:", error);

        // 실패 시 이전 상태로 롤백
        setSettings(previousSettings);
        previousSettingsRef.current = previousSettings;

        // 저장 중 상태 해제
        setSavingFields(prev => {
          const updated = { ...prev };
          changedFields.forEach(field => { updated[field] = false; });
          return updated;
        });

        showError(
          "설정 저장 실패",
          "설정 저장 중 오류가 발생했습니다. 다시 시도해주세요."
        );
      } finally {
        if (isMounted.current) {
          setSaving(false);
        }
      }
    },
    500,
    []
  );

  const updateSettings = useCallback((newSettings: Partial<Setting>) => {
    if (!settings || !isMounted.current) return;

    const previousSettings = previousSettingsRef.current || settings;
    const fullSettings: Setting = {
      ...settings,
      ...newSettings,
    };

    // 변경된 필드 추출
    const changedFields = Object.keys(newSettings) as SettingField[];

    // 낙관적 업데이트: UI 즉시 반영
    setSettings(fullSettings);

    // API 호출은 500ms 디바운스
    setSaving(true);
    debouncedApiCall(fullSettings, previousSettings, changedFields);
  }, [settings, debouncedApiCall]);

  const handleSingleToggle = (
    field: keyof Pick<
      Setting,
      "messageNotification" | "commentNotification" | "postFeaturedNotification" | "friendSendNotification"
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
      friendSendNotification: enabled,
    });
  };

  // 탈퇴 모달 열기
  const handleOpenWithdrawModal = () => {
    setShowWithdrawModal(true);
  };

  // 탈퇴 모달 닫기
  const handleCloseWithdrawModal = () => {
    setShowWithdrawModal(false);
  };

  // 회원탈퇴 처리: 탈퇴 진행, 성공 시 홈으로 이동
  const handleConfirmWithdraw = async () => {
    try {
      setWithdrawing(true);
      const response = await userCommand.withdraw();
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
        // 실패 시 모달 닫고 에러 메시지 표시
        setShowWithdrawModal(false);
        showError(
          "회원탈퇴 실패",
          response.error || "회원탈퇴 중 오류가 발생했습니다. 다시 시도해주세요."
        );
      }
    } catch (error) {
      if (!isMounted.current) return;
      logger.error("회원탈퇴 실패:", error);
      // 실패 시 모달 닫고 에러 메시지 표시
      setShowWithdrawModal(false);
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

  // 전체 알림 상태 계산: true(모두 켜짐), false(모두 꺼짐), 'indeterminate'(일부만 켜짐)
  const allEnabled = settings
    ? settings.messageNotification &&
      settings.commentNotification &&
      settings.postFeaturedNotification &&
      settings.friendSendNotification
    : false;

  const allDisabled = settings
    ? !settings.messageNotification &&
      !settings.commentNotification &&
      !settings.postFeaturedNotification &&
      !settings.friendSendNotification
    : false;

  const isIndeterminate = settings ? !allEnabled && !allDisabled : false;

  return {
    settings,
    loading,
    saving,
    savingFields,
    savedFields,
    withdrawing,
    showWithdrawModal,
    error,
    allEnabled,
    isIndeterminate,
    handleSingleToggle,
    handleAllToggle,
    handleOpenWithdrawModal,
    handleCloseWithdrawModal,
    handleConfirmWithdraw,
    loadSettings,
  };
}
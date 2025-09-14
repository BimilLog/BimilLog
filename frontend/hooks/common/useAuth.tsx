"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuthStore } from "@/stores/auth.store";
import { useToastStore } from "@/stores/toast.store";
import { apiClient, Comment } from "@/lib/api";
import { logger } from '@/lib/utils/logger';

// ===== PASSWORD MODAL =====
export type PasswordModalMode = "post" | "comment";

export interface PasswordModalState {
  isOpen: boolean;
  password: string;
  title: string;
  mode: PasswordModalMode | null;
  targetComment: Comment | null;
}

export interface UsePasswordModalReturn {
  // State
  showPasswordModal: boolean;
  modalPassword: string;
  passwordModalTitle: string;
  deleteMode: PasswordModalMode | null;
  targetComment: Comment | null;

  // Actions
  openModal: (title: string, mode: PasswordModalMode, target?: Comment) => void;
  closeModal: () => void;
  setModalPassword: (password: string) => void;
  resetModal: () => void;

  // For legacy compatibility
  setShowPasswordModal: (show: boolean) => void;
  setPasswordModalTitle: (title: string) => void;
  setDeleteMode: (mode: PasswordModalMode | null) => void;
  setTargetComment: (comment: Comment | null) => void;
}

/**
 * 비밀번호 모달 상태를 관리하는 공통 훅
 * 게시글 삭제, 댓글 삭제 등에서 사용되는 비밀번호 모달 로직을 통합
 */
export const usePasswordModal = (): UsePasswordModalReturn => {
  const [modalState, setModalState] = useState<PasswordModalState>({
    isOpen: false,
    password: "",
    title: "",
    mode: null,
    targetComment: null,
  });

  const openModal = (title: string, mode: PasswordModalMode, target?: Comment) => {
    setModalState({
      isOpen: true,
      password: "",
      title,
      mode,
      targetComment: target || null,
    });
  };

  const closeModal = () => {
    setModalState(prev => ({
      ...prev,
      isOpen: false,
    }));
  };

  const setModalPassword = (password: string) => {
    setModalState(prev => ({
      ...prev,
      password,
    }));
  };

  const resetModal = () => {
    setModalState({
      isOpen: false,
      password: "",
      title: "",
      mode: null,
      targetComment: null,
    });
  };

  // Legacy compatibility methods
  const setShowPasswordModal = (show: boolean) => {
    setModalState(prev => ({
      ...prev,
      isOpen: show,
    }));
  };

  const setPasswordModalTitle = (title: string) => {
    setModalState(prev => ({
      ...prev,
      title,
    }));
  };

  const setDeleteMode = (mode: PasswordModalMode | null) => {
    setModalState(prev => ({
      ...prev,
      mode,
    }));
  };

  const setTargetComment = (comment: Comment | null) => {
    setModalState(prev => ({
      ...prev,
      targetComment: comment,
    }));
  };

  return {
    // State (legacy naming for compatibility)
    showPasswordModal: modalState.isOpen,
    modalPassword: modalState.password,
    passwordModalTitle: modalState.title,
    deleteMode: modalState.mode,
    targetComment: modalState.targetComment,

    // Actions
    openModal,
    closeModal,
    setModalPassword,
    resetModal,

    // Legacy compatibility
    setShowPasswordModal,
    setPasswordModalTitle,
    setDeleteMode,
    setTargetComment,
  };
};

// ===== AUTH CALLBACKS =====
/**
 * Kakao OAuth callback 처리 훅
 */
export const useKakaoCallback = () => {
  const [isProcessing, setIsProcessing] = useState(true);
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const processCallback = async () => {
      const code = searchParams.get("code");
      const error = searchParams.get("error");

      // 카카오에서 에러가 발생한 경우 (사용자 취소 등)
      if (error) {
        router.push(`/login?error=${encodeURIComponent(error)}`);
        return;
      }

      // Authorization Code가 없는 경우
      if (!code) {
        router.push("/login?error=no_code");
        return;
      }

      try {
        // Authorization Code를 백엔드로 전송하여 JWT 토큰 받기
        const response = await apiClient.post("/auth/callback", { code });

        if (response.success) {
          const data = response.data as { needsSignup?: boolean; tempUuid?: string };
          // 신규 사용자인 경우 회원가입 페이지로 이동 (임시 UUID 포함)
          if (data?.needsSignup) {
            router.push(`/signup?uuid=${data.tempUuid}`);
          } else {
            // 기존 사용자인 경우 메인 페이지로 이동
            router.push("/");
          }
        } else {
          router.push(`/login?error=${response.error || "login_failed"}`);
        }
      } catch (error) {
        logger.error("Callback processing error:", error);
        router.push("/login?error=callback_failed");
      } finally {
        setIsProcessing(false);
      }
    };

    processCallback();
  }, [searchParams, router]);

  return { isProcessing };
};

/**
 * 인증 에러 처리 훅
 */
export const useAuthError = () => {
  const [authError, setAuthError] = useState<string | null>(null);
  const searchParams = useSearchParams();

  useEffect(() => {
    const error = searchParams.get("error");
    if (error) {
      setAuthError(error);
    }
  }, [searchParams]);

  const clearAuthError = () => {
    setAuthError(null);
  };

  return { authError, clearAuthError };
};

/**
 * 회원가입 UUID 검증 훅
 */
export const useSignupUuid = () => {
  const [tempUuid, setTempUuid] = useState<string | null>(null);
  const [isValidating, setIsValidating] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const searchParams = useSearchParams();
  const router = useRouter();

  useEffect(() => {
    const validateUuid = async () => {
      const uuid = searchParams.get("uuid");

      // 회원가입 페이지인데 UUID가 없는 경우 (직접 접근 방지)
      if (!uuid) {
        setError("no_uuid");
        router.push("/login");
        return;
      }

      try {
        // 카카오 로그인 후 발급받은 임시 UUID의 유효성 검증
        // 만료되었거나 이미 사용된 UUID인지 백엔드에서 확인
        const response = await apiClient.get(`/auth/validate-uuid?uuid=${uuid}`);
        const data = response.data as { valid?: boolean };

        if (response.success && data?.valid) {
          // UUID가 유효하면 회원가입 진행 가능
          setTempUuid(uuid);
        } else {
          // UUID가 유효하지 않으면 로그인으로 리다이렉트
          setError("invalid_uuid");
          router.push("/login?error=invalid_signup_session");
        }
      } catch (error) {
        logger.error("UUID validation error:", error);
        setError("validation_failed");
        router.push("/login?error=signup_validation_failed");
      } finally {
        setIsValidating(false);
      }
    };

    validateUuid();
  }, [searchParams, router]);

  return { tempUuid, isValidating, error };
};

// ===== MAIN USE AUTH HOOK =====
export function useAuth() {
  const authStore = useAuthStore();
  const { showInfo } = useToastStore();

  // 초기 로드 시 사용자 정보 새로고침
  useEffect(() => {
    authStore.refreshUser();
  }, []);

  // needsRelogin 이벤트 리스너 - 토큰 만료나 인증 에러 시 전역에서 발생하는 커스텀 이벤트 처리
  useEffect(() => {
    const handleNeedsRelogin = (event: Event) => {
      const customEvent = event as CustomEvent;
      const { title, message } = customEvent.detail;

      // 사용자에게 재로그인이 필요함을 알림
      showInfo(title, message, 5000);

      // 인증 상태를 초기화하고 로그인 페이지로 리다이렉트
      authStore.handleNeedsRelogin(title, message);
    };

    // API 에러 핸들러에서 발생시키는 커스텀 이벤트를 전역으로 수신
    window.addEventListener("needsRelogin", handleNeedsRelogin);

    return () => {
      window.removeEventListener("needsRelogin", handleNeedsRelogin);
    };
  }, [showInfo, authStore.handleNeedsRelogin]);

  return {
    user: authStore.user,
    isLoading: authStore.isLoading,
    isAuthenticated: authStore.isAuthenticated,
    login: authStore.login,
    logout: authStore.logout,
    signUp: authStore.signUp,
    updateUserName: authStore.updateUserName,
    deleteAccount: authStore.deleteAccount,
    refreshUser: authStore.refreshUser,
  };
}
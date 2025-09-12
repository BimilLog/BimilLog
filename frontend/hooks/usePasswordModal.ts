import { useState } from "react";
import type { Comment } from "@/lib/api";

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
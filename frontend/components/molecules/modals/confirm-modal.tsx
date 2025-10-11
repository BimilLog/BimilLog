"use client";

import React from "react";
import { Modal, ModalHeader, ModalBody } from "flowbite-react";
import { Button, Spinner } from "@/components";
import { AlertCircle } from "lucide-react";

interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  confirmButtonVariant?: "default" | "destructive" | "outline" | "secondary" | "ghost" | "link";
  icon?: React.ReactNode;
  isLoading?: boolean;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = "확인",
  cancelText = "취소",
  confirmButtonVariant = "default",
  icon,
  isLoading = false,
}) => {
  return (
    <Modal
      show={isOpen}
      onClose={isLoading ? undefined : onClose}
      size="md"
      popup
      dismissible={!isLoading}
    >
      <ModalHeader />
      <ModalBody>
        <div className="text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-gray-100">
            {icon || <AlertCircle className="h-8 w-8 stroke-red-600 fill-red-100" />}
          </div>
          <h3 className="mb-5 text-lg font-bold text-gray-900">
            {title}
          </h3>
          <p className="mb-5 text-sm text-gray-500 whitespace-pre-line">
            {message}
          </p>
          <div className="flex justify-center gap-4">
            <Button
              variant="outline"
              onClick={onClose}
              disabled={isLoading}
            >
              {cancelText}
            </Button>
            <Button
              variant={confirmButtonVariant}
              onClick={onConfirm}
              disabled={isLoading}
            >
              {isLoading ? (
                <span className="flex items-center gap-2">
                  <Spinner size="sm" />
                  처리 중...
                </span>
              ) : (
                confirmText
              )}
            </Button>
          </div>
        </div>
      </ModalBody>
    </Modal>
  );
};

// Hook for easy confirm modal usage
export const useConfirmModal = () => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [config, setConfig] = React.useState<Partial<ConfirmModalProps>>({});
  const resolveRef = React.useRef<((value: boolean) => void) | null>(null);

  const confirm = React.useCallback((modalConfig: Partial<ConfirmModalProps> = {}) => {
    return new Promise<boolean>((resolve) => {
      setConfig(modalConfig);
      setIsOpen(true);
      resolveRef.current = resolve;
    });
  }, []);

  const handleConfirm = React.useCallback(() => {
    resolveRef.current?.(true);
    setIsOpen(false);
  }, []);

  const handleClose = React.useCallback(() => {
    resolveRef.current?.(false);
    setIsOpen(false);
  }, []);

  const ConfirmModalComponent = React.useCallback(() => (
    <ConfirmModal
      isOpen={isOpen}
      onClose={handleClose}
      onConfirm={handleConfirm}
      title={config.title || "확인"}
      message={config.message || "계속하시겠습니까?"}
      confirmText={config.confirmText}
      cancelText={config.cancelText}
      confirmButtonVariant={config.confirmButtonVariant}
      icon={config.icon}
    />
  ), [isOpen, config, handleClose, handleConfirm]);

  return {
    confirm,
    ConfirmModalComponent
  };
};
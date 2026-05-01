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

/**
 * 확인 다이얼로그 (라운드 16 F-16-038):
 * - paper 토큰 일괄 적용 (bg-gray-100 / text-gray-900 / text-gray-500 → paper-aged + ink/ink-soft)
 * - 라운드 1 메타포 일관 (다크 변형 포함)
 */
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
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-paper-aged border-2 border-dashed border-stamp-red/40 dark:bg-stamp-red/15">
            {icon || (
              <AlertCircle
                className="h-8 w-8 stroke-stamp-red"
                aria-hidden="true"
              />
            )}
          </div>
          <h3 className="mb-5 text-lg font-bold font-display text-ink dark:text-foreground break-keep">
            {title}
          </h3>
          <p className="mb-5 text-sm text-ink-soft dark:text-muted-foreground whitespace-pre-line break-keep">
            {message}
          </p>
          <div className="flex justify-center gap-4">
            <Button
              variant="outline"
              onClick={onClose}
              disabled={isLoading}
              className="min-h-[44px]"
            >
              {cancelText}
            </Button>
            <Button
              variant={confirmButtonVariant}
              onClick={onConfirm}
              disabled={isLoading}
              className="min-h-[44px]"
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

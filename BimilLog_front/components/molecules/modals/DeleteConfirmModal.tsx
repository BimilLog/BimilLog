"use client";

import { Modal, ModalHeader, ModalBody, Button } from 'flowbite-react';
import { AlertCircle, Loader2 } from 'lucide-react';

interface DeleteConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title?: string;
  message?: string;
  confirmText?: string;
  cancelText?: string;
  isLoading?: boolean;
}

export function DeleteConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title = "정말 삭제하시겠습니까?",
  message = "이 작업은 되돌릴 수 없습니다. 정말 삭제하시겠습니까?",
  confirmText = "삭제",
  cancelText = "취소",
  isLoading = false,
}: DeleteConfirmModalProps) {
  return (
    <Modal show={isOpen} size="md" onClose={onClose} popup>
      <ModalHeader />
      <ModalBody>
        {/* 라운드 8: gray-* 하드코딩 → ink/paper 토큰 + stamp-red 메타포 */}
        <div className="text-center">
          <AlertCircle
            className="mx-auto mb-4 h-14 w-14 stroke-stamp-red dark:stroke-stamp-red/80"
            aria-hidden="true"
          />
          <h3 className="mb-5 text-lg font-semibold text-ink dark:text-paper-50 break-keep">
            {title}
          </h3>
          <p className="mb-5 text-sm text-ink-soft dark:text-paper-200 break-keep whitespace-pre-line">
            {message}
          </p>
          <div className="flex justify-center gap-4">
            <Button
              color="failure"
              onClick={onConfirm}
              disabled={isLoading}
            >
              {isLoading ? (
                <div className="flex items-center">
                  <Loader2 className="w-4 h-4 animate-spin mr-2" aria-hidden="true" />
                  삭제 중...
                </div>
              ) : (
                confirmText
              )}
            </Button>
            <Button
              color="gray"
              onClick={onClose}
              disabled={isLoading}
            >
              {cancelText}
            </Button>
          </div>
        </div>
      </ModalBody>
    </Modal>
  );
}
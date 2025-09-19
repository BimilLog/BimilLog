"use client";

import { Modal, ModalHeader, ModalBody, Button } from 'flowbite-react';
import { HiOutlineExclamationCircle } from 'react-icons/hi';

interface DeleteConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title?: string;
  message?: string;
  confirmText?: string;
  cancelText?: string;
}

export function DeleteConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title = "정말 삭제하시겠습니까?",
  message = "이 작업은 되돌릴 수 없습니다. 정말 삭제하시겠습니까?",
  confirmText = "삭제",
  cancelText = "취소"
}: DeleteConfirmModalProps) {
  return (
    <Modal show={isOpen} size="md" onClose={onClose} popup>
      <ModalHeader />
      <ModalBody>
        <div className="text-center">
          <HiOutlineExclamationCircle className="mx-auto mb-4 h-14 w-14 text-gray-400 dark:text-gray-200" />
          <h3 className="mb-5 text-lg font-normal text-gray-500 dark:text-gray-400">
            {title}
          </h3>
          <p className="mb-5 text-sm text-gray-500 dark:text-gray-400">
            {message}
          </p>
          <div className="flex justify-center gap-4">
            <Button color="failure" onClick={onConfirm}>
              {confirmText}
            </Button>
            <Button color="gray" onClick={onClose}>
              {cancelText}
            </Button>
          </div>
        </div>
      </ModalBody>
    </Modal>
  );
}
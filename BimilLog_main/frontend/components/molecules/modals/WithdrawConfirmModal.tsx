"use client";

import React, { useState, useEffect } from "react";
import { Modal, ModalHeader, ModalBody } from "flowbite-react";
import { Button, Input } from "@/components";
import { AlertTriangle, X } from "lucide-react";

interface WithdrawConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isProcessing?: boolean;
}

export const WithdrawConfirmModal: React.FC<WithdrawConfirmModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  isProcessing = false,
}) => {
  const [confirmText, setConfirmText] = useState("");
  const CONFIRM_KEYWORD = "탈퇴하기";
  const isConfirmValid = confirmText === CONFIRM_KEYWORD;

  // 모달이 열릴 때마다 입력 텍스트 초기화
  useEffect(() => {
    if (isOpen) {
      setConfirmText("");
    }
  }, [isOpen]);

  const handleConfirm = () => {
    if (isConfirmValid && !isProcessing) {
      onConfirm();
    }
  };

  const handleClose = () => {
    if (!isProcessing) {
      onClose();
    }
  };

  return (
    <Modal
      show={isOpen}
      onClose={handleClose}
      size="md"
      popup
      dismissible={!isProcessing}
    >
      <ModalHeader />
      <ModalBody>
        <div className="text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
            <AlertTriangle className="h-10 w-10 stroke-red-600 fill-red-50" />
          </div>

          <h3 className="mb-3 text-xl font-bold text-gray-900">
            정말로 탈퇴하시겠습니까?
          </h3>

          <div className="mb-6 text-left">
            <p className="mb-3 text-sm text-gray-700 font-medium">
              회원 탈퇴 시 다음 데이터가 영구적으로 삭제됩니다:
            </p>
            <ul className="space-y-2 text-sm text-gray-600">
              <li className="flex items-start gap-2">
                <X className="w-4 h-4 text-red-500 mt-0.5 flex-shrink-0" />
                <span>작성한 모든 게시글 및 댓글</span>
              </li>
              <li className="flex items-start gap-2">
                <X className="w-4 h-4 text-red-500 mt-0.5 flex-shrink-0" />
                <span>받은 롤링페이퍼 메시지</span>
              </li>
              <li className="flex items-start gap-2">
                <X className="w-4 h-4 text-red-500 mt-0.5 flex-shrink-0" />
                <span>알림 설정 및 기록</span>
              </li>
              <li className="flex items-start gap-2">
                <X className="w-4 h-4 text-red-500 mt-0.5 flex-shrink-0" />
                <span>계정 정보</span>
              </li>
            </ul>
          </div>

          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-left">
            <p className="text-sm text-red-800 font-semibold">
              ⚠️ 이 작업은 되돌릴 수 없습니다
            </p>
            <p className="text-xs text-red-700 mt-1">
              삭제된 데이터는 복구할 수 없습니다.
            </p>
          </div>

          <div className="mb-6 text-left">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              계속하려면 <span className="font-bold text-red-600">&quot;{CONFIRM_KEYWORD}&quot;</span>를 입력하세요
            </label>
            <Input
              type="text"
              placeholder={CONFIRM_KEYWORD}
              value={confirmText}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                setConfirmText(e.target.value)
              }
              disabled={isProcessing}
              autoFocus
              className="text-center font-medium"
            />
          </div>

          <div className="flex justify-center gap-3">
            <Button
              variant="outline"
              onClick={handleClose}
              disabled={isProcessing}
              className="min-w-[100px]"
            >
              취소
            </Button>
            <Button
              variant="destructive"
              onClick={handleConfirm}
              disabled={!isConfirmValid || isProcessing}
              className="min-w-[100px] bg-red-600 hover:bg-red-700"
            >
              {isProcessing ? "처리 중..." : "탈퇴하기"}
            </Button>
          </div>
        </div>
      </ModalBody>
    </Modal>
  );
};

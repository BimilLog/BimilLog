"use client";

import React, { useState, useEffect } from "react";
import { Modal, ModalHeader, ModalBody } from "flowbite-react";
import { Button, Input } from "@/components";
import { AlertTriangle, Mail, X } from "lucide-react";

interface WithdrawConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isProcessing?: boolean;
}

/**
 * 회원 탈퇴 확인 모달.
 *
 * B-305 (라운드 10):
 * - paper/ink/stamp-red 토큰으로 다크 모드 일관성 회복.
 * - 종이/편지 메타포 카피 ("이 편지함을 영원히 닫을까요?") — 라운드 3 useGoodbyeFarewell 톤.
 * - "탈퇴하기" 직접 입력 가드는 destructive 패턴으로 유지.
 */
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
      theme={{
        content: {
          inner: "relative flex max-h-[90dvh] flex-col rounded-lg bg-paper-50 dark:bg-paper-900 shadow",
          base: "relative h-full w-full p-4 md:h-auto",
        },
      }}
    >
      <ModalHeader className="!bg-paper-50 dark:!bg-paper-900 !border-postal-navy/20" />
      <ModalBody className="!bg-paper-50 dark:!bg-paper-900">
        <div className="text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-stamp-red/15 dark:bg-stamp-red/25">
            <Mail className="h-9 w-9 text-stamp-red" aria-hidden="true" />
          </div>

          <h3
            id="withdraw-modal-title"
            className="mb-3 text-xl font-bold text-ink-900 dark:text-ink-100 break-keep"
          >
            이 편지함을 영원히 닫을까요?
          </h3>

          <div className="mb-6 text-left">
            <p className="mb-3 text-sm text-ink-soft dark:text-ink-300 font-medium break-keep">
              회원 탈퇴를 진행하면 다음 자취가 모두 사라져요:
            </p>
            <ul className="space-y-2 text-sm text-ink-soft dark:text-ink-300">
              <li className="flex items-start gap-2">
                <X className="w-4 h-4 text-stamp-red mt-0.5 flex-shrink-0" aria-hidden="true" />
                <span className="break-keep">주고받은 편지의 흔적 (게시글, 댓글)</span>
              </li>
              <li className="flex items-start gap-2">
                <X className="w-4 h-4 text-stamp-red mt-0.5 flex-shrink-0" aria-hidden="true" />
                <span className="break-keep">받은 모든 롤링페이퍼 편지</span>
              </li>
              <li className="flex items-start gap-2">
                <X className="w-4 h-4 text-stamp-red mt-0.5 flex-shrink-0" aria-hidden="true" />
                <span className="break-keep">알림 설정과 활동 기록</span>
              </li>
              <li className="flex items-start gap-2">
                <X className="w-4 h-4 text-stamp-red mt-0.5 flex-shrink-0" aria-hidden="true" />
                <span className="break-keep">계정 정보</span>
              </li>
            </ul>
          </div>

          <div className="mb-6 p-4 bg-stamp-red/10 dark:bg-stamp-red/20 border border-stamp-red/30 dark:border-stamp-red/40 rounded-lg text-left">
            <p className="text-sm text-stamp-red font-semibold flex items-center gap-2 break-keep">
              <AlertTriangle className="w-4 h-4 flex-shrink-0" aria-hidden="true" />
              한 번 닫은 편지함은 다시 열 수 없어요
            </p>
            <p className="text-xs text-stamp-red/90 dark:text-stamp-red mt-1 break-keep">
              삭제된 편지는 복구할 수 없어요.
            </p>
          </div>

          <div className="mb-6 text-left">
            <label
              htmlFor="withdraw-confirm-input"
              className="block text-sm font-medium text-ink-900 dark:text-ink-100 mb-2 break-keep"
            >
              마지막 인사로{" "}
              <span className="font-bold text-stamp-red">&quot;{CONFIRM_KEYWORD}&quot;</span>
              를 또박또박 적어주세요
            </label>
            <Input
              id="withdraw-confirm-input"
              type="text"
              placeholder={CONFIRM_KEYWORD}
              value={confirmText}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                setConfirmText(e.target.value)
              }
              disabled={isProcessing}
              autoFocus
              className="text-center font-medium"
              aria-describedby="withdraw-confirm-help"
            />
            <span id="withdraw-confirm-help" className="sr-only">
              회원 탈퇴를 확정하려면 &quot;{CONFIRM_KEYWORD}&quot; 라는 단어를 그대로 입력하세요.
            </span>
          </div>

          <div className="flex justify-center gap-3">
            <Button
              variant="outline"
              onClick={handleClose}
              disabled={isProcessing}
              className="min-w-[100px] border-postal-navy/30 text-postal-navy hover:bg-postal-navy/10 dark:border-postal-navy/50 dark:text-ink-100 dark:hover:bg-postal-navy/20"
            >
              취소
            </Button>
            <Button
              variant="destructive"
              onClick={handleConfirm}
              disabled={!isConfirmValid || isProcessing}
              className="min-w-[100px] bg-stamp-red hover:bg-stamp-red-deep text-white dark:bg-stamp-red dark:hover:bg-stamp-red-deep"
            >
              {isProcessing ? "처리 중..." : "탈퇴하기"}
            </Button>
          </div>
        </div>
      </ModalBody>
    </Modal>
  );
};

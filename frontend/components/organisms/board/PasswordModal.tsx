import React from "react";
import { Modal, ModalHeader, ModalBody, ModalFooter } from "flowbite-react";
import { Button, Input } from "@/components";
import { Loader2 } from "lucide-react";

interface PasswordModalProps {
  isOpen: boolean;
  password: string;
  onPasswordChange: (password: string) => void;
  onConfirm: () => void;
  onCancel: () => void;
  title: string;
  error?: string;
  isLoading?: boolean;
}

export const PasswordModal = React.memo<PasswordModalProps>(({
  isOpen,
  password,
  onPasswordChange,
  onConfirm,
  onCancel,
  title,
  error,
  isLoading = false,
}) => {
  return (
    <Modal show={isOpen} onClose={onCancel} size="sm">
      <ModalHeader>{title}</ModalHeader>
      <ModalBody>
        <div className="space-y-4">
          <Input
            type="password"
            placeholder="비밀번호 (1000~9999)"
            value={password}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => onPasswordChange(e.target.value)}
            autoFocus
            disabled={isLoading}
          />
          {error && (
            <div className="text-sm text-red-600 mt-2">
              {error}
            </div>
          )}
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          variant="outline"
          onClick={onCancel}
          disabled={isLoading}
        >
          취소
        </Button>
        <Button
          onClick={onConfirm}
          disabled={isLoading}
        >
          {isLoading ? (
            <div className="flex items-center">
              <Loader2 className="w-4 h-4 animate-spin mr-2" />
              처리 중...
            </div>
          ) : (
            "확인"
          )}
        </Button>
      </ModalFooter>
    </Modal>
  );
});
PasswordModal.displayName = "PasswordModal";

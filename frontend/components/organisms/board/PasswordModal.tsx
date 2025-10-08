import React from "react";
import { Modal, ModalHeader, ModalBody, ModalFooter } from "flowbite-react";
import { Button, Input } from "@/components";

interface PasswordModalProps {
  isOpen: boolean;
  password: string;
  onPasswordChange: (password: string) => void;
  onConfirm: () => void;
  onCancel: () => void;
  title: string;
}

export const PasswordModal = React.memo<PasswordModalProps>(({
  isOpen,
  password,
  onPasswordChange,
  onConfirm,
  onCancel,
  title,
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
          />
        </div>
      </ModalBody>
      <ModalFooter>
        <Button variant="outline" onClick={onCancel}>
          취소
        </Button>
        <Button onClick={onConfirm}>확인</Button>
      </ModalFooter>
    </Modal>
  );
});
PasswordModal.displayName = "PasswordModal";

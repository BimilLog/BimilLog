import React, { useId } from "react";
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
  description?: string;
  error?: string;
  isLoading?: boolean;
}

/**
 * 라운드 8 B-8-007: <form> 래핑으로 Enter 키 제출 + 모바일 키보드 "확인" 동작 활성화.
 * inputMode="numeric" + pattern 으로 모바일 숫자 키패드 노출.
 * description / error 도 paper/ink 다크 토큰으로 통일.
 */
export const PasswordModal = React.memo<PasswordModalProps>(({
  isOpen,
  password,
  onPasswordChange,
  onConfirm,
  onCancel,
  title,
  description,
  error,
  isLoading = false,
}) => {
  const descId = useId();
  const errorId = useId();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isLoading) return;
    if (!/^[1-9]\d{3}$/.test(password)) return;
    onConfirm();
  };

  return (
    <Modal show={isOpen} onClose={onCancel} size="sm">
      <ModalHeader>{title}</ModalHeader>
      <form
        onSubmit={handleSubmit}
        aria-label={`${title} 비밀번호 입력 폼`}
        aria-describedby={description ? descId : undefined}
      >
        <ModalBody>
          <div className="space-y-4">
            {description && (
              <p id={descId} className="text-sm text-ink-soft dark:text-paper-200 break-keep">
                {description}
              </p>
            )}
            <label htmlFor="password-modal-input" className="sr-only">
              비밀번호 (1000~9999)
            </label>
            <Input
              id="password-modal-input"
              type="password"
              placeholder="비밀번호 (1000~9999)"
              value={password}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                onPasswordChange(e.target.value.replace(/\D/g, "").slice(0, 4))
              }
              autoFocus
              disabled={isLoading}
              inputMode="numeric"
              pattern="[1-9][0-9]{3}"
              maxLength={4}
              autoComplete="off"
              aria-invalid={!!error || undefined}
              aria-describedby={error ? errorId : undefined}
            />
            {error && (
              <div id={errorId} role="alert" className="text-sm text-stamp-red mt-2 break-keep">
                {error}
              </div>
            )}
          </div>
        </ModalBody>
        <ModalFooter>
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={isLoading}
          >
            취소
          </Button>
          <Button
            type="submit"
            disabled={isLoading || !/^[1-9]\d{3}$/.test(password)}
          >
            {isLoading ? (
              <div className="flex items-center">
                <Loader2 className="w-4 h-4 animate-spin mr-2" aria-hidden="true" />
                처리 중...
              </div>
            ) : (
              "확인"
            )}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
});
PasswordModal.displayName = "PasswordModal";

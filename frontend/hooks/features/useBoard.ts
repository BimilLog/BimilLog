import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth, useToast } from "@/hooks";
import { postCommand } from "@/lib/api";
import { stripHtml, validatePassword } from "@/lib/utils";

// Write Form Hook
export const useWriteForm = () => {
  const { user, isAuthenticated } = useAuth();
  const router = useRouter();
  const { showSuccess, showError, showWarning } = useToast();
  
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPreview, setIsPreview] = useState(false);

  const handleSubmit = async () => {
    const plainContent = stripHtml(content).trim();

    if (!title.trim() || !plainContent) {
      showWarning("입력 확인", "제목과 내용을 모두 입력해주세요.");
      return;
    }

    let validatedPassword: number | undefined;
    try {
      validatedPassword = validatePassword(password, isAuthenticated);
    } catch (error) {
      if (error instanceof Error) {
        showWarning("입력 확인", error.message);
      }
      return;
    }

    setIsSubmitting(true);
    try {
      const postData = {
        userName: isAuthenticated ? user!.userName : null,
        title: title.trim(),
        content: plainContent,
        password: validatedPassword,
      };

      const response = await postCommand.create(postData);
      if (response.success && response.data) {
        showSuccess("작성 완료", "게시글이 성공적으로 작성되었습니다!");
        router.push(`/board/post/${response.data.id}`);
      }
    } catch (error) {
      showError("작성 실패", "게시글 작성 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const isFormValid = Boolean(title.trim() && content.trim());

  return {
    title,
    setTitle,
    content,
    setContent,
    password,
    setPassword,
    isSubmitting,
    isPreview,
    setIsPreview,
    handleSubmit,
    isFormValid,
    user,
    isAuthenticated,
  };
};

// Password Modal Hook
export interface PasswordModalState {
  isOpen: boolean;
  title: string;
  mode: "post" | "comment" | null;
  targetComment?: any;
  password: string;
}

export type PasswordModalMode = "post" | "comment";

export interface UsePasswordModalReturn {
  modalState: PasswordModalState;
  openModal: (title: string, mode: PasswordModalMode, target?: any) => void;
  closeModal: () => void;
  setPassword: (password: string) => void;
  submitPassword: (onSubmit: (password: string, mode: PasswordModalMode, target?: any) => void) => void;
}

export const usePasswordModal = (): UsePasswordModalReturn => {
  const [modalState, setModalState] = useState<PasswordModalState>({
    isOpen: false,
    title: "",
    mode: null,
    targetComment: undefined,
    password: "",
  });

  const openModal = (title: string, mode: PasswordModalMode, target?: any) => {
    setModalState({
      isOpen: true,
      title,
      mode,
      targetComment: target,
      password: "",
    });
  };

  const closeModal = () => {
    setModalState({
      isOpen: false,
      title: "",
      mode: null,
      targetComment: undefined,
      password: "",
    });
  };

  const setPassword = (password: string) => {
    setModalState(prev => ({ ...prev, password }));
  };

  const submitPassword = (onSubmit: (password: string, mode: PasswordModalMode, target?: any) => void) => {
    if (modalState.mode) {
      onSubmit(modalState.password, modalState.mode, modalState.targetComment);
    }
    closeModal();
  };

  return {
    modalState,
    openModal,
    closeModal,
    setPassword,
    submitPassword,
  };
};
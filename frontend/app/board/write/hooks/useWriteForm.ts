import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { boardApi } from "@/lib/api";

const stripHtml = (html: string) => {
  // <br> 태그를 줄바꿈으로 변환
  let result = html.replace(/<br\s*\/?>/gi, '\n');
  // <p> 태그 끝을 줄바꿈으로 변환
  result = result.replace(/<\/p>/gi, '\n');
  // 다른 HTML 태그들 제거
  result = result.replace(/<[^>]*>?/gm, '');
  // 연속된 줄바꿈을 정리 (3개 이상을 2개로)
  result = result.replace(/\n{3,}/g, '\n\n');
  return result;
};

export const useWriteForm = () => {
  const { user, isAuthenticated } = useAuth();
  const router = useRouter();
  
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPreview, setIsPreview] = useState(false);

  const handleSubmit = async () => {
    const plainContent = stripHtml(content).trim();

    if (!title.trim() || !plainContent) {
      alert("제목과 내용을 모두 입력해주세요.");
      return;
    }

    if (!isAuthenticated && !password) {
      alert("비회원은 비밀번호를 입력해야 합니다.");
      return;
    }

    if (password && !/^[0-9]+$/.test(password)) {
      alert("비밀번호는 숫자만 입력 가능합니다.");
      return;
    }

    setIsSubmitting(true);
    try {
      const postData: {
        userName: string | null;
        title: string;
        content: string;
        password?: number;
      } = {
        userName: isAuthenticated ? user!.userName : null,
        title: title.trim(),
        content: plainContent,
      };

      if (!isAuthenticated && password) {
        postData.password = Number.parseInt(password);
      }

      const response = await boardApi.createPost(postData);
      if (response.success && response.data) {
        alert("게시글이 성공적으로 작성되었습니다!");
        router.push(`/board/post/${response.data.id}`);
      }
    } catch (error) {
      console.error("Failed to create post:", error);
      alert("게시글 작성 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const isFormValid = Boolean(title.trim() && content.trim());

  return {
    // Form states
    title,
    setTitle,
    content,
    setContent,
    password,
    setPassword,
    isSubmitting,
    isPreview,
    setIsPreview,
    
    // Form actions
    handleSubmit,
    isFormValid,
    
    // User info
    user,
    isAuthenticated,
  };
}; 
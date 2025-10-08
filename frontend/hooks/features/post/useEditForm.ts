"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import { useAuth, useToast } from "@/hooks";
import { postQuery, postCommand, type Post } from "@/lib/api";
import { stripHtml, validatePassword } from "@/lib/utils";

/**
 * 게시글 수정 폼을 위한 통합 훅
 * edit/page.tsx의 비즈니스 로직을 추출하여 재사용 가능하게 만듦
 */
export function useEditForm() {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const params = useParams();
  const { showSuccess, showError, showWarning } = useToast();

  // 게시글 ID 추출
  const [postId, setPostId] = useState<number | null>(null);

  useEffect(() => {
    if (params.id) {
      setPostId(Number.parseInt(params.id as string));
    }
  }, [params]);

  // Post 상태
  const [post, setPost] = useState<Post | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPreview, setIsPreview] = useState(false);

  // 비회원 게시글 수정 상태
  const [isGuest, setIsGuest] = useState(false);
  const [isAuthorized, setIsAuthorized] = useState(false);
  const [guestPassword, setGuestPassword] = useState("");

  // 게시글 정보 조회
  const fetchPost = useCallback(async () => {
    if (!postId) return;

    try {
      const response = await postQuery.getById(postId);
      if (response.success && response.data) {
        const postData = response.data;
        setPost(postData);
        setTitle(postData.title);
        setContent(postData.content);

        // memberId가 null 또는 0이면 비회원이 작성한 게시글
        const isGuestPost = postData.memberId === null || postData.memberId === 0;
        setIsGuest(isGuestPost);

        // 회원 글일 경우 작성자만 권한 부여
        if (!isGuestPost) {
          if (isAuthenticated && user?.memberId === postData.memberId) {
            setIsAuthorized(true);
          } else {
            // 회원 글인데 다른 사용자가 접근한 경우
            showError("권한 없음", "수정 권한이 없습니다.");
            router.push(`/board/post/${postId}`);
          }
        } else {
          // 비회원 글의 경우 바로 수정 화면으로 이동 (비밀번호는 수정 시 검증)
          setIsAuthorized(true);
        }
      } else {
        showError("게시글 없음", "게시글을 찾을 수 없습니다.");
        router.push("/board");
      }
    } catch {
      showError("오류", "게시글을 불러오는 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
    }
  }, [postId, router, isAuthenticated, user, showError]);

  // 초기 로드
  useEffect(() => {
    fetchPost();
  }, [fetchPost]);

  // 폼 유효성 검사
  const validateForm = () => {
    const plainContent = stripHtml(content).trim();

    if (!title.trim() || !plainContent) {
      showWarning("입력 확인", "제목과 내용을 입력해주세요.");
      return false;
    }

    // 비회원 게시글의 경우 비밀번호 검증
    if (isGuest) {
      try {
        validatePassword(guestPassword, false);
      } catch (error) {
        if (error instanceof Error) {
          showWarning("비밀번호 확인", error.message);
        }
        return false;
      }
    }

    return true;
  };

  // 게시글 수정 제출
  const handleSubmit = async () => {
    if (!validateForm() || !post) return;

    const plainContent = stripHtml(content).trim();

    setIsSubmitting(true);
    try {
      const updatedPost: Post = {
        ...post,
        title: title.trim(),
        content: plainContent,
        password: isGuest ? Number(guestPassword) : undefined,
      };

      const response = await postCommand.update(updatedPost);
      if (response.success) {
        showSuccess("수정 완료", "게시글이 성공적으로 수정되었습니다!");
        router.push(`/board/post/${postId}`);
      } else {
        // 서버 응답의 에러 메시지 확인
        if (
          response.error &&
          response.error.includes("게시글 비밀번호가 일치하지 않습니다")
        ) {
          showError("비밀번호 오류", "비밀번호가 일치하지 않습니다.");
        } else {
          showError(
            "수정 실패",
            response.error || "게시글 수정에 실패했습니다."
          );
        }
      }
    } catch (error) {
      // HTTP 상태 코드 기반 에러 처리
      if (error instanceof Error && error.message.includes("403")) {
        showError("비밀번호 오류", "비밀번호가 일치하지 않습니다.");
      } else {
        showError("수정 실패", "게시글 수정 중 오류가 발생했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // 폼 유효성 상태
  const isFormValid = Boolean(title.trim() && content.trim());

  return {
    // Post data
    post,
    postId,
    isLoading: isLoading || authLoading,
    isAuthorized,

    // Form fields
    title,
    setTitle,
    content,
    setContent,
    guestPassword,
    setGuestPassword,
    isGuest,

    // Form states
    isPreview,
    setIsPreview,
    isSubmitting,
    isFormValid,

    // Actions
    handleSubmit,
    validateForm,
  };
}
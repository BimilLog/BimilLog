"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import { useAuth, useToast } from "@/hooks";
import { postQuery, type Post } from "@/lib/api";
import { stripHtml, validatePassword } from "@/lib/utils";
import { useUpdatePostAction } from "@/hooks/actions/usePostActions";

/**
 * 게시글 수정 폼을 위한 통합 훅
 * edit/page.tsx의 비즈니스 로직을 추출하여 재사용 가능하게 만듦
 */
interface UseEditFormOptions {
  initialPost?: Post | null;
  initialPostId?: number;
}

export function useEditForm(options?: UseEditFormOptions) {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const params = useParams();
  const { showError, showWarning } = useToast();
  const { updatePost, isPending: isUpdatePending } = useUpdatePostAction();

  // 게시글 ID 추출
  const resolvedPostId = options?.initialPostId ?? (params.id ? Number.parseInt(params.id as string) : null);
  const [postId, setPostId] = useState<number | null>(resolvedPostId);

  useEffect(() => {
    if (!postId && params.id) {
      setPostId(Number.parseInt(params.id as string));
    }
  }, [params, postId]);

  const hasInitialPost = !!options?.initialPost;

  // Post 상태
  const [post, setPost] = useState<Post | null>(options?.initialPost ?? null);
  const [title, setTitle] = useState(options?.initialPost?.title ?? "");
  const [content, setContent] = useState(options?.initialPost?.content ?? "");
  const [isLoading, setIsLoading] = useState(!hasInitialPost);
  const [isPreview, setIsPreview] = useState(false);

  // 비회원 게시글 수정 상태
  const initIsGuest = options?.initialPost ? (options.initialPost.memberId === null || options.initialPost.memberId === 0) : false;
  const [isGuest, setIsGuest] = useState(initIsGuest);
  const [isAuthorized, setIsAuthorized] = useState(hasInitialPost && initIsGuest);
  const [guestPassword, setGuestPassword] = useState("");

  // SSR initialPost 권한 체크
  useEffect(() => {
    if (!hasInitialPost || authLoading) return;
    const postData = options!.initialPost!;
    const isGuestPost = postData.memberId === null || postData.memberId === 0;
    if (isGuestPost) {
      setIsAuthorized(true);
    } else if (isAuthenticated && user?.memberId === postData.memberId) {
      setIsAuthorized(true);
    } else {
      showError("권한 없음", "수정 권한이 없습니다.");
      router.push(`/board/post/${postId}`);
    }
  }, [hasInitialPost, authLoading, isAuthenticated, user]);

  // 게시글 정보 조회 (initialPost 없을 때만)
  const fetchPost = useCallback(async () => {
    if (hasInitialPost || !postId || authLoading) return;

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
  }, [hasInitialPost, postId, router, isAuthenticated, user, showError, authLoading]);

  // 초기 로드 (initialPost 없을 때만)
  useEffect(() => {
    if (!hasInitialPost) fetchPost();
  }, [fetchPost, hasInitialPost]);

  // 폼 유효성 검사
  const validateForm = () => {
    const plainContent = stripHtml(content).trim();

    if (!title.trim() || !plainContent) {
      showWarning("입력 확인", "제목과 내용을 입력해주세요.");
      return false;
    }

    // 비회원 게시글의 경우 비밀번호 입력 확인
    if (isGuest && !guestPassword.trim()) {
      showWarning("비밀번호 확인", "비밀번호를 입력해주세요.");
      return false;
    }

    return true;
  };

  // 게시글 수정 제출
  const handleSubmit = async () => {
    if (!validateForm() || !post || !postId) return;

    const plainContent = stripHtml(content).trim();

    let validatedPassword: number | undefined = undefined;
    if (isGuest) {
      validatedPassword = validatePassword(guestPassword, false);
    }

    updatePost({
      postId,
      title: title.trim(),
      content: plainContent,
      password: validatedPassword,
    });
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
    isSubmitting: isUpdatePending,
    isFormValid,

    // Actions
    handleSubmit,
    validateForm,
  };
}
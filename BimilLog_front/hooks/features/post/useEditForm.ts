"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { useRouter, useParams } from "next/navigation";
import { useAuth, useToast } from "@/hooks";
import { postQuery, type Post } from "@/lib/api";
import { stripHtml, validatePassword } from "@/lib/utils";
import { useUpdatePostAction } from "@/hooks/actions/usePostActions";
import { useDraft } from "@/hooks/features/useDraft";

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
  // hasInitialPost / options.initialPost / postId / router / showError 모두 의존성에 명시
  // (eslint react-hooks/exhaustive-deps 경고 해소 + 시드/실 운영 ID 변경 시 재평가 보장)
  useEffect(() => {
    if (!hasInitialPost || authLoading) return;
    const postData = options?.initialPost;
    if (!postData) return;
    const isGuestPost = postData.memberId === null || postData.memberId === 0;
    if (isGuestPost) {
      setIsAuthorized(true);
    } else if (isAuthenticated && user?.memberId === postData.memberId) {
      setIsAuthorized(true);
    } else {
      showError("권한 없음", "수정 권한이 없습니다.");
      router.push(`/board/post/${postId}`);
    }
  }, [hasInitialPost, authLoading, isAuthenticated, user, options?.initialPost, postId, router, showError]);

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

  // B-7-004: 수정 모드 임시저장 (postId 분기로 별도 슬롯 사용)
  const {
    isAutoSaving,
    lastSavedAt,
    saveDraftManual,
    handleAutoSave,
    removeDraft,
    formatLastSaved,
  } = useDraft({
    postId: postId ?? undefined,
    enabled: !!postId,
    autoSave: true,
  });

  // 자동저장 트리거 - prefill 이후 dirty 상태에서만 저장
  const dirtyRef = useRef(false);
  useEffect(() => {
    if (!post) return;
    // 초기 prefill 대비 변경 감지 (initialPost 와 동일하면 dirty 아님)
    if (title !== post.title || content !== post.content) {
      dirtyRef.current = true;
    }
    if (dirtyRef.current && (title || content)) {
      handleAutoSave(title, content);
    }
  }, [title, content, handleAutoSave, post]);

  // B-7-005 보조: 작성/수정 중 이탈 가드 (브라우저 beforeunload)
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (!dirtyRef.current || isUpdatePending) return;
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [isUpdatePending]);

  // 폼 유효성 검사
  const validateForm = () => {
    // 길이 체크는 plain text 기준이지만 백엔드 전송은 HTML 원본 사용 (B-7-001)
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
  // B-7-001 (CRITICAL): plainContent → HTML 원본(content) 전달로 변경
  // Quill 서식(굵기/리스트/링크 등)이 백엔드까지 보존되어야 한다.
  // DOMPurify sanitize 는 editor.tsx 의 text-change 핸들러에서 이미 적용됨.
  // B-7-012: validatePassword throw → try/catch 로 감싸 토스트 노출
  const handleSubmit = async () => {
    if (!validateForm() || !post || !postId) return;

    let validatedPassword: number | undefined = undefined;
    if (isGuest) {
      try {
        validatedPassword = validatePassword(guestPassword, false);
      } catch (e) {
        const message = e instanceof Error ? e.message : "비밀번호를 확인해주세요.";
        showWarning("비밀번호 확인", message);
        return;
      }
    }

    // 성공 흐름은 useUpdatePostAction 내부에서 router.push 까지 처리.
    // 임시저장은 게시 성공 여부와 무관하게 즉시 정리해도 무방 (수정 폼은 서버 데이터 기반).
    removeDraft();

    updatePost({
      postId,
      title: title.trim(),
      content, // HTML 원본 (DOMPurify-sanitized)
      password: validatedPassword,
    });
  };

  // 폼 유효성 상태 (HTML 태그 제거 후 텍스트 길이로 판단)
  const isFormValid = Boolean(title.trim() && stripHtml(content).trim());

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

    // Draft (B-7-004)
    isAutoSaving,
    lastSavedAt,
    saveDraftManual,
    formatLastSaved,
  };
}
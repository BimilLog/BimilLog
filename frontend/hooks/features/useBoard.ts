"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth, useToast } from "@/hooks";
import { postCommand, postQuery } from "@/lib/api";
import { stripHtml, validatePassword } from "@/lib/utils";
import type { SimplePost } from "@/types/domains/post";
import type { PasswordModalMode } from "./usePasswordModal";

// ============ BOARD LIST HOOKS ============

// Board 페이지 데이터 관리
export const useBoardData = () => {
  // 검색 상태
  const [searchTerm, setSearchTerm] = useState("");
  const [searchType, setSearchType] = useState<"TITLE" | "TITLE_CONTENT" | "AUTHOR">("TITLE");
  const [postsPerPage, setPostsPerPage] = useState("30");

  // 게시글 상태
  const [posts, setPosts] = useState<SimplePost[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // 메인 게시글 조회 및 검색
  const fetchPostsAndSearch = useCallback(
    async (page = 0) => {
      setIsLoading(true);
      try {
        const response = searchTerm.trim()
          ? await postQuery.search(
              searchType,
              searchTerm.trim(),
              page,
              Number(postsPerPage)
            )
          : await postQuery.getAll(page, Number(postsPerPage));

        if (response.success && response.data) {
          setPosts(response.data.content);
          setTotalPages(response.data.totalPages);
          setCurrentPage(response.data.number);
        } else {
          setPosts([]);
          setTotalPages(0);
          setCurrentPage(0);
        }
      } catch {
        setPosts([]);
        setTotalPages(0);
        setCurrentPage(0);
      } finally {
        setIsLoading(false);
      }
    },
    [searchType, searchTerm, postsPerPage]
  );

  // 검색 핸들러
  const handleSearch = useCallback(() => {
    setCurrentPage(0);
    fetchPostsAndSearch(0);
  }, [fetchPostsAndSearch]);

  // 페이지당 게시글 수 변경 시 첫 페이지로 이동
  useEffect(() => {
    fetchPostsAndSearch(0);
  }, [postsPerPage, fetchPostsAndSearch]);

  return {
    // 검색 상태
    searchTerm,
    setSearchTerm,
    searchType,
    setSearchType,
    postsPerPage,
    setPostsPerPage,
    handleSearch,

    // 게시글 상태
    posts,
    isLoading,
    currentPage,
    setCurrentPage,
    totalPages,
    fetchPostsAndSearch,
  };
};

// ============ BOARD WRITE HOOKS ============

// 게시글 작성 폼 Hook
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
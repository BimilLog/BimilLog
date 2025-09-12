import { useState, useEffect, useCallback } from "react";
import { boardApi, type SimplePost } from "@/lib/api";

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
          ? await boardApi.searchPosts(
              searchType,
              searchTerm.trim(),
              page,
              Number(postsPerPage)
            )
          : await boardApi.getPosts(page, Number(postsPerPage));

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
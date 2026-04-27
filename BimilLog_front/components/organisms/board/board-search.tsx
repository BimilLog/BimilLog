"use client";

import { memo, useState, useCallback } from "react";
import { Input, Button, Card } from "@/components";
import { Dropdown, DropdownItem, Spinner } from "flowbite-react";
import { Search, ChevronDown, X, Edit, AlertCircle } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/common/useAuth";
import {
  Modal,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from "flowbite-react";

interface BoardSearchProps {
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  searchType: "TITLE" | "TITLE_CONTENT" | "WRITER";
  setSearchType: (type: "TITLE" | "TITLE_CONTENT" | "WRITER") => void;
  // termOverride 를 받을 수 있어 빈 입력 Enter 도 안전하게 처리
  handleSearch: (termOverride?: string) => void;
  // 검색 결과 카운트 (검색 모드일 때만)
  isSearching?: boolean;
  totalElements?: number;
  // 검색 중 spinner 표시
  isSearchFetching?: boolean;
}

export const BoardSearch = memo(({
  searchTerm,
  setSearchTerm,
  searchType,
  setSearchType,
  handleSearch,
  isSearching = false,
  totalElements,
  isSearchFetching = false,
}: BoardSearchProps) => {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const [showAnonymousModal, setShowAnonymousModal] = useState(false);

  // 검색 실행 핸들러: 빈 입력도 허용 (일반 목록 복귀)
  const executeSearch = useCallback(() => {
    handleSearch();
  }, [handleSearch]);

  // 검색 타입별 동적 placeholder
  const getPlaceholder = () => {
    switch (searchType) {
      case "TITLE":
        return "제목을 입력하세요...";
      case "TITLE_CONTENT":
        return "제목 또는 내용을 입력하세요...";
      case "WRITER":
        return "작성자명을 입력하세요...";
      default:
        return "검색어를 입력하세요...";
    }
  };

  // 글쓰기 버튼 클릭: 비로그인 시 안내 모달 노출
  const handleWriteClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    if (!isAuthenticated) {
      e.preventDefault();
      setShowAnonymousModal(true);
    }
  };

  const handleProceedAnonymous = () => {
    setShowAnonymousModal(false);
    router.push("/board/write");
  };

  const handleGoLogin = () => {
    setShowAnonymousModal(false);
    router.push("/login");
  };

  const typeLabel =
    searchType === "TITLE"
      ? "제목"
      : searchType === "TITLE_CONTENT"
        ? "제목+내용"
        : searchType === "WRITER"
          ? "작성자"
          : "제목";

  return (
    <>
      <Card
        variant="default"
        className="mb-6 p-4 bg-white backdrop-blur-none dark:bg-slate-900/70 dark:text-gray-100"
      >
        <div
          data-testid="board-search-row"
          className="flex flex-col gap-3 md:flex-row md:items-center md:gap-4"
        >
          {/* 모바일: type 드롭다운을 별도 행으로 분리 (B-M1) */}
          <div className="md:hidden">
            <Dropdown
              label=""
              dismissOnClick={true}
              renderTrigger={() => (
                <button
                  aria-label="검색 유형 선택"
                  className="flex w-full items-center justify-between rounded-lg border border-border bg-card px-4 py-3 min-h-[44px] text-sm text-foreground hover:bg-accent transition-colors"
                >
                  <span className="font-medium">{typeLabel}</span>
                  <ChevronDown className="w-4 h-4 stroke-muted-foreground" />
                </button>
              )}
            >
              <DropdownItem onClick={() => setSearchType("TITLE")}>제목</DropdownItem>
              <DropdownItem onClick={() => setSearchType("TITLE_CONTENT")}>
                제목+내용
              </DropdownItem>
              <DropdownItem onClick={() => setSearchType("WRITER")}>작성자</DropdownItem>
            </Dropdown>
          </div>

          {/* 검색 입력 영역 */}
          <div className="flex-1">
            <div className="flex items-center border border-border rounded-lg bg-card overflow-hidden transition-all hover:border-brand-secondary/50 focus-within:border-brand-secondary focus-within:ring-2 focus-within:ring-brand-secondary/20">
              {/* 데스크톱: type 드롭다운을 input 좌측 inline (B-M1: 모바일에서는 hidden) */}
              <div className="hidden md:block">
                <Dropdown
                  label=""
                  dismissOnClick={true}
                  renderTrigger={() => (
                    <button
                      aria-label="검색 유형 선택"
                      className="flex items-center justify-between w-[120px] px-3 py-2 border-0 rounded-none focus:ring-0 focus-visible:ring-0 focus-visible:ring-offset-0 bg-gray-50 hover:bg-gray-100 border-r border-border text-sm text-foreground dark:bg-slate-900 dark:hover:bg-slate-800"
                    >
                      <span>{typeLabel}</span>
                      <ChevronDown className="w-4 h-4 stroke-muted-foreground" />
                    </button>
                  )}
                >
                  <DropdownItem onClick={() => setSearchType("TITLE")}>제목</DropdownItem>
                  <DropdownItem onClick={() => setSearchType("TITLE_CONTENT")}>
                    제목+내용
                  </DropdownItem>
                  <DropdownItem onClick={() => setSearchType("WRITER")}>
                    작성자
                  </DropdownItem>
                </Dropdown>
              </div>

              <Input
                type="text"
                placeholder={getPlaceholder()}
                className="flex-1 border-0 rounded-none bg-transparent text-foreground focus-visible:ring-0 focus-visible:ring-offset-0"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    // 현재 input value 직접 전달 (closure stale 방지)
                    const currentValue = (e.target as HTMLInputElement).value;
                    handleSearch(currentValue);
                  }
                }}
              />

              {/* 검색 중 spinner */}
              {isSearchFetching && (
                <span
                  data-testid="board-search-spinner"
                  className="flex items-center px-3"
                  aria-label="검색 중"
                >
                  <Spinner size="sm" />
                </span>
              )}

              {searchTerm && (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => {
                    setSearchTerm("");
                    handleSearch("");
                  }}
                  className="border-0 rounded-none hover:bg-accent"
                  aria-label="검색어 지우기"
                >
                  <X className="w-4 h-4 stroke-muted-foreground" />
                </Button>
              )}
              <Button
                variant="ghost"
                size="icon"
                onClick={executeSearch}
                aria-label="검색"
                className="border-0 rounded-none border-l border-border hover:bg-brand-secondary/10"
              >
                <Search className="w-5 h-5 stroke-blue-600 fill-blue-100" />
              </Button>
            </div>
          </div>

          {/* 글쓰기 버튼 */}
          <div className="flex items-center gap-2 text-brand-muted dark:text-gray-300">
            <Link
              href="/board/write"
              onClick={handleWriteClick}
              data-testid="write-post-button"
              prefetch
            >
              <Button size="sm" className="inline-flex items-center min-h-[44px]">
                <Edit className="w-4 h-4 mr-1 stroke-slate-600 fill-slate-100" />
                <span className="text-sm">글쓰기</span>
              </Button>
            </Link>
          </div>
        </div>

        {/* 검색 결과 카운트 (A-2) */}
        {isSearching && typeof totalElements === "number" && (
          <div
            data-testid="search-result-count"
            className="mt-3 text-sm text-muted-foreground"
          >
            총 <strong className="text-foreground">{totalElements}건</strong>의 결과
          </div>
        )}
      </Card>

      {/* 비로그인 글쓰기 안내 모달 (A-5) */}
      <Modal
        show={showAnonymousModal}
        onClose={() => setShowAnonymousModal(false)}
        size="md"
        popup
        dismissible
      >
        <ModalHeader />
        <ModalBody>
          <div
            data-testid="anonymous-write-info-modal"
            className="text-center"
          >
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-purple-50 dark:bg-purple-900/30">
              <AlertCircle className="h-8 w-8 stroke-purple-600 fill-purple-100" />
            </div>
            <h3 className="mb-3 text-lg font-bold text-foreground">
              로그인하면 더 편리해요
            </h3>
            <p className="mb-4 text-sm text-muted-foreground whitespace-pre-line">
              {`로그인하면 게시글 수정/삭제가 자동으로 가능합니다.\n비로그인으로 작성하시면 4자리 숫자(1000~9999) 비밀번호가 필요합니다.`}
            </p>
            <div className="flex flex-col gap-2 sm:flex-row sm:justify-center">
              <Button
                variant="default"
                onClick={handleGoLogin}
                className="min-h-[44px]"
              >
                로그인하기
              </Button>
              <Button
                variant="outline"
                onClick={handleProceedAnonymous}
                className="min-h-[44px]"
              >
                비로그인으로 계속
              </Button>
            </div>
          </div>
        </ModalBody>
        <ModalFooter className="border-t-0 hidden" />
      </Modal>
    </>
  );
});

BoardSearch.displayName = "BoardSearch";

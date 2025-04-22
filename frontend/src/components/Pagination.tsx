import React from "react";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  pageRangeDisplayed?: number; // 표시할 페이지 범위 (기본값: 5)
}

const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  pageRangeDisplayed = 5,
}) => {
  // 현재 페이지 그룹과 총 그룹 수 계산
  const currentGroup = Math.ceil(currentPage / pageRangeDisplayed);
  const totalGroups = Math.ceil(totalPages / pageRangeDisplayed);

  // 현재 그룹의 페이지 범위 계산
  const getPageNumbers = (): number[] => {
    const startPage = (currentGroup - 1) * pageRangeDisplayed + 1;
    const endPage = Math.min(startPage + pageRangeDisplayed - 1, totalPages);

    return Array.from(
      { length: endPage - startPage + 1 },
      (_, i) => startPage + i
    );
  };

  const pageNumbers = getPageNumbers();

  // 이전 그룹의 첫 페이지 계산
  const prevGroupFirstPage = (currentGroup - 2) * pageRangeDisplayed + 1;

  // 다음 그룹의 첫 페이지 계산
  const nextGroupFirstPage = currentGroup * pageRangeDisplayed + 1;

  // 첫 번째 그룹인지 확인
  const isFirstGroup = currentGroup === 1;

  // 마지막 그룹인지 확인
  const isLastGroup = currentGroup === totalGroups;

  return (
    <nav aria-label="페이지 네비게이션">
      <div className="d-flex justify-content-center my-4">
        <div className="btn-group">
          {/* 이전 그룹 버튼 (첫 번째 그룹이 아닐 때만 표시) */}
          {!isFirstGroup && (
            <button
              className="btn btn-outline-secondary"
              onClick={() => onPageChange(prevGroupFirstPage)}
              aria-label="이전 그룹"
            >
              이전
            </button>
          )}

          {/* 페이지 번호 목록 */}
          {pageNumbers.map((page) => (
            <button
              key={page}
              className={`btn ${
                currentPage === page ? "btn-secondary" : "btn-outline-secondary"
              }`}
              onClick={() => onPageChange(page)}
              aria-current={currentPage === page ? "page" : undefined}
            >
              {page}
            </button>
          ))}

          {/* 다음 그룹 버튼 (마지막 그룹이 아닐 때만 표시) */}
          {!isLastGroup && (
            <button
              className="btn btn-outline-secondary"
              onClick={() => onPageChange(nextGroupFirstPage)}
              aria-label="다음 그룹"
            >
              다음
            </button>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Pagination;

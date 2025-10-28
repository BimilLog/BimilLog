"use client";

import { Pagination } from "flowbite-react";

interface BoardPaginationProps {
  currentPage: number;
  totalPages: number;
  setCurrentPage: (page: number) => void;
}

export const BoardPagination = ({
  currentPage,
  totalPages,
  setCurrentPage,
}: BoardPaginationProps) => {
  if (totalPages <= 1) return null;

  // Convert from 0-based to 1-based page numbering for Flowbite
  const flowbiteCurrentPage = currentPage + 1;

  const handlePageChange = (page: number) => {
    // Convert back to 0-based for your internal state
    setCurrentPage(page - 1);
  };

  return (
    <div className="flex items-center justify-center mt-8">
      <Pagination
        currentPage={flowbiteCurrentPage}
        totalPages={totalPages}
        onPageChange={handlePageChange}
        showIcons
        previousLabel="이전"
        nextLabel="다음"
        className="text-sm"
        theme={{
          pages: {
            base: "xs:mt-0 mt-2 inline-flex items-center -space-x-px",
            showIcon: "inline-flex",
            previous: {
              base: "ml-0 flex min-w-[3rem] items-center justify-center gap-1 rounded-l-lg border border-gray-300 bg-white px-3 py-2 text-gray-500 leading-tight hover:bg-gray-100 hover:text-gray-700 dark:border-slate-700 dark:bg-slate-900/80 dark:text-gray-300 dark:hover:bg-slate-800 dark:hover:text-gray-100",
              icon: "h-5 w-5"
            },
            next: {
              base: "flex min-w-[3rem] items-center justify-center gap-1 rounded-r-lg border border-gray-300 bg-white px-3 py-2 text-gray-500 leading-tight hover:bg-gray-100 hover:text-gray-700 dark:border-slate-700 dark:bg-slate-900/80 dark:text-gray-300 dark:hover:bg-slate-800 dark:hover:text-gray-100",
              icon: "h-5 w-5"
            },
            selector: {
              base: "flex min-w-[3rem] items-center justify-center border border-gray-300 bg-white px-3 py-2 text-gray-500 leading-tight hover:bg-gray-100 hover:text-gray-700 dark:border-slate-700 dark:bg-slate-900/80 dark:text-gray-300 dark:hover:bg-slate-800 dark:hover:text-gray-100",
              active: "border-blue-500 bg-blue-500 text-white hover:bg-blue-500 hover:text-white dark:border-blue-400 dark:bg-blue-500",
              disabled: "cursor-not-allowed text-gray-400 dark:text-gray-600"
            }
          }
        }}
      />
    </div>
  );
};

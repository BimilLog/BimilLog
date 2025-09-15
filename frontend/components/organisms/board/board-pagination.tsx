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
        className="text-sm"
        theme={{
          pages: {
            base: "xs:mt-0 mt-2 inline-flex items-center -space-x-px",
            showIcon: "inline-flex",
            previous: {
              base: "ml-0 rounded-l-lg border border-gray-300 bg-white py-2 px-3 leading-tight text-gray-500 hover:bg-gray-100 hover:text-gray-700",
              icon: "h-5 w-5"
            },
            next: {
              base: "rounded-r-lg border border-gray-300 bg-white py-2 px-3 leading-tight text-gray-500 hover:bg-gray-100 hover:text-gray-700",
              icon: "h-5 w-5"
            },
            selector: {
              base: "w-12 border border-gray-300 bg-white py-2 px-3 leading-tight text-gray-500 hover:bg-gray-100 hover:text-gray-700",
              active: "bg-brand-primary border-brand-primary text-white hover:bg-brand-primary hover:text-white",
              disabled: "cursor-not-allowed opacity-50"
            }
          }
        }}
      />
    </div>
  );
};

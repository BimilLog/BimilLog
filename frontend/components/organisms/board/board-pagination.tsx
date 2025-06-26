"use client";

import { Button } from "@/components/ui/button";
import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
} from "lucide-react";

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

  const pageNumbers = [];
  const maxPagesToShow = 5;
  let startPage = Math.max(0, currentPage - Math.floor(maxPagesToShow / 2));
  const endPage = Math.min(totalPages - 1, startPage + maxPagesToShow - 1);

  if (endPage - startPage + 1 < maxPagesToShow) {
    startPage = Math.max(0, endPage - maxPagesToShow + 1);
  }

  for (let i = startPage; i <= endPage; i++) {
    pageNumbers.push(i);
  }

  return (
    <div className="flex items-center justify-center space-x-2 mt-8">
      <Button
        variant="outline"
        size="icon"
        onClick={() => setCurrentPage(0)}
        disabled={currentPage === 0}
        className="bg-white"
      >
        <ChevronsLeft className="h-4 w-4" />
      </Button>
      <Button
        variant="outline"
        size="icon"
        onClick={() => setCurrentPage(currentPage - 1)}
        disabled={currentPage === 0}
        className="bg-white"
      >
        <ChevronLeft className="h-4 w-4" />
      </Button>

      {startPage > 0 && (
        <>
          <Button variant="ghost" size="icon" disabled>
            ...
          </Button>
        </>
      )}

      {pageNumbers.map((number) => (
        <Button
          key={number}
          variant={currentPage === number ? "default" : "outline"}
          onClick={() => setCurrentPage(number)}
          className={currentPage === number ? "bg-purple-600" : "bg-white"}
        >
          {number + 1}
        </Button>
      ))}

      {endPage < totalPages - 1 && (
        <>
          <Button variant="ghost" size="icon" disabled>
            ...
          </Button>
        </>
      )}

      <Button
        variant="outline"
        size="icon"
        onClick={() => setCurrentPage(currentPage + 1)}
        disabled={currentPage === totalPages - 1}
        className="bg-white"
      >
        <ChevronRight className="h-4 w-4" />
      </Button>
      <Button
        variant="outline"
        size="icon"
        onClick={() => setCurrentPage(totalPages - 1)}
        disabled={currentPage === totalPages - 1}
        className="bg-white"
      >
        <ChevronsRight className="h-4 w-4" />
      </Button>
    </div>
  );
};

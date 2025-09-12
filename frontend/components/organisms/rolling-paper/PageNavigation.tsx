"use client";

import React from "react";
import { Button } from "@/components/ui/button";
import { ChevronLeft, ChevronRight } from "lucide-react";

interface PageNavigationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export const PageNavigation: React.FC<PageNavigationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  className = "",
}) => {
  return (
    <div className={`flex flex-col items-center space-y-4 ${className}`}>
      {/* 페이지 인디케이터 */}
      <div className="flex justify-center items-center space-x-2">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.max(1, currentPage - 1))}
          disabled={currentPage === 1}
          className="h-8 w-8 p-0 bg-white/80"
        >
          <ChevronLeft className="w-4 h-4" />
        </Button>

        <div className="flex space-x-1">
          {Array.from({ length: totalPages }, (_, i) => (
            <Button
              key={i + 1}
              variant={currentPage === i + 1 ? "default" : "outline"}
              size="sm"
              onClick={() => onPageChange(i + 1)}
              className={`h-8 w-8 p-0 ${
                currentPage === i + 1
                  ? "bg-gradient-to-r from-blue-500 to-cyan-600 text-white"
                  : "bg-white/80"
              }`}
            >
              {i + 1}
            </Button>
          ))}
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
          disabled={currentPage === totalPages}
          className="h-8 w-8 p-0 bg-white/80"
        >
          <ChevronRight className="w-4 h-4" />
        </Button>
      </div>

      {/* 현재 페이지 표시 */}
      <div className="text-center">
        <p className="text-cyan-600 text-sm font-medium">
          페이지 {currentPage} / {totalPages}
        </p>
      </div>
    </div>
  );
};

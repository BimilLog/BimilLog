"use client";

import { Button } from "@/components";
import { ArrowLeft, Save, Eye } from "lucide-react";
import Link from "next/link";

interface WritePageHeaderProps {
  isPreview: boolean;
  onTogglePreview: () => void;
  onSubmit: () => void;
  isSubmitting: boolean;
  isFormValid: boolean;
}

export const WritePageHeader: React.FC<WritePageHeaderProps> = ({
  isPreview,
  onTogglePreview,
  onSubmit,
  isSubmitting,
  isFormValid,
}) => {
  return (
    <div className="sticky top-16 z-40 border-b bg-white/60 backdrop-blur-sm transition-colors duration-300 dark:border-slate-800 dark:bg-slate-900/70">
      <div className="container mx-auto px-4 py-3">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          {/* 좌측: 뒤로가기 및 제목 */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Link href="/board">
                <Button variant="ghost" size="sm" className="pl-0">
                  <ArrowLeft className="w-4 h-4 mr-1 stroke-slate-600 fill-slate-100" />
                  <span className="hidden sm:inline">게시판</span>
                </Button>
              </Link>
              <h1 className="text-lg font-bold text-brand-primary whitespace-nowrap sm:text-xl dark:text-gray-100">
                새 글 작성
              </h1>
            </div>

            {/* 모바일에서만 보이는 버튼 그룹 */}
            <div className="flex items-center gap-2 sm:hidden">
              <Button
                variant="outline"
                size="sm"
                onClick={onTogglePreview}
                className="bg-white dark:bg-slate-900/80"
              >
                <Eye className="w-4 h-4 stroke-purple-600 fill-purple-100" />
              </Button>
              <Button
                size="sm"
                onClick={onSubmit}
                disabled={isSubmitting || !isFormValid}
                className="bg-gradient-to-r from-pink-500 to-purple-600"
              >
                <Save className="w-4 h-4 stroke-green-600 fill-green-100" />
              </Button>
            </div>
          </div>

          {/* 우측: 버튼 그룹 (데스크톱) */}
          <div className="hidden items-center gap-2 sm:flex">
            <Button
              variant="outline"
              onClick={onTogglePreview}
              className="bg-white dark:bg-slate-900/80"
            >
              <Eye className="w-4 h-4 mr-2 stroke-purple-600 fill-purple-100" />
              {isPreview ? "편집" : "미리보기"}
            </Button>
            <Button
              onClick={onSubmit}
              disabled={isSubmitting || !isFormValid}
              className="bg-gradient-to-r from-pink-500 to-purple-600"
            >
              <Save className="w-4 h-4 mr-2 stroke-green-600 fill-green-100" />
              {isSubmitting ? "작성 중..." : "작성완료"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

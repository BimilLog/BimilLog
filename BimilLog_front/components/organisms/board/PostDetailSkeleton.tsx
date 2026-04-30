import { memo } from "react";
import { Card, CardHeader, CardContent } from "@/components";
import { CommentSectionSkeleton } from "./CommentSectionSkeleton";

/** 스켈레톤 블록 유틸리티 — 라운드 8: paper-aged / postal-navy 톤으로 통일 */
const Bone = ({ className }: { className: string }) => (
  <div className={`bg-paper-aged dark:bg-postal-navy/30 rounded animate-pulse ${className}`} />
);

const PostDetailSkeleton = memo(() => {
  return (
    /* 라운드 8 B-8-013: 본 페이지(bg-paper)와 일치시켜 로딩→마운트 시 깜빡임 방지 */
    <div className="min-h-screen bg-paper">
      <div className="container mx-auto px-4 py-8">
        {/* 브레드크럼 스켈레톤 */}
        <div className="mb-4">
          <Bone className="h-4 w-48" />
        </div>

        {/* 게시글 카드 스켈레톤 */}
        <Card variant="elevated" className="mb-8">
          <CardHeader className="border-b p-4 md:p-6">
            {/* 제목 스켈레톤 */}
            <div className="mb-4">
              <Bone className="h-8 w-3/4 mb-2" />
            </div>

            {/* 작성자 정보 스켈레톤 */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex flex-col space-y-2">
                {/* 작성자와 시간 */}
                <div className="flex items-center space-x-3">
                  <Bone className="h-4 w-24" />
                  <Bone className="h-4 w-32" />
                </div>

                {/* 통계 정보 */}
                <div className="flex items-center space-x-4">
                  <Bone className="h-4 w-16" />
                  <Bone className="h-4 w-16" />
                  <Bone className="h-4 w-16" />
                </div>
              </div>

              {/* 버튼 영역 스켈레톤 */}
              <div className="flex gap-2">
                <Bone className="h-9 w-24" />
                <Bone className="h-9 w-24" />
              </div>
            </div>
          </CardHeader>

          <CardContent className="p-6">
            {/* 본문 스켈레톤 */}
            <div className="space-y-3 mb-8">
              <Bone className="h-4 w-full" />
              <Bone className="h-4 w-full" />
              <Bone className="h-4 w-5/6" />
              <Bone className="h-4 w-full" />
              <Bone className="h-4 w-4/5" />
            </div>

            {/* 추천/신고 버튼 스켈레톤 */}
            <div className="flex items-center justify-center gap-4 mt-8 pt-6 border-t border-paper-200 dark:border-postal-navy/40">
              <Bone className="h-10 w-32" />
              <Bone className="h-10 w-32" />
            </div>
          </CardContent>
        </Card>

        {/* 댓글 섹션 스켈레톤 */}
        <CommentSectionSkeleton count={3} />
      </div>
    </div>
  );
});

PostDetailSkeleton.displayName = "PostDetailSkeleton";

export { PostDetailSkeleton };

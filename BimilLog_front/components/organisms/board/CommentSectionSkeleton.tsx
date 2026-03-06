import { memo } from "react";
import { Card, CardContent, CardHeader } from "@/components";

/**
 * 댓글 섹션 로딩 스켈레톤
 * CommentSection.tsx의 레이아웃에 대응:
 * - 댓글 작성 폼 스켈레톤
 * - 댓글 목록 스켈레톤 (아바타, 닉네임, 본문, 액션 버튼)
 */

interface CommentSectionSkeletonProps {
  /** 표시할 댓글 수 (기본값: 3) */
  count?: number;
}

/** 스켈레톤 블록 유틸리티 */
const Bone = ({ className }: { className: string }) => (
  <div className={`bg-gray-200 dark:bg-gray-700 rounded animate-pulse ${className}`} />
);

/** 단일 댓글 스켈레톤 */
const CommentItemSkeleton = ({ isReply = false }: { isReply?: boolean }) => (
  <div className={`py-4 ${isReply ? "ml-8 pl-4 border-l-2 border-gray-100 dark:border-gray-700" : ""}`}>
    {/* 상단: 아바타 + 닉네임 + 시간 */}
    <div className="flex items-center gap-3 mb-3">
      <Bone className="h-8 w-8 rounded-full shrink-0" />
      <div className="flex items-center gap-2">
        <Bone className="h-4 w-16" />
        <Bone className="h-3 w-20" />
      </div>
    </div>

    {/* 본문 */}
    <div className="space-y-2 mb-3 ml-11">
      <Bone className="h-4 w-full" />
      <Bone className="h-4 w-3/4" />
    </div>

    {/* 하단: 좋아요 + 답글 버튼 */}
    <div className="flex items-center gap-4 ml-11">
      <Bone className="h-6 w-14" />
      <Bone className="h-6 w-14" />
    </div>
  </div>
);

const CommentSectionSkeleton = memo<CommentSectionSkeletonProps>(({
  count = 3,
}) => {
  return (
    <div className="space-y-6">
      {/* 댓글 작성 폼 스켈레톤 */}
      <Card variant="elevated">
        <CardHeader className="border-b p-4">
          <Bone className="h-5 w-24" />
        </CardHeader>
        <CardContent className="p-4">
          <div className="space-y-3">
            <Bone className="h-24 w-full rounded-lg" />
            <div className="flex justify-end">
              <Bone className="h-9 w-20 rounded-lg" />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 댓글 목록 스켈레톤 */}
      <Card variant="elevated">
        <CardHeader className="border-b p-4">
          <div className="flex items-center gap-2">
            <Bone className="h-5 w-12" />
            <Bone className="h-5 w-6" />
          </div>
        </CardHeader>
        <CardContent className="p-4">
          <div className="divide-y divide-gray-100 dark:divide-gray-800">
            {Array.from({ length: count }).map((_, idx) => (
              <div key={idx}>
                <CommentItemSkeleton />
                {/* 두 번째 댓글에 대댓글 스켈레톤 추가 (실제 구조 반영) */}
                {idx === 1 && <CommentItemSkeleton isReply />}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
});

CommentSectionSkeleton.displayName = "CommentSectionSkeleton";

export { CommentSectionSkeleton };

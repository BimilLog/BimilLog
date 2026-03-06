import { memo } from "react";
import { Card } from "@/components";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from "flowbite-react";

/**
 * 게시판 목록 로딩 스켈레톤
 * BoardTable.tsx의 데스크톱(테이블) + 모바일(카드) 레이아웃에 대응
 */

interface BoardTableSkeletonProps {
  /** 표시할 행 수 (기본값: 6) */
  rows?: number;
  /** 순위 표시 여부 (인기글/레전드 탭) */
  showRanking?: boolean;
}

/** 스켈레톤 블록 유틸리티 */
const Bone = ({ className }: { className: string }) => (
  <div className={`bg-gray-200 dark:bg-gray-700 rounded animate-pulse ${className}`} />
);

const BoardTableSkeleton = memo<BoardTableSkeletonProps>(({
  rows = 6,
  showRanking = false,
}) => {
  return (
    <>
      {/* 데스크톱 테이블 스켈레톤 */}
      <div className="hidden sm:block overflow-x-auto">
        <Table className="min-w-full">
          <TableHead className="bg-gray-50 dark:bg-slate-900/80">
            <TableRow>
              <TableHeadCell className="w-20 text-center">
                {showRanking ? "순위" : "번호"}
              </TableHeadCell>
              <TableHeadCell>제목</TableHeadCell>
              <TableHeadCell className="w-24">작성자</TableHeadCell>
              <TableHeadCell className="w-28 hidden sm:table-cell">작성일</TableHeadCell>
              <TableHeadCell className="w-20 text-center">추천</TableHeadCell>
              <TableHeadCell className="w-20 text-center">조회</TableHeadCell>
            </TableRow>
          </TableHead>
          <TableBody className="divide-y divide-gray-100 dark:divide-slate-800">
            {Array.from({ length: rows }).map((_, idx) => (
              <TableRow key={idx} className="bg-card">
                {/* 번호/순위 */}
                <TableCell className="text-center">
                  <Bone className="h-4 w-8 mx-auto" />
                </TableCell>

                {/* 제목 (너비 랜덤 효과) */}
                <TableCell>
                  <div className="flex items-center gap-2">
                    {/* 뱃지 자리 (간헐적 표시) */}
                    {idx % 3 === 0 && <Bone className="h-5 w-12 shrink-0" />}
                    <Bone className={`h-4 ${idx % 2 === 0 ? "w-3/4" : "w-1/2"}`} />
                  </div>
                </TableCell>

                {/* 작성자 */}
                <TableCell>
                  <div className="flex items-center gap-1">
                    <Bone className="h-3 w-3 rounded-full shrink-0" />
                    <Bone className="h-4 w-14" />
                  </div>
                </TableCell>

                {/* 작성일 */}
                <TableCell className="hidden sm:table-cell">
                  <Bone className="h-4 w-20" />
                </TableCell>

                {/* 추천 */}
                <TableCell className="text-center">
                  <Bone className="h-4 w-8 mx-auto" />
                </TableCell>

                {/* 조회 */}
                <TableCell className="text-center">
                  <Bone className="h-4 w-8 mx-auto" />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* 모바일 카드 스켈레톤 */}
      <div className="sm:hidden space-y-3">
        {Array.from({ length: rows }).map((_, idx) => (
          <Card key={idx} variant="elevated">
            <div className="p-3 space-y-2">
              {/* 뱃지 + 제목 영역 */}
              <div className="flex items-center gap-2 mb-1.5">
                {showRanking && <Bone className="h-5 w-6" />}
                {idx % 3 === 0 && <Bone className="h-5 w-12" />}
              </div>
              <Bone className={`h-4 ${idx % 2 === 0 ? "w-full" : "w-4/5"}`} />
              {idx % 3 === 1 && <Bone className="h-4 w-2/3" />}

              {/* 하단: 작성자, 날짜, 좋아요, 조회수 */}
              <div className="flex items-center justify-between pt-1">
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-1">
                    <Bone className="h-3 w-3 rounded-full" />
                    <Bone className="h-3 w-12" />
                  </div>
                  <Bone className="h-3 w-16" />
                </div>
                <div className="flex items-center gap-2">
                  <Bone className="h-3 w-8" />
                  <Bone className="h-3 w-8" />
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </>
  );
});

BoardTableSkeleton.displayName = "BoardTableSkeleton";

export { BoardTableSkeleton };

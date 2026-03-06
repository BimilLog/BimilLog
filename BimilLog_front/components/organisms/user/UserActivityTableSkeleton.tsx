import React, { memo } from "react";
import { Card } from "@/components";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow
} from "flowbite-react";

interface UserActivityTableSkeletonProps {
  contentType: "posts" | "comments";
  rows?: number;
}

const SkeletonBar = ({ className = "" }: { className?: string }) => (
  <div className={`h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse ${className}`} />
);

const SkeletonBarSmall = ({ className = "" }: { className?: string }) => (
  <div className={`h-3 bg-gray-200 dark:bg-gray-700 rounded animate-pulse ${className}`} />
);

export const UserActivityTableSkeleton = memo<UserActivityTableSkeletonProps>(({
  contentType,
  rows = 5
}) => {
  const skeletonRows = [...Array(rows)];

  return (
    <>
      {/* 데스크톱 로딩 스켈레톤 */}
      <div className="hidden sm:block overflow-x-auto">
        <Table>
          <TableHead>
            <TableRow>
              <TableHeadCell className="w-20 text-center">번호</TableHeadCell>
              <TableHeadCell>{contentType === "posts" ? "제목" : "댓글 내용"}</TableHeadCell>
              <TableHeadCell className="w-28 hidden sm:table-cell">작성일</TableHeadCell>
              <TableHeadCell className="w-20 text-center">추천</TableHeadCell>
              {contentType === "posts" ? (
                <>
                  <TableHeadCell className="w-20 text-center">조회</TableHeadCell>
                  <TableHeadCell className="w-20 text-center">댓글</TableHeadCell>
                </>
              ) : (
                <TableHeadCell className="w-24 text-center">게시글 보기</TableHeadCell>
              )}
            </TableRow>
          </TableHead>
          <TableBody className="divide-y">
            {skeletonRows.map((_, idx) => (
              <TableRow key={idx}>
                <TableCell className="text-center">
                  <SkeletonBar />
                </TableCell>
                <TableCell>
                  <SkeletonBar />
                </TableCell>
                <TableCell className="hidden sm:table-cell">
                  <SkeletonBar className="w-20" />
                </TableCell>
                <TableCell className="text-center">
                  <SkeletonBar className="w-8 mx-auto" />
                </TableCell>
                {contentType === "posts" ? (
                  <>
                    <TableCell className="text-center">
                      <SkeletonBar className="w-8 mx-auto" />
                    </TableCell>
                    <TableCell className="text-center">
                      <SkeletonBar className="w-8 mx-auto" />
                    </TableCell>
                  </>
                ) : (
                  <TableCell className="text-center">
                    <SkeletonBar className="w-16 mx-auto" />
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* 모바일 로딩 스켈레톤 */}
      <div className="sm:hidden space-y-3">
        {skeletonRows.map((_, idx) => (
          <Card key={idx} variant="elevated">
            <div className="p-3 space-y-2">
              <SkeletonBar />
              <SkeletonBar className="w-3/4" />
              <div className="flex justify-between">
                <SkeletonBarSmall className="w-20" />
                <SkeletonBarSmall className="w-16" />
              </div>
            </div>
          </Card>
        ))}
      </div>
    </>
  );
});

UserActivityTableSkeleton.displayName = "UserActivityTableSkeleton";

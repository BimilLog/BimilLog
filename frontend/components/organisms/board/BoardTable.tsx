"use client";

import React, { memo } from "react";
import { Card } from "@/components";
import { Badge } from "@/components";
import Link from "next/link";
import { type SimplePost } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { usePostReadStatus } from "@/hooks/features/useReadingProgress";
import {
  CheckCircle,
  Megaphone,
  TrendingUp,
  Calendar,
  Crown,
  ThumbsUp,
  Eye
} from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
  Popover
} from "flowbite-react";

interface BoardTableProps {
  posts: SimplePost[];
  variant: "all" | "popular" | "legend";

  // 페이징
  currentPage?: number;
  totalPages?: number;
  onPageChange?: (page: number) => void;

  // 옵션
  showRanking?: boolean;
  enablePopover?: boolean;
}

interface TableRowProps {
  post: SimplePost;
  index: number;
  variant: "all" | "popular" | "legend";
  isRead: boolean;
  progress: number;
  showRanking?: boolean;
  enablePopover?: boolean;
}

// 데스크톱용 테이블 행 컴포넌트
const BoardTableRow = memo<TableRowProps>(({
  post,
  index,
  variant,
  isRead,
  progress,
  showRanking,
  enablePopover
}) => {
  const isPopularVariant = variant === "popular" || variant === "legend";

  return (
    <TableRow className="bg-white hover:bg-gray-50">
      {/* 상태 */}
      <TableCell className="text-center">
        {post.postCacheFlag === "NOTICE" && (
          <Badge variant="info" icon={Megaphone}>공지</Badge>
        )}
        {post.postCacheFlag === "REALTIME" && (
          <Badge variant="destructive" icon={TrendingUp}>실시간</Badge>
        )}
        {post.postCacheFlag === "WEEKLY" && (
          <Badge variant="warning" icon={Calendar}>주간</Badge>
        )}
        {post.postCacheFlag === "LEGEND" && (
          <Badge variant="purple" icon={Crown}>레전드</Badge>
        )}
      </TableCell>

      {/* 순위 또는 번호 */}
      <TableCell className="text-center font-medium">
        {showRanking ? (
          <span className="text-lg font-bold text-purple-600">
            {index + 1}
          </span>
        ) : (
          <span>{post.id}</span>
        )}
      </TableCell>

      {/* 제목 */}
      <TableCell>
        <div className="flex items-center gap-2">
          <Link
            href={`/board/post/${post.id}`}
            className={`font-semibold transition-colors line-clamp-2 block flex-1 ${
              isRead ? 'text-gray-500' : 'text-gray-900 hover:text-purple-600'
            }`}
          >
            {post.title}
            {post.commentCount > 0 && (
              <span className="ml-2 text-purple-500 font-normal">
                [{post.commentCount}]
              </span>
            )}
          </Link>
          {isRead && (
            <span title="읽음">
              <CheckCircle className="w-4 h-4 text-green-500 flex-shrink-0" />
            </span>
          )}
          {!isRead && progress > 0 && progress < 90 && (
            <div className="w-12 h-1.5 bg-gray-200 rounded-full flex-shrink-0">
              <div
                className="h-full bg-gradient-to-r from-purple-500 to-pink-500 rounded-full"
                style={{ width: `${progress}%` }}
                title={`${Math.round(progress)}% 읽음`}
              />
            </div>
          )}
        </div>
      </TableCell>

      {/* 작성자 */}
      <TableCell>
        <Link
          href={`/rolling-paper/${encodeURIComponent(post.userName)}`}
          className="hover:text-purple-600 hover:underline transition-colors truncate block max-w-20"
          title={`${post.userName}님의 롤링페이퍼 보기`}
        >
          {post.userName}
        </Link>
      </TableCell>

      {/* 작성일 */}
      <TableCell className="text-sm hidden sm:table-cell">
        {formatDate(post.createdAt)}
      </TableCell>

      {/* 추천 */}
      <TableCell className="text-center">
        {enablePopover ? (
          <Popover
            trigger="hover"
            placement="top"
            content={
              <div className="p-3 min-w-[180px]">
                <div className="flex items-center gap-2 mb-2">
                  <ThumbsUp className="w-4 h-4 text-purple-600" />
                  <span className="font-semibold text-sm">좋아요 통계</span>
                </div>
                <div className="space-y-1 text-xs">
                  <div className="flex justify-between">
                    <span className="text-gray-500">현재 좋아요:</span>
                    <span className="font-medium">{post.likeCount}개</span>
                  </div>
                  {showRanking && (
                    <div className="flex justify-between">
                      <span className="text-gray-500">순위:</span>
                      <span className="font-medium text-purple-600">#{index + 1}</span>
                    </div>
                  )}
                </div>
              </div>
            }
          >
            <span className="flex items-center justify-center cursor-help">
              <ThumbsUp className="w-3 h-3 mr-1" />
              {post.likeCount}
            </span>
          </Popover>
        ) : (
          <span>{post.likeCount}</span>
        )}
      </TableCell>

      {/* 조회수 */}
      <TableCell className="text-center">
        {post.viewCount}
      </TableCell>
    </TableRow>
  );
});

// 모바일용 카드 컴포넌트
const BoardMobileCard = memo<TableRowProps>(({
  post,
  index,
  variant,
  isRead,
  progress,
  showRanking
}) => {
  const isPopularVariant = variant === "popular" || variant === "legend";

  return (
    <Card variant="elevated" className="hover:shadow-brand-md transition-all">
      <div className="p-4">
        <div className="flex items-start justify-between mb-2">
          <div className="flex-1">
            {/* 상태, 번호/순위 표시 */}
            <div className="flex items-center gap-2 mb-2">
              {/* 상태 뱃지 */}
              {post.postCacheFlag === "NOTICE" && (
                <Badge variant="info" icon={Megaphone}>공지</Badge>
              )}
              {post.postCacheFlag === "REALTIME" && (
                <Badge variant="destructive" icon={TrendingUp}>실시간</Badge>
              )}
              {post.postCacheFlag === "WEEKLY" && (
                <Badge variant="warning" icon={Calendar}>주간</Badge>
              )}
              {post.postCacheFlag === "LEGEND" && (
                <Badge variant="purple" icon={Crown}>레전드</Badge>
              )}

              {/* 순위 또는 번호 */}
              {showRanking ? (
                <span className="text-xl font-bold text-purple-600">
                  #{index + 1}
                </span>
              ) : (
                <Badge variant="secondary" className="text-xs">
                  번호: {post.id}
                </Badge>
              )}
            </div>

            {/* 제목 */}
            <div className="flex items-start gap-2">
              <Link
                href={`/board/post/${post.id}`}
                className={`font-semibold transition-colors line-clamp-2 block text-base flex-1 ${
                  isRead ? 'text-gray-500' : 'text-brand-primary hover:text-purple-600'
                }`}
              >
                {post.title}
                {post.commentCount > 0 && (
                  <span className="ml-2 text-purple-500 font-normal">
                    [{post.commentCount}]
                  </span>
                )}
              </Link>
              {isRead && (
                <span title="읽음">
                  <CheckCircle className="w-4 h-4 text-green-500 flex-shrink-0 mt-1" />
                </span>
              )}
            </div>

            {/* 진행률 바 */}
            {!isRead && progress > 0 && progress < 90 && (
              <div className="w-full h-1.5 bg-gray-200 rounded-full mt-2">
                <div
                  className="h-full bg-gradient-to-r from-purple-500 to-pink-500 rounded-full"
                  style={{ width: `${progress}%` }}
                />
              </div>
            )}
          </div>
        </div>

        {/* 하단 정보 */}
        <div className="flex items-center justify-between text-sm text-brand-secondary">
          <div className="flex items-center gap-3">
            <Link
              href={`/rolling-paper/${encodeURIComponent(post.userName)}`}
              className="hover:text-purple-600 transition-colors truncate max-w-20"
              title={`${post.userName}님의 롤링페이퍼 보기`}
            >
              {post.userName}
            </Link>
            <span>{formatDate(post.createdAt)}</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="flex items-center gap-1">
              <ThumbsUp className="w-3 h-3" />
              {post.likeCount}
            </span>
            <span className="flex items-center gap-1">
              <Eye className="w-3 h-3" />
              {post.viewCount}
            </span>
          </div>
        </div>
      </div>
    </Card>
  );
});

BoardTableRow.displayName = "BoardTableRow";
BoardMobileCard.displayName = "BoardMobileCard";

export const BoardTable = memo<BoardTableProps>(({
  posts,
  variant,
  showRanking = variant !== "all",
  enablePopover = variant !== "all"
}) => {
  // 읽음 상태 추적 - 모든 variant에서 사용
  const postIds = posts.map(post => post.id);
  const { readStatus, progressStatus } = usePostReadStatus(postIds);

  const isPopularVariant = variant === "popular" || variant === "legend";

  return (
    <>
      {/* 데스크톱 테이블 */}
      <div className="hidden sm:block overflow-x-auto">
        <Table hoverable>
          <TableHead>
            <TableRow>
              <TableHeadCell className="w-20"></TableHeadCell>
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
          <TableBody className="divide-y">
            {posts.length > 0 ? (
              posts.map((post, index) => (
                <BoardTableRow
                  key={post.id}
                  post={post}
                  index={index}
                  variant={variant}
                  isRead={readStatus[post.id] || false}
                  progress={progressStatus[post.id] || 0}
                  showRanking={showRanking}
                  enablePopover={enablePopover}
                />
              ))
            ) : (
              <TableRow>
                <TableCell
                  colSpan={7}
                  className="text-center py-12 text-gray-500"
                >
                  {variant === "all" ? "게시글이 없습니다." : "인기글이 없습니다."}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* 모바일 카드 */}
      <div className="sm:hidden space-y-3">
        {posts.length > 0 ? (
          posts.map((post, index) => (
            <BoardMobileCard
              key={post.id}
              post={post}
              index={index}
              variant={variant}
              isRead={readStatus[post.id] || false}
              progress={progressStatus[post.id] || 0}
              showRanking={showRanking}
              enablePopover={enablePopover}
            />
          ))
        ) : (
          <Card variant="elevated">
            <div className="p-8 text-center text-brand-secondary">
              {variant === "all" ? "게시글이 없습니다." : "인기글이 없습니다."}
            </div>
          </Card>
        )}
      </div>
    </>
  );
});

BoardTable.displayName = "BoardTable";